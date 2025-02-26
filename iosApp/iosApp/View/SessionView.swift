import SwiftUI
import Foundation
import ComposeApp

struct SessionView: View {
    @State var options: [OptionImpl<SessionOption>] = []
    
    var body: some View {
        List {
            Section("Takes") {
                
            }
            Section("Sessions") {
                ForEach(options) { option in
                    OptionListItem(data: option)
                }
            }
        }
        .task {
            
        }
    }
}
