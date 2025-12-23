import ComposeApp
import Foundation
import SwiftUI

struct StatusBarModifier: ViewModifier {
    let title: LocalizedStringKey
    let feiState: FeiDbState?
    @State private var missingModelDialog: FeiDbState.MissingModel? = nil
    @State private var pendingDownloadDialog: FeiDbState.PendingDownload? = nil
    @State private var errorState: FeiDbState.Error? = nil

    func body(content: Content) -> some View {
        Group {
            if #available(iOS 26.0, *) {
                content
                    .navigationTitle(title)
                    .toolbar {
                        ToolbarItem(placement: .subtitle) {
                            actionStatus
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        ToolbarItem(placement: .topBarTrailing) {
                            progress
                        }
                    }
            } else {
                content.toolbar {
                    ToolbarItem(placement: .principal) {
                        VStack {
                            Text(title)
                                .fontWeight(.semibold)

                            actionStatus
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .missingModelAlert(stateBinding: $missingModelDialog)
        .downloadAlert(stateBinding: $pendingDownloadDialog)
        .errorAlert(stateBinding: $errorState)
    }

    var actionStatus: some View {
        Group {
            switch onEnum(of: feiState) {
            case let .inProgress(progress):
                Text("Inferring \(progress.total) items...")

            case .downloadingInference:
                Text("Downloading model...")

            case let .pendingDownload(state):
                Button("Download required") {
                    pendingDownloadDialog = state
                }

            case let .missingModel(state):
                Button("Missing models") {
                    missingModelDialog = state
                }

            case .collecting:
                Text("Collecting frames...")

            case let .error(error):
                Button("An error occurred") {
                    errorState = error
                }

            default:
                EmptyView()
            }
        }
    }

    var progress: some View {
        Group {
            switch onEnum(of: feiState) {
            case let .inProgress(progress):
                CircularProgressView(value: Float(progress.done) / Float(progress.total))

            case let .downloadingInference(progress):
                CircularProgressView(value: progress.progress)

            default:
                EmptyView()
            }
        }
    }
}

fileprivate struct SymmetricToolbarContent<Leading: View, Middle: View, Trailing: View>: View {
    @ViewBuilder
    let leading: () -> Leading
    @ViewBuilder
    let middle: () -> Middle
    @ViewBuilder
    let trailing: () -> Trailing

    var body: some View {
        HStack {
            leading()
                .padding(.leading)
            middle()
                .frame(maxWidth: .infinity)
            trailing()
                .padding(.trailing)
        }
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

    func titleBar(title: LocalizedStringKey, feiState: FeiDbState?) -> some View {
        modifier(StatusBarModifier(title: title, feiState: feiState))
    }
}

#Preview {
    NavigationStack {
        Text("Example text here")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    EditButton()
                }
            }
            .titleBar(title: "Practiso", feiState: .InProgress(total: 30, done: 25))
    }
}
