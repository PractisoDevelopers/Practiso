import Foundation
import SwiftUI

struct FileGridItem<Icon : View, Title : View, Caption : View> : View {
    let title: Title
    let caption: Caption
    @ViewBuilder let icon: Icon

    var body: some View {
        VStack {
            icon
            title
                .multilineTextAlignment(.center)
            caption
                .multilineTextAlignment(.center)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxHeight: .infinity, alignment: .top)
    }
}
