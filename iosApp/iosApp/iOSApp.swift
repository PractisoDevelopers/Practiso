import Combine
import SwiftUI
import CoreHaptics
import ComposeApp

@main
struct iOSApp: App {
    @ObservedObject var viewModel = ViewModel()
    
    var body: some Scene {
        WindowGroup {
            NavigationStack(path: $viewModel.navigationPath) {
                ContentView()
                    .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                    .navigationDestination(for: NavigatorStackItem.self) { screen in
                        switch screen.destination {
                        default:
                            Text("should never reach here")
                        }
                    }
            }
            .task {
                await self.viewModel.startObserving()
            }
        }
    }
}

extension iOSApp {
    @MainActor
    class ViewModel: ObservableObject {
        @Published var navigationPath: [NavigatorStackItem] = []
        var subscription: AnyCancellable?
        
        init() {
            subscription = $navigationPath.sink { path in
                Task {
                    do {
                        try await UINavigator.shared.mutateBackstack(
                            newValue: [NavigatorStackItem(destination: AppDestination.mainView, options: [])] + path,
                            pointer: Int32(path.count)
                        )
                    } catch {
                        // ignored
                    }
                }
            }
        }
        
        deinit {
            subscription!.cancel()
        }
        
        func startObserving() async {
            for await path in UINavigator.shared.path {
                if path != navigationPath {
                    navigationPath = path
                }
            }
        }
    }
}
