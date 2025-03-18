import Foundation
import SwiftUI

struct ClosePushButton : View {
    var body: some View {
        Image(systemName: "xmark.circle.fill")
            .resizable()
            .aspectRatio(contentMode: .fit)
            .background()
            .frame(width: 28, height: 28)
    }
}
