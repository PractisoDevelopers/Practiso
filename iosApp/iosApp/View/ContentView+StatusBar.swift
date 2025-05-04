import Foundation
import SwiftUI
import ComposeApp

struct StatusBarModifier : ViewModifier {
    let feiState: FeiDbState?
    @State private var missingModelState: FeiDbState.MissingModel? = nil

    func body(content: Content) -> some View {
        content.toolbar {
                switch onEnum(of: feiState) {
                case .collecting(_):
                    ToolbarItem(placement: .status) {
                        Text("Collecting Frames...")
                            .font(.caption)
                    }
                case .inProgress(let progress):
                    ToolbarItem(placement: .status) {
                        Text("Inferring \(progress.total) items...")
                            .font(.caption)
                    }
                    ToolbarItem(placement: .bottomBar) {
                        CircularProgressView(value: Float(progress.done) / Float(progress.total))
                    }
                case .missingModel(let state):
                    ToolbarItem(placement: .status) {
                        Button("Missing Models") {
                            missingModelState = state
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(.red)
                        .font(.caption)
                    }
                default:
                    ToolbarItem(placement: .status) {
                        EmptyView()
                    }
            }
        }
        .missingModelAlert(stateBinding: $missingModelState)
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

#Preview {
    NavigationStack {
        Text("Example text here")
    }
    .modifier(StatusBarModifier(feiState: .InProgress(total: 3, done: 1)))
}
