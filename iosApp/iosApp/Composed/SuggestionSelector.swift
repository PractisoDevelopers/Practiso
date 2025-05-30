import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct SuggestionSelector : View {
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    @Binding var selection: (any Option)?
    var searchText: String
    @State private var data: [any ComposeApp.SessionCreator]? = nil
    
    init(selection: Binding<(any Option)?> = Binding.constant(nil), searchText: String = "") {
        self._selection = selection
        self.searchText = searchText
    }
    
    var body: some View {
        Group {
            if let array = data {
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
            } else {
                initializationView
            }
        }
        .collect(flow: AppRecommendationService.shared.getCombined()) { latestValue in
            data = latestValue
        }
    }
    
    var initializationView: some View {
        Observing(Database.shared.fei.getUpgradeState()) {
            loadingView
        } content: { feiState in
            switch onEnum(of: feiState) {
            case .ready(_):
                loadingView
            default:
                VStack {
                    ProgressView()
                    Text("Waiting for inference...")
                    Text("interact with status bar to continue")
                }
            }
        }
        .foregroundStyle(.secondary)
        .padding()
    }
    
    var loadingView: some View {
        VStack {
            ProgressView()
            Text("Loading suggestions...")
        }
    }
    
    private func isIncluded(option: any Option) -> Bool {
        return option.view.header.contains(searchText)
        || option.view.title?.contains(searchText) == true
        || option.view.subtitle?.contains(searchText) == true
    }
}
