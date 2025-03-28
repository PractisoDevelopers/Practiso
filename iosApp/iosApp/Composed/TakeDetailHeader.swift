import Foundation
import SwiftUI
import ComposeApp

struct TakeDetailHeader : View {
    private let service: TakeService
    let stat: TakeStat
    
    init(stat: TakeStat) {
        self.stat = stat
        self.service = .init(takeId: stat.id, db: Database.shared.app)
    }
    
    @State private var data: DataState = .pending
    
    enum DataState {
        case pending
        case ok(takeNumber: Int)
    }
    
    var body: some View {
        HStack(spacing: 12) {
            CircularProgressView(value: Double(stat.countQuizDone) / Double(stat.countQuizTotal))
            VStack(alignment: .leading) {
                switch data {
                case .pending:
                    Text("Loading Information...")
                case .ok(let takeNumber):
                    Text("Take \(takeNumber)")
                }
                Text("\(100 * stat.countQuizDone / stat.countQuizTotal)% done")
                    .font(.subheadline)
            }
            .task {
                for await num in service.getTakeNumber() {
                    data = .ok(takeNumber: num.intValue)
                }
            }
        }
    }
}
