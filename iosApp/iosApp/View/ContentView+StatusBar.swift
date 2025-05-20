import Foundation
import SwiftUI
import ComposeApp

struct StatusBarModifier : ViewModifier {
    let feiState: FeiDbState?
    @State private var missingModelState: FeiDbState.MissingModel? = nil

    func body(content: Content) -> some View {
        content.toolbar {
            toolbarContent
        }
        .missingModelAlert(stateBinding: $missingModelState)
    }
    
    @ToolbarContentBuilder
    var toolbarContent: some ToolbarContent {
        switch onEnum(of: feiState) {
        case .inProgress(let progress):
            ToolbarItem(placement: .status) {
                Text("Inferring \(progress.total) items...")
                    .font(.caption)
            }
            ToolbarItem(placement: .bottomBar) {
                CircularProgressView(value: Float(progress.done) / Float(progress.total))
            }
            
        case .downloadingInference(let download):
            ToolbarItem(placement: .status) {
                Text("Downloading model...")
                    .font(.caption)
            }
            ToolbarItem(placement: .bottomBar) {
                CircularProgressView(value: download.progress)
            }
            
        default:
            toolbarContent_2
        }
    }
    
    @ToolbarContentBuilder
    var toolbarContent_2: some ToolbarContent {
        switch onEnum(of: feiState) {
        case .missingModel(let mms):
            ToolbarItem(placement: .status) {
                Button("Missing Models") {
                    missingModelState = mms
                }
                .buttonStyle(.plain)
                .foregroundStyle(.red)
                .font(.caption)
            }
            
        case .collecting(_):
            ToolbarItem(placement: .status) {
                Text("Collecting Frames...")
                    .font(.caption)
            }
            
        default:
            ToolbarItem(placement: .status) {
                EmptyView()
            }
        }
    }
}

extension View {
    fileprivate func missingModelAlert(stateBinding: Binding<FeiDbState.MissingModel?>) -> some View {
        alert("Missing Models", isPresented: Binding(get: {
            stateBinding.wrappedValue != nil
        }, set: { shown in
            if (!shown) {
                stateBinding.wrappedValue = nil
            }
        }), presenting: stateBinding) { missing in
            if let proceed = missing.wrappedValue!.proceed {
                Button("Proceed Anyway", role: .destructive) {
                    proceed.trySend(element: MissingModelResponse.ProceedAnyway.shared)
                    missing.wrappedValue = nil
                }
                Button("Cancel", role: .cancel) {
                    proceed.trySend(element: MissingModelResponse.Cancel.shared)
                    missing.wrappedValue = nil
                }
            } else {
                Button("Cancel", role: .cancel) {
                    missing.wrappedValue = nil
                }
            }
        } message: { missing in
            Text(missing.wrappedValue!.descriptiveMessage)
        }
    }
}

extension View {
    func statusBar(feiState: FeiDbState?) -> some View {
        self.modifier(StatusBarModifier(feiState: feiState))
    }
}

#Preview {
    NavigationStack {
        Text("Example text here")
    }
    .statusBar(feiState: .InProgress(total: 3, done: 1))
}
