import ComposeApp
import Foundation
import SwiftUI

final class FeiStateDialogModel: ObservableObject {
    @State var missingModelDialog: FeiDbState.MissingModel? = nil
    @State var pendingDownloadDialog: FeiDbState.PendingDownload? = nil
    @State var errorState: FeiDbState.Error? = nil
    
    func show(_ state: FeiDbState) -> Bool {
        switch onEnum(of: state) {
        case .collecting(_):
            fallthrough
        case .downloadingInference(_):
            fallthrough
        case .inProgress(_):
            fallthrough
        case .ready(_):
            self.missingModelDialog = nil
            self.pendingDownloadDialog = nil
            self.errorState = nil
            return false
        case .missingModel(let state):
            self.missingModelDialog = state
            self.pendingDownloadDialog = nil
            self.errorState = nil
        case .pendingDownload(let state):
            self.pendingDownloadDialog = state
            self.missingModelDialog = nil
            self.pendingDownloadDialog = nil
            self.errorState = nil
        case .error(let state):
            self.errorState = state
            self.pendingDownloadDialog
        }
        return true
    }
}

extension View {
    func missingModelAlert(stateBinding: Binding<FeiDbState.MissingModel?>) -> some View {
        alert("Missing Models", isPresented: Binding(get: {
            stateBinding.wrappedValue != nil
        }, set: { shown in
            if !shown {
                stateBinding.wrappedValue = nil
            }
        }), presenting: stateBinding) { missing in
            if let proceed = missing.wrappedValue!.proceed {
                Button("Choose Another Model") {
                    Task {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            await UIApplication.shared.open(url)
                        }
                    }
                }
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

    func downloadAlert(stateBinding: Binding<FeiDbState.PendingDownload?>) -> some View {
        alert("Pending Download", isPresented: Binding(get: {
            stateBinding.wrappedValue != nil
        }, set: { shown in
            if !shown {
                stateBinding.wrappedValue = nil
            }
        })) {
            if let response = stateBinding.wrappedValue?.response {
                Button("Now") {
                    response.trySend(element: PendingDownloadResponse.Immediate.shared)
                }
                Button("Use WLAN Only") {
                    response.trySend(element: PendingDownloadResponse.Discretion.shared)
                }
                Button("Cancel", role: .cancel) {
                    stateBinding.wrappedValue = nil
                }
            }
        } message: {
            if let state = stateBinding.wrappedValue {
                let totalBytes = state.files.reduce(Int64.zero, { partialResult, curr in
                    partialResult + (curr.size?.int64Value ?? 1 >> 12)
                })
                let byteCount = Measurement(value: Double(totalBytes), unit: UnitInformationStorage.bytes)

                Text("About to download \(state.files.count) files, consuming about \(byteCount.formatted(.byteCount(style: .file))) of data. When would you like to start?")
            }
        }
    }

    func errorAlert(stateBinding: Binding<FeiDbState.Error?>) -> some View {
        alert("An error occurred", isPresented: Binding(get: {
            stateBinding.wrappedValue != nil
        }, set: { newValue in
            if !newValue {
                stateBinding.wrappedValue = nil
            }
        })) {
            Button("Retry") {
                stateBinding.wrappedValue!.proceed.trySend(element: FeiErrorResponse.Retry.shared)
            }
            Button("Cancel") {
                stateBinding.wrappedValue = nil
            }
        } message: {
            if let msg = stateBinding.wrappedValue?.error.appMessage {
                Text(String(errorMessage: msg))
            } else {
                Text("No details were reported.")
            }
        }
    }
    
    func feiStateDialog(present: Binding<FeiStateDialogModel?>) {
        
    }
}
