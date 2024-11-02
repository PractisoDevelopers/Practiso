import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ViewControllerKt.MainViewController(darkMode: context.environment.colorScheme == .dark)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct QuizCreateView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ViewControllerKt.QuizCreateViewController(darkMode: context.environment.colorScheme == .dark)
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
                .task {
                    do {
                        try await UINavigator.shared.navigate(destination: .mainView)
                    } catch {
                    }
                }
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .navigationDestination(for: AppDestination.self) { screen in
                    switch screen {
                    case .quizCreate:
                        QuizCreateView()
                            .navigationBarBackButtonHidden()
                    case .mainView:
                        EmptyView()
                            .task {
                                viewModel.navigationPath.removeLast()
                            }
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
        @Published var navigationPath = NavigationPath()
        
        func startObserving() async {
            for await dest in UINavigator.shared.current {
                if dest == .mainView {
                    navigationPath = NavigationPath()
                } else {
                    navigationPath.append(dest)
                }
            }
        }
    }
}
