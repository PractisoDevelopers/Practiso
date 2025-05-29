import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct SuggestionSelector : View {
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    enum DataState {
        case pending
        case ok([any Option])
    }
    
    @Binding var selection: (any Option)?
    var searchText: String
    @State private var data: DataState = .pending
    
    init(selection: Binding<(any Option)?> = Binding.constant(nil), searchText: String = "") {
        self._selection = selection
        self.searchText = searchText
    }
    
    var body: some View {
        Observing(AppRecommendationService.shared.getCombined()) {
            VStack {
                ProgressView()
                Text("Loading suggestions...")
            }
            .foregroundStyle(.secondary)
        } content: { array in
            LazyVStack(spacing: 0) {
                ForEach({
                    let array = array.map(SessionCreatorOption.from)
                    return if searchText.isEmpty { array } else { array.filter(isIncluded) }
                }(), id: \.id) { option in
                    Divider()
                        .padding(.leading)
                    Button {
                        selection = if selection?.id == option.id {
                            nil
                        } else {
                            option
                        }
                    } label: {
                        HStack {
                            OptionListItem(data: option)
                                .frame(maxWidth: .infinity)
                            if selection?.id == option.id {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(.tint)
                                    .padding(.trailing)
                            }
                        }
                        .padding(.vertical, 8)
                        .padding(.leading)
                    }
                    .buttonStyle(.listItem)
                }
            }
        }
    }
    
    private func isIncluded(option: any Option) -> Bool {
        return option.view.header.contains(searchText)
        || option.view.title?.contains(searchText) == true
        || option.view.subtitle?.contains(searchText) == true
    }
}
