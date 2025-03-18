import Foundation
import SwiftUI
import ComposeApp

struct TakeCreatorView : View {
    let service = LibraryService(db: Database.shared.app)
    
    let session: SessionOption
    @Binding var takeParams: TakeParameters
    
    @State private var takeCount: Int? = nil
    
    var body: some View {
        List {
            Section("Take") {
                if let counting = takeCount {
                    Text("Will create as Take \(counting + 1)")
                        .foregroundStyle(.secondary)
                } else {
                    Text("Loading session...")
                        .foregroundStyle(.secondary)
                }
            }
            
            Section("Timer") {
                TimerView(value: Binding(get: {
                    takeParams.timers
                }, set: { newValue in
                    takeParams = TakeParameters(sessionId: session.id, timers: newValue)
                }))
            }
        }
        .task {
            for await stats in service.getTakesBySession(id: session.id) {
                takeCount = stats.count
            }
        }
    }
}
