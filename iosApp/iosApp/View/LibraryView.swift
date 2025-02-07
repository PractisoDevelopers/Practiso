import Foundation
import SwiftUI

private struct OptionGroup<LabelView: View>: View {
    @State var isExpanded: Bool = false
    @ViewBuilder var label: LabelView
    
    var body: some View {
        Button(action: {
            isExpanded = !isExpanded
        }) {
            HStack {
                HStack {
                    label
                    Spacer()
                    Image(systemName: "chevron.right")
                }
            }
        }
    }
}

struct LibraryView: View {
    @Environment(ContentView.ViewModel.self) private var contentViewModel: ContentView.ViewModel?
    var body: some View {
        List {
            Section(header: Text("derived")) {
                Button(action: openSessionsView) {
                    HStack {
                        Label {
                            Text("Sessions")
                                .foregroundStyle(Color.primary)
                        } icon: {
                            Image(systemName: "star")
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                    }
                }
                .contentShape(Rectangle())
            }
            
            Section(header: Text("source")) {
                OptionGroup(label: {
                    Label {
                        Text("Templates")
                            .foregroundStyle(Color.primary)
                    } icon: {
                        Image(systemName: "gearshape")
                    }
                })
                OptionGroup(label: {
                    Label {
                        Text("Dimensions")
                            .foregroundStyle(Color.primary)
                    } icon: {
                        Image(systemName: "tag")
                    }
                })
                OptionGroup(label: {
                    Label {
                        Text("Questions")
                            .foregroundStyle(Color.primary)
                    } icon: {
                        Image(systemName: "document")
                    }
                })
            }
        }
    }
    
    func openSessionsView() {
        guard let model = contentViewModel else {
            return
        }
        model.navigationPath.append(.session)
    }
}

#Preview {
    LibraryView()
}
