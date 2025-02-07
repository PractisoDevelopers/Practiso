import Foundation
import SwiftUI

struct LibraryView: View {
    @Environment(ContentView.ViewModel.self) private var contentViewModel: ContentView.ViewModel?
    var body: some View {
        List {
            Section(header: Text("derived")) {
                BlockButton(action: { openView(dest: .session)}) {
                    Label {
                        Text("Sessions")
                            .foregroundStyle(Color.primary)
                    } icon: {
                        Image(systemName: "star")
                    }
                }
            }
            
            Section(header: Text("source")) {
                BlockButton(action: { openView(dest: .template) }) {
                    Label {
                        Text("Templates")
                            .foregroundStyle(Color.primary)
                    } icon: {
                        Image(systemName: "gearshape")
                    }
                }
                BlockButton(action: { openView(dest: .dimension) }) {
                    Label {
                        Text("Dimensions")
                            .foregroundStyle(Color.primary)
                    } icon: {
                        Image(systemName: "tag")
                    }
                }
                BlockButton(action: { openView(dest: .question) }) {
                    Label {
                        Text("Questions")
                            .foregroundStyle(Color.primary)
                    } icon: {
                        Image(systemName: "document")
                    }
                }
            }
        }
    }
    
    func openView(dest: ContentView.Destination) {
        guard let model = contentViewModel else {
            return
        }
        model.navigationPath.append(dest)
    }
}

#Preview {
    LibraryView()
}
