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
                buildContentList(suggestions: array)
            } else {
                initializationView
            }
        }
        .collect(flow: AppRecommendationService.shared.getCombined()) { latestValue in
            data = latestValue
        }
    }
    
    func buildContentList(suggestions: [any ComposeApp.SessionCreator]) -> some View {
        LazyVStack(spacing: 0) {
            ForEach({
                let array = suggestions.map(SessionCreatorOption.from)
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
    
    @State private var missingModelAlert: FeiDbState.MissingModel? = nil
    @State private var downloadPendingAlert: FeiDbState.PendingDownload? = nil
    var initializationView: some View {
        Observing(Database.shared.fei.getUpgradeState()) {
            loadingView
        } content: { feiState in
            switch onEnum(of: feiState) {
            case .ready(_):
                loadingView
            case let .missingModel(state):
                loadingView
                    .onAppear {
                        missingModelAlert = state
                    }
                    .missingModelAlert(stateBinding: $missingModelAlert)
            case let .pendingDownload(state):
                loadingView
                    .onAppear {
                        downloadPendingAlert = state
                    }
                    .downloadAlert(stateBinding: $downloadPendingAlert)
            default:
                VStack {
                    ProgressView()
                    Text("Waiting for inference...")
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
