import UIKit
import SwiftUI
import ComposeApp
import Combine

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct QuizCreateView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ViewControllerKt.QuizCreateViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct AnswerView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ViewControllerKt.AnswerAppViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

extension UINavigationController: @retroactive UIGestureRecognizerDelegate {
    override open func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        return viewControllers.count > 1
    }
}

struct ContentView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    
    var body: some View {
        NavigationStack(path: $viewModel.navigationPath) {
            ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .navigationDestination(for: NavigatorStackItem.self) { screen in
                    switch screen.destination {
                    case .quizCreate:
                        QuizCreateView()
                            .ignoresSafeArea(.keyboard)
                            .navigationBarBackButtonHidden()
                    case .answer:
                        AnswerView()
                            .ignoresSafeArea(.keyboard)
                            .navigationBarBackButtonHidden()
                    case .mainView:
                        Text("should never reach here")
                    }
                }
        }
        .task {
            await self.viewModel.startObserving()
        }
    }
}

extension ContentView {
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
