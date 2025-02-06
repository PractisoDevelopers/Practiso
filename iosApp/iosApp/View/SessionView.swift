import SwiftUI
import Foundation
import ComposeApp

struct SessionView: View {
    @ObservedObject private var viewModel = ViewModel()
    var body: some View {
        Text("Hello there")
    }
}
