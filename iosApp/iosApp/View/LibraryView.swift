import Foundation
import SwiftUI
import Shared

struct LibraryView: View {
    @Binding var destination: Destination?

    struct LibraryOption: Identifiable {
        let name: String
        let systemImage: String
        let id: Destination
    }

    @State var deriveSectionExpanded = true
    @State var sourceSectionExpanded = true

    var body: some View {
        List(selection: $destination) {
            Section(isExpanded: $deriveSectionExpanded) {
                Item(option: .init(name: .init(localized: "Session"), systemImage: "star", id: .session)) {
                    destination = .session
                }
            } header: {
                Text("Derived")
            }

            Section(isExpanded: $sourceSectionExpanded) {
                Item(option: .init(name: .init(localized: "Dimension"), systemImage: "tag", id: .dimension)) {
                    destination = .dimension
                }
                Item(option: .init(name: .init(localized: "Question"), systemImage: "document", id: .question)) {
                    destination = .question
                }
                Item(option: .init(name: .init(localized: "Community"), systemImage: "globe.americas", id: .community)) {
                    destination = .community
                }
            } header: {
                Text("Source")
            }
        }
    }

    private struct Item: View {
        let option: LibraryOption
        @ObservedObject private var dropDelegate: HoverableDropDelegate
        init(option: LibraryOption, trigger: @escaping () -> Void) {
            self.option = option
            dropDelegate = HoverableDropDelegate(trigger: trigger)
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
