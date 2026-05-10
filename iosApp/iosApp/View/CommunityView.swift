import AsyncAlgorithms
@preconcurrency import ComposeApp
import Foundation
import SwiftUI

struct CommunityView: View {
    @Environment(ContentView.Model.self) private var model

    @State private var state: PageState = .loading
    @State private var refreshCounter = 0
    @State private var selection = Set<String>()

    private let archivePages = AppCommunityService.shared.getArchivePagination(sortOptions: SortOptions(descending: true, keyword: .updateTime))
    private let refreshChannel = AsyncChannel<Void>()

    enum PageState {
        case loading
        case ready(dimensions: [DimensionMetadata], archives: [ArchiveMetadata])
        case error(message: String)
    }

    var body: some View {
        VStack {
            switch state {
            case .loading:
                ProgressView()
                    .controlSize(.large)
            case let .ready(dimensions, archives):
                List(selection: Binding(get: {
                    selection
                }, set: { newValue in
                    selection = newValue
                    if let id = selection.first, let meta = archives.first(where: { $0.id == id }) {
                        model.detail = .archivePreview(meta)
                    }
                })) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        LazyHStack {
                            Spacer(minLength: 20)
                            ForEach(dimensions, id: \.name) { dim in
                                DimensionChip(emoji: dim.emoji, name: dim.name)
                            }
                            Spacer(minLength: 20)
                        }
                    }
                    .frame(minHeight: 30)
                    .listRowSeparator(.hidden)
                    .listRowInsets(.zero)

                    ForEach(archives, id: \.id) { archive in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(archive.nameWithoutExtension)
                            Text("\(Image(systemName: "heart")) \(archive.likes) \(Image(systemName: "arrow.down.circle")) \(archive.downloads)")
                                .foregroundStyle(.secondary)
                        }
                    }
                }

            case let .error(message):
                Text("Failed to load Community")
                Text(message)
                Button {
                    refreshCounter += 1
                } label: {
                    Text("try again")
                }
            }
        }
        .refreshable {
            refreshCounter += 1
            var iter = refreshChannel.makeAsyncIterator()
            await iter.next()
        }
        .task(id: refreshCounter) {
            await withTaskGroup { tg in
                actor Resources {
                    var dims: [DimensionMetadata]?
                    var archives: [ArchiveMetadata]?

                    func setDims(_ newValue: [DimensionMetadata]) {
                        self.dims = newValue
                    }

                    func setArchives(_ newValue: [ArchiveMetadata]) {
                        self.archives = newValue
                    }

                    func appendArchives<S>(contentsOf: S) where S: Sequence<ArchiveMetadata> {
                        self.archives?.append(contentsOf: contentsOf)
                    }
                }

                let resources = Resources()
                tg.addTask {
                    for await items in AppCommunityService.shared.getDimensions(takeFirst: 5) {
                        await resources.setDims(items)
                        Task { @MainActor in
                            if let dimensions = await resources.dims, let archives = await resources.archives {
                                state = .ready(dimensions: dimensions, archives: archives)
                                await refreshChannel.send(())
                            }
                        }
                    }
                }
                tg.addTask {
                    for await page in await archivePages {
                        for await items in page.items {
                            if await resources.archives == nil {
                                await resources.setArchives(items as! [ArchiveMetadata])
                            } else {
                                await resources.appendArchives(contentsOf: items as! [ArchiveMetadata])
                            }

                            Task { @MainActor in
                                if let dimensions = await resources.dims, let archives = await resources.archives {
                                    state = .ready(dimensions: dimensions, archives: archives)
                                    await refreshChannel.send(())
                                }
                            }
                        }
                    }
                }
                await tg.waitForAll()
            }
        }
    }
}
