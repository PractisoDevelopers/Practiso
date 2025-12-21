import ComposeApp
import Foundation
import SwiftUI

struct OptionList<Content: View, Item: Option>: View {
    @Environment(\.editMode) var editMode: Binding<EditMode>?
    @ObservedObject var data: OptionListData<Item>
    @Binding private var selection: Set<Int64>
    var onDelete: (Set<Int64>) -> Void
    var content: (Item) -> Content

    init(data: OptionListData<Item>, selection: Binding<Set<Int64>> = Binding.constant(Set()), onDelete: @escaping (Set<Int64>) -> Void, content: @escaping (Item) -> Content) {
        self.data = data
        _selection = selection
        self.onDelete = onDelete
        self.content = content
    }

    @State private var searchText: String = ""
    @State private var sorting: OptionListSort = .name(.acending)

    private var itemModel: [Item] {
        let filtered =
            if searchText.isEmpty { data.items }
        else { data.items.filter { $0.view.header.contains(searchText) || $0.view.title?.contains(searchText) == true || $0.view.subtitle?.contains(searchText) == true } }

        let sorted: [Item] = switch sorting {
        case .name(.acending):
            filtered.sorted { ($0.kt as! any NameComparable).nameCompare < ($1.kt as! any NameComparable).nameCompare }
        case .name(.decending):
            filtered.sorted { ($0.kt as! any NameComparable).nameCompare > ($1.kt as! any NameComparable).nameCompare }
        case .modification(.acending):
            filtered.sorted { ($0.kt as! any ModificationComparable).modificationCompare < ($1.kt as! any ModificationComparable).modificationCompare }
        case .modification(.decending):
            filtered.sorted { ($0.kt as! any ModificationComparable).modificationCompare > ($1.kt as! any ModificationComparable).modificationCompare }
        case .creation(.acending):
            filtered.sorted { ($0.kt as! any CreationComparable).creationCompare < ($1.kt as! any CreationComparable).creationCompare }
        case .creation(.decending):
            filtered.sorted { ($0.kt as! any CreationComparable).creationCompare > ($1.kt as! any CreationComparable).creationCompare }
        }

        return sorted
    }

    var body: some View {
        Delay { pastDelay in
            Group {
                if data.items.isEmpty {
                    OptionListPlaceholder()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(.background)
                } else {
                    List(itemModel, selection: $selection) { option in
                        content(option)
                    }
                    .listStyle(.plain)
                    .navigationBarBackButtonHidden(editMode?.wrappedValue.isEditing == true)
                    .toolbar {
                        if editMode?.wrappedValue.isEditing == true {
                            ToolbarItem(placement: .navigation) {
                                if selection.count == data.items.count {
                                    Button("Deselect All") {
                                        selection = Set()
                                    }
                                } else {
                                    Button("Select All") {
                                        selection = Set(data.items.map(\.id))
                                    }
                                }
                            }

                            ToolbarItem(placement: .confirmationAction) {
                                EditButton()
                            }

                            ToolbarItem(placement: .bottomBar) {
                                Button("Delete", systemImage: "trash", role: .destructive) {
                                    onDelete(selection)
                                }
                                .disabled(selection.isEmpty)
                            }
                        }
                    }
                    .searchable(text: $searchText)
                }
            }
            .toolbar {
                if data.isRefreshing && pastDelay {
                    ToolbarItem(placement: .primaryAction) {
                        ProgressView()
                    }
                }

                if editMode?.wrappedValue.isEditing == false && (!data.items.isEmpty || !pastDelay) {
                    ToolbarItem(placement: .secondaryAction) {
                        Button("Select", systemImage: "checkmark.circle") {
                            withAnimation {
                                editMode?.wrappedValue = .active
                            }
                        }
                    }
                    
                    ToolbarItem(placement: .secondaryAction) {
                        Menu("Sorting", systemImage: "line.3.horizontal.decrease.circle") {
                            Toggle("Acending", systemImage: "chevron.up", isOn: Binding(get: {
                                sorting.order == .acending
                            }, set: { isOn in
                                sorting = sorting.ordered(by: isOn ? .acending : .decending)
                            }))
                            Toggle("Decending", systemImage: "chevron.down", isOn: Binding(get: {
                                sorting.order == .decending
                            }, set: { isOn in
                                sorting = sorting.ordered(by: isOn ? .decending : .acending)
                            }))
                            
                            Divider()
                            
                            if Item.KtType.self is any NameComparable.Type {
                                Toggle("Name", isOn: Binding(get: {
                                    switch sorting {
                                    case .name:
                                        true
                                    default:
                                        false
                                    }
                                }, set: { _ in
                                    sorting = .name(sorting.order)
                                }))
                            }
                            
                            if Item.KtType.self is any ModificationComparable.Type {
                                Toggle("Modification", isOn: Binding(get: {
                                    switch sorting {
                                    case .modification:
                                        true
                                    default:
                                        false
                                    }
                                }, set: { _ in
                                    sorting = .modification(sorting.order)
                                }))
                            }
                            
                            if Item.KtType.self is any CreationComparable.Type {
                                Toggle("Creation", isOn: Binding(get: {
                                    switch sorting {
                                    case .creation:
                                        true
                                    default:
                                        false
                                    }
                                }, set: { _ in
                                    sorting = switch sorting {
                                    case .creation(.acending):
                                        .creation(.decending)
                                    default:
                                        .creation(.acending)
                                    }
                                }))
                            }
                        }
                    }
                }
            }
        }
    }
}

#Preview {
    let future = KotlinInstant.Companion.shared.DISTANT_FUTURE
    let items: [OptionImpl<QuizOption>] = (0 ... 10).map { i in
        OptionImpl(kt: QuizOption(quiz: Quiz(id: Int64(i), name: "Sample \(i)", creationTimeISO: future, modificationTimeISO: future), preview: "Lore Ipsum"))
    }

    NavigationStack {
        OptionList(
            data: OptionListData(items: items, refreshing: false),
            onDelete: { _ in },
            content: { option in
                OptionListItem(data: option)
            }
        )
    }
}
