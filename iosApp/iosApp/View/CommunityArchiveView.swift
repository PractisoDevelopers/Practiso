@preconcurrency import ComposeApp
import Foundation
import SwiftUI

struct CommunityArchiveView: View {
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var model

    let item: ArchiveMetadata

    @State private var dimSelection = Set<String>()
    @State private var state: PageState = .loading
    @State private var downloadState: DownloadCycle = DownloadStopped.Idle.shared
    @State private var showRedownloadAlert = false
    @State private var searchKeywords = ""

    enum PageState: Equatable {
        case loading
        case ready(content: [ArchivePreview])
    }

    var body: some View {
        Group {
            switch state {
            case .loading:
                ProgressView()
                    .controlSize(.large)
            case let .ready(content):
                ScrollView(.vertical) {
                    LazyVGrid(columns: [.init(.adaptive(minimum: 100), alignment: .top)], spacing: 24, pinnedViews: .sectionHeaders) {
                        Section {
                            ForEach(filterContent(content, dims: dimSelection, keywords: searchKeywords), id: \.hashValue) { quiz in
                                FileGridItem(title: Text(quiz.name), caption: EmptyView()) {
                                    archiveView(quiz)
                                }
                            }
                        } header: {
                            ScrollView(.horizontal, showsIndicators: false) {
                                LazyHStack {
                                    ForEach(item.dimensions, id: \.name) { dim in
                                        Button {
                                            withAnimation {
                                                if dimSelection.contains(dim.name) {
                                                    dimSelection.remove(dim.name)
                                                } else {
                                                    dimSelection.insert(dim.name)
                                                }
                                            }
                                        } label: {
                                            DimensionChip(emoji: dim.emoji, name: dim.name,
                                                          selected: dimSelection.contains(dim.name))
                                                .tint(.primary)
                                        }
                                    }
                                }
                                .padding(.horizontal)
                            }
                            .padding(.bottom, 6)
                        }
                    }
                }
            }
        }
        .navigationTitle(item.nameWithoutExtension)
        .navigationBarTitleDisplayMode(.inline)
        .task(id: item.id) {
            state = .loading
        }
        .collect(flow: AppCommunityService.shared.getArchivePreview(archiveId: item.id), into: $state) { update in
            .ready(content: update)
        }
        .collect(flow: AppCommunityService.shared.downloadState(of: item), into: $downloadState)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                downloadProgressView(cycle: downloadState)
                    .animation(.default, value: downloadState)

                Button("Download", systemImage: "square.and.arrow.down") {
                    if downloadState is DownloadState.Completed {
                        showRedownloadAlert = true
                    } else if downloadState is DownloadState {
                        Task {
                            await errorHandler.catchAndShowImmediately {
                                try await AppCommunityService.shared.cancelDownload(of: item)
                            }
                        }
                    } else {
                        Task {
                            await errorHandler.catchAndShowImmediately {
                                try await AppCommunityService.shared.download(archive: item)
                            }
                        }
                    }
                }
            }
        }
        .alert("Re-download", isPresented: $showRedownloadAlert, actions: {
            Button("Yes") {
                Task {
                    await errorHandler.catchAndShowImmediately {
                        try await AppCommunityService.shared.cancelDownload(of: item)
                        try await AppCommunityService.shared.download(archive: item)
                    }
                }
            }
            Button("Cancel", role: .cancel) {
                showRedownloadAlert = false
            }
        }, message: {
            Text("Do you want to download this again?")
        })
        .searchable(text: $searchKeywords)
    }

    func archiveView(_ quiz: ArchivePreview) -> some View {
        FileIcon()
            .contextMenu {
                if quiz.dimensions.isEmpty {
                    Text("No actions available")
                }
                ForEach(quiz.dimensions) { dimName in
                    Toggle(dimName, isOn: Binding(get: {
                        dimSelection.contains(dimName)
                    }, set: { newValue in
                        withAnimation {
                            if newValue {
                                dimSelection.insert(dimName)
                            } else {
                                dimSelection.remove(dimName)
                            }
                        }
                    }))
                }
            } preview: {
                Text(quiz.body)
                    .frame(height: 200)
                    .padding()
            }
    }

    func downloadProgressView(cycle: DownloadCycle) -> some View {
        Group {
            switch onEnum(of: cycle) {
            case .downloadStopped:
                EmptyView()
            case let .downloadState(state):
                switch onEnum(of: state) {
                case .completed:
                    Image(systemName: "checkmark")

                case .configure:
                    IndeterministicCircularProgressView()

                case .preparing:
                    IndeterministicCircularProgressView()

                case let .downloading(download):
                    CircularProgressView(value: download.progress)
                }
            }
        }
    }

    func filterContent(_ content: [ArchivePreview], dims: Set<String>, keywords: String) -> [ArchivePreview] {
        func filterByDim(_ content: [ArchivePreview]) -> [ArchivePreview] {
            if dims.isEmpty {
                return content
            }
            return content.filter { quiz in
                quiz.dimensions.first(where: dims.contains) != nil
            }
        }

        func filterByKeywords(_ content: [ArchivePreview]) -> [ArchivePreview] {
            if keywords.isEmpty {
                return content
            }
            let splitKeywords = keywords.split(separator: " ")
            return content.filter { quiz in
                splitKeywords.contains { keyword in
                    quiz.name.contains(keyword) || quiz.body.contains(keyword)
                }
            }
        }

        return filterByKeywords(filterByDim(content))
    }
}
