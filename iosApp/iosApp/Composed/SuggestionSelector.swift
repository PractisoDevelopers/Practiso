import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct SuggestionSelector : View {
    let service = RecommendationService(db: Database.shared.app)
    
    enum DataState {
        case pending
        case ok([any Option])
    }
    
    @Binding var selection: (any Option)?
    @State private var data: DataState = .pending
    
    init(selection: Binding<(any Option)?> = Binding.constant(nil)) {
        self._selection = selection
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
                    ForEach(array, id: \.id) { option in
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
            for await options in service.getSmartRecommendations() {
                data = .ok(options.map(SessionCreatorOption.from))
            }
        }
    }
}
