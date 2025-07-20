import Foundation
import SwiftUI
import ComposeApp

extension View {
    func categorizeSheet(isPresent: Binding<Bool>, quizId: Int64) -> some View {
        self.sheet(isPresented: isPresent) {
            CategorizeSheet(quizId: quizId, isPresented: isPresent)
        }
    }
}

fileprivate struct CategorizeSheet : View {
    let quizId: Int64
    @Binding var isPresented: Bool
        
    var body: some View {
        NavigationStack {
            DimensionListView(quizId: quizId)
                .navigationTitle("Categorize")
                .toolbar {
                    ToolbarItem {
                        Button("Done") {
                            isPresented = false
                        }
                    }
                }
        }
    }
}

fileprivate struct DimensionListView : View {
    let libraryService = LibraryService(db: Database.shared.app)
    let quizId: Int64
    
    var body: some View {
        Observing(libraryService.getDimensionsByQuiz(id: quizId)) {
            VStack {
                ProgressView()
                Text("Loading dimensions...")
            }
        } content: { dims in
            List(dims, id: \.dimension.id) { dim in
                HStack {
                    Text(dim.dimension.name)
                    Spacer()
                    Text("\(Int(round(dim.intensity * 100)))%")
                }
                .frame(minHeight: 42)
            }
        }
    }
}
