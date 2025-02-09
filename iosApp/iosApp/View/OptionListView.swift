import Foundation
import SwiftUI
import ComposeApp

struct OptionListView<Content : View, Item : Option>: View {
    @ObservedObject var data: OptionListData<Item>
    var onDelete: (Set<Int64>) -> Void
    var content: (Item) -> Content
    
    @State private var selection = Set<Int64>()
    @State private var editMode: EditMode = .inactive
    @State private var searchText: String = ""
    @State private var sorting: OptionListSort = .name(.acending)
    
    private var itemModel: [Item] {
        let sorted: [Item] = switch sorting {
        case .name(.acending):
            data.items.sorted { ($0.kt as! any NameComparable).nameCompare < ($1.kt as! any NameComparable).nameCompare }
        case .name(.decending):
            data.items.sorted { ($0.kt as! any NameComparable).nameCompare > ($1.kt as! any NameComparable).nameCompare }
        case .modification(.acending):
            data.items.sorted { ($0.kt as! any ModificationComparable).modificationCompare < ($1.kt as! any ModificationComparable).modificationCompare }
        case .modification(.decending):
            data.items.sorted { ($0.kt as! any ModificationComparable).modificationCompare > ($1.kt as! any ModificationComparable).modificationCompare }
        case .creation(.acending):
            data.items.sorted { ($0.kt as! any CreationComparable).creationCompare < ($1.kt as! any CreationComparable).creationCompare }
        case .creation(.decending):
            data.items.sorted { ($0.kt as! any CreationComparable).creationCompare > ($1.kt as! any CreationComparable).creationCompare }
        }
        
        let filtered =
        if searchText.isEmpty { sorted }
        else { sorted.filter { $0.view.header.contains(searchText) || $0.view.title?.contains(searchText) == true || $0.view.subtitle?.contains(searchText) == true } }
        
        return filtered
    }

    var body: some View {
        if data.items.isEmpty {
            OptionListPlaceholder()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(.background)
        } else {
            List(
                itemModel,
                selection: $selection) { option in
                content(option)
            }
            .environment(\.editMode, $editMode)
            .listStyle(.plain)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    if data.isRefreshing {
                        ProgressView()
                    }
                }
                
                if editMode == .inactive {
                    ToolbarItem {
                        Menu("More", systemImage: "ellipsis.circle") {
                            Button("Select", systemImage: "checkmark.circle") {
                                withAnimation {
                                    editMode = .active
                                }
                            }
                            
                            Divider()
                            
                            if Item.KtType.self is any NameComparable.Type {
                                Button {
                                    sorting = switch sorting {
                                    case .name(.acending):
                                            .name(.decending)
                                    default:
                                            .name(.acending)
                                    }
                                } label: {
                                    switch sorting {
                                    case .name(.acending):
                                        Label("Name", systemImage: "chevron.up")
                                    case .name(.decending):
                                        Label("Name", systemImage: "chevron.down")
                                    default:
                                        Text("Name")
                                    }
                                }
                            }
                            
                            if Item.KtType.self is any ModificationComparable.Type {
                                Button {
                                    sorting = switch sorting {
                                    case .modification(.acending):
                                            .modification(.decending)
                                    default:
                                            .modification(.acending)
                                    }
                                } label: {
                                    switch sorting {
                                    case .modification(.acending):
                                        Label("Modification", systemImage: "chevron.up")
                                    case .modification(.decending):
                                        Label("Modification", systemImage: "chevron.down")
                                    default:
                                        Text("Modification")
                                    }
                                }
                            }
                            
                            if Item.KtType.self is any CreationComparable.Type {
                                Button {
                                    sorting = switch sorting {
                                    case .creation(.acending):
                                            .creation(.decending)
                                    default:
                                            .creation(.acending)
                                    }
                                } label: {
                                    switch sorting {
                                    case .creation(.acending):
                                        Label("Creation", systemImage: "chevron.up")
                                    case .creation(.decending):
                                        Label("Creation", systemImage: "chevron.down")
                                    default:
                                        Text("Creation")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    ToolbarItem(placement: .topBarTrailing) {
                        if selection.count == data.items.count {
                            Button("Deselect All") {
                                selection = Set()
                            }
                        } else {
                            Button("Select All") {
                                selection = Set(data.items.map { $0.id })
                            }
                        }
                    }
                    
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Done") {
                            withAnimation {
                                editMode = .inactive
                            }
                        }
                    }
                    
                    ToolbarItem(placement: .bottomBar) {
                        Button("Delete", role: .destructive) {
                            onDelete(selection)
                        }
                        .disabled(selection.isEmpty)
                    }
                }
            }
            .searchable(text: $searchText, prompt: "Search...")
        }
    }
}

#Preview {
    let future = Kotlinx_datetimeInstant.Companion.shared.DISTANT_FUTURE
    let items: [OptionImpl<QuizOption>] = (0...10).map { i in
        return OptionImpl(kt: QuizOption(quiz: Quiz(id: Int64(i), name: "Sample \(i)", creationTimeISO: future, modificationTimeISO: future), preview: "Lore Ipsum"))
    }
    OptionListView(
        data: OptionListData(items: items),
        onDelete: { _ in },
        content: { option in
            OptionListItem(data: option)
        }
    )
}
