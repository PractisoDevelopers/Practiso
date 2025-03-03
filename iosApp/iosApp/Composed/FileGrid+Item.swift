import Foundation
import SwiftUI

struct FileGridItem<Title : View, Caption : View> : View {
    let title: Title
    let caption: Caption
    
    var body: some View {
        VStack {
            Image("Document")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 54, height: 54)
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
