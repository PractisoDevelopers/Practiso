import Foundation
import SwiftUI

struct LibraryView: View {
    @Binding var destination: Destination?
    
    struct LibrarySection: Identifiable {
        var isExpanded = true
        let options: [LibraryOption]
        let id: String
    }
    
    struct LibraryOption: Identifiable {
        let name: String
        let systemImage: String
        let id: Destination
    }
    
    @State private var sections: [LibrarySection] = [
        LibrarySection(options: [
            LibraryOption(name: "Session", systemImage: "star", id: .session)
        ], id: "Derived"),
        LibrarySection(options: [
            LibraryOption(name: "Template", systemImage: "gearshape", id: .template),
            LibraryOption(name: "Dimension", systemImage: "tag", id: .dimension),
            LibraryOption(name: "Question", systemImage: "document", id: .question)
        ], id: "Source")
    ]
    
    var body: some View {
        List(selection: $destination) {
            ForEach($sections) { $section in
                Section(isExpanded: $section.isExpanded, content: {
                    ForEach(section.options) { option in
                        Label(option.name, systemImage: option.systemImage)
                    }
                }, header: {
                    Text(section.id)
                })
            }
        }
    }
}

#Preview {
    @Previewable @State var dest: Destination? = .session
    LibraryView(destination: $dest)
}
