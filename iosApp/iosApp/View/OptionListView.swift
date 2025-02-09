import Foundation
import SwiftUI
import ComposeApp

struct OptionListItem: View {
    @State var data: Option
    var body: some View {
        VStack(spacing: 4) {
            Text(data.view.header)
                .lineLimit(1)
                .fontWeight(.medium)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let title = data.view.title {
                Text(title)
                    .lineLimit(1)
                    .font(.system(size: 15))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            if let subtitle = data.view.subtitle {
                Text(subtitle)
                    .lineLimit(1)
                    .font(.system(size: 15))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

class OptionListData: ObservableObject {
    @Published var items: [Option]
    @Published var isRefreshing: Bool
    
    init(items: [Option] = [], refreshing: Bool = true) {
        self.items = items
        self.isRefreshing = refreshing
    }
}


struct OptionListView<Content : View>: View {
    @ObservedObject var data: OptionListData
    var onDelete: (Set<Int64>) -> Void
    var content: (Option) -> Content
    
    @State private var selection = Set<Int64>()
    @State private var editMode: EditMode = .inactive

    var body: some View {
        if data.items.isEmpty {
            OptionListPlaceholder()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(.background)
        } else {
            List(data.items, selection: $selection) { option in
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
                        Menu {
                            Button {
                                withAnimation {
                                    editMode = .active
                                }
                            } label: {
                                Label("Select", systemImage: "checkmark.circle")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
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
        }
    }
}

#Preview {
    let items: [Option] = (0...10).map { i in
        if i < 5 {
            return Option(kt: DimensionOption(dimension: Dimension(id: Int64(i), name: "Sample \(i)"), quizCount: Int32(120 * sin(Double(i)))))
        } else {
            let future = Kotlinx_datetimeInstant.Companion.shared.DISTANT_FUTURE
            return Option(kt: QuizOption(quiz: Quiz(id: Int64(i), name: "Sample \(i)", creationTimeISO: future, modificationTimeISO: future), preview: "Lore Ipsum"))
        }
    }
    OptionListView(
        data: OptionListData(items: items),
        onDelete: { _ in },
        content: { option in
            OptionListItem(data: option)
        }
    )
}
