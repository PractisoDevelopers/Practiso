import Foundation
import SwiftUI
import ComposeApp

struct StatusBarModifier : ViewModifier {
    let feiState: FeiDbState?
    @State private var missingModelState: FeiDbState.MissingModel? = nil
    @State private var pendingDownloadState: FeiDbState.PendingDownload? = nil

    func body(content: Content) -> some View {
        content.toolbar {
            toolbarContent
        }
        .missingModelAlert(stateBinding: $missingModelState)
        .downloadAlert(stateBinding: $pendingDownloadState)
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
        
        case .pendingDownload(let pending):
            ToolbarItem(placement: .status) {
                Button("Download required") {
                    pendingDownloadState = pending
                }
                .buttonStyle(.plain)
                .foregroundStyle(.red)
                .font(.caption)
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
                Button("Missing models") {
                    missingModelState = mms
                }
                .buttonStyle(.plain)
                .foregroundStyle(.red)
                .font(.caption)
            }
            
        case .collecting(_):
            ToolbarItem(placement: .status) {
                Text("Collecting frames...")
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
    
    fileprivate func downloadAlert(stateBinding: Binding<FeiDbState.PendingDownload?>) -> some View {
        alert("Pending Download", isPresented: Binding(get: {
            stateBinding.wrappedValue != nil
        }, set: { shown in
            if (!shown) {
                stateBinding.wrappedValue = nil
            }
        })) {
            if let response = stateBinding.wrappedValue?.response {
                Button("Now") {
                    response.trySend(element: PendingDownloadResponse.Immediate.shared)
                }
                Button("Only in WLAN") {
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
