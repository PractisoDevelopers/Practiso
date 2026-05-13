import Foundation
import SwiftUI
import ComposeApp

struct TakeStatHeader : View {
    let stat: TakeStat
    
    var body: some View {
        HStack(spacing: 12) {
            CircularProgressView(value: Double(stat.countQuizDone) / Double(stat.countQuizTotal))
            VStack(alignment: .leading) {
                Text(stat.name)
                Text("\(Text(100 * stat.countQuizDone / stat.countQuizTotal, format: .percent)) done")
                    .font(.subheadline)
            }
        }
    }
}
