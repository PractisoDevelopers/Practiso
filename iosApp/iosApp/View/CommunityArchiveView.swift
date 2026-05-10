@preconcurrency import ComposeApp
import Foundation
import SwiftUI

struct CommunityArchiveView: View {
    let item: ArchiveMetadata
    
    @State private var dimSelection = Set<String>()
    @State private var state: PageState = .loading
    
    enum PageState {
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
                            ForEach(filterContent(content), id: \.hashValue) { quiz in
                                FileGridItem(title: Text(quiz.name), caption: EmptyView()) {
                                    FileIcon()
                                        .contextMenu {
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
            for await res in AppCommunityService.getArchivePreview(archiveId: item.id) {
                state = .ready(content: res)
            }
        }
    }
    
    func filterContent(_ content: [ArchivePreview]) -> [ArchivePreview] {
        if dimSelection.isEmpty {
            return content
        }
        return content.filter { quiz in
            quiz.dimensions.first(where: dimSelection.contains) != nil
        }
    }
}
