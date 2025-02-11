import Foundation
import SwiftUI
import ComposeApp

struct DimensionDetailView : View {
    var option: DimensionOption
    
    var body: some View {
        Text("Details for dimension \(option.dimension.name) here")
    }
}
