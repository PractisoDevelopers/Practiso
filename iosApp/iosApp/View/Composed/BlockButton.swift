import SwiftUI

struct BlockButton<LabelView: View>: View {
    @State var action: () -> Void
    @ViewBuilder var label: LabelView
    
    var body: some View {
        Button(action: action) {
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
