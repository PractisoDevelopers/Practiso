import Foundation
import SwiftUI
import ComposeApp

struct OptionListView: View {
    @State var model: Model?
    @ObservedObject var data: Data

    protocol Model {
        func onTap(option: Option)
        func onRemove(option: Option)
    }
    
    class Data: ObservableObject {
        @Published var items: [Option]
        @Published var isRefreshing: Bool
        
        init(items: [Option] = [], refreshing: Bool = true) {
            self.items = items
            self.isRefreshing = refreshing
        }
    }

    var body: some View {
        List {
            ForEach(data.items) { option in
                VStack(spacing: 4) {
                    Text(option.view.header)
                        .lineLimit(1)
                        .fontWeight(.medium)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if let title = option.view.title {
                        Text(title)
                            .lineLimit(1)
                            .font(.system(size: 15))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    if let subtitle = option.view.subtitle {
                        Text(subtitle)
                            .lineLimit(1)
                            .font(.system(size: 15))
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        }
        .listStyle(.plain)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if data.isRefreshing {
                    ProgressView()
                }
            }
        }
    }
}

#Preview {
    let items: [Option] = (0...10).map { i in
        if i < 5 {
            return Option(kt: DimensionOption(dimension: Dimension(id: Int64(i), name: "Sample \(i)"), quizCount: Int32(120 * sin(Double(i)))))
        } else {
            let future = Kotlinx_datetimeInstant.Companion.shared.DISTANT_FUTURE
            return Option(kt: QuizOption(quiz: Quiz(id: Int64(i), name: "Sample \(i)", creationTimeISO: future, modificationTimeISO: future), preview: "Lore Ipsum"))
        }
    }
    OptionListView(data: OptionListView.Data(items: items))
}
