import Foundation
import ComposeApp

extension ContentView {
    enum Destination {
        case session
        case library
        case template
        case dimension
        case question
    }
    
    @MainActor
    class ViewModel: Observable, ObservableObject {
        @Published
        var navigationPath: [Destination] = [.session]
        
        @Published
        var templates = OptionListView.Data()
        @Published
        var quizzes = OptionListView.Data()
        @Published
        var dimensions = OptionListView.Data()
        
        func startObserving() async {
            let source = LibraryDataModel(db: Database.shared.app)
            Task {
                for await it in source.quiz {
                    quizzes.items = it.map(Option.init)
                }
            }
            Task {
                for await it in source.dimensions {
                    dimensions.items = it.map(Option.init)
                }
            }
            Task {
                for await it in source.templates {
                    templates.items = it.map(Option.init)
                }
            }
        }
    }
}
