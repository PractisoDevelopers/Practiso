import Foundation
import SwiftUI

struct OptionListItem: View {
    let data: any Option
    var body: some View {
        VStack(spacing: 4) {
            Text(data.view.header)
                .lineLimit(1)
                .fontWeight(.medium)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let title = data.view.title {
                Text(title)
                    .lineLimit(1)
                    .font(.system(size: 15))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            if let subtitle = data.view.subtitle {
                Text(subtitle)
                    .lineLimit(1)
                    .font(.system(size: 15))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

