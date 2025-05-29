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
            .init(name: .init(localized: "Session"), systemImage: "star", id: .session)
        ], id: .init(localized: "Derived")),
        LibrarySection(options: [
            .init(name: .init(localized: "Template"), systemImage: "gearshape", id: .template),
            .init(name: .init(localized: "Dimension"), systemImage: "tag", id: .dimension),
            .init(name: .init(localized: "Question"), systemImage: "document", id: .question)
        ], id: .init(localized: "Source"))
    ]
    
    var body: some View {
        List(selection: $destination) {
            ForEach($sections) { $section in
                Section(isExpanded: $section.isExpanded, content: {
                    ForEach(section.options) { option in
                        Item(option: option) {
                            destination = option.id
                        }
                    }
                }, header: {
                    Text(section.id)
                })
            }
        }
    }
    
    private struct Item : View {
        let option: LibraryOption
        @ObservedObject private var dropDelegate: HoverableDropDelegate
        init(option: LibraryOption, trigger: @escaping () -> Void) {
            self.option = option
            self.dropDelegate = HoverableDropDelegate(trigger: trigger)
        }

        var body: some View {
            NavigationLink(value: option.id) {
                Label(option.name, systemImage: option.systemImage)
            }
            .opacity(dropDelegate.flicker ? 0.4 : 1)
            .onDrop(
                of: [.psarchive, .psquiz],
                delegate: dropDelegate
            )
        }
    }
}

#Preview {
    @Previewable @State var dest: Destination? = .session
    LibraryView(destination: $dest)
}
