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
        LazyVStack {
            switch data {
            case .pending:
                VStack {
                    ProgressView()
                    Text("Loading suggestions")
                }
                .foregroundStyle(.secondary)
            case .ok(let array):
                LazyVStack(spacing: 0) {
                    ForEach({ if searchText.isEmpty { array } else { array.filter(isIncluded) }}(), id: \.id) { option in
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
        .task {
            await errorHandler.catchAndShowImmediately {
                for try await options in AppRecommendationService.shared.getCombined() {
                    data = .ok(options.map(SessionCreatorOption.from))
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
