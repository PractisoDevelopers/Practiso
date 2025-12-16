import ComposeApp
import Foundation
import SwiftUI

struct StatusBarModifier: ViewModifier {
    let feiState: FeiDbState?
    @State private var missingModelDialog: FeiDbState.MissingModel? = nil
    @State private var pendingDownloadDialog: FeiDbState.PendingDownload? = nil
    @State private var errorState: FeiDbState.Error? = nil

    func body(content: Content) -> some View {
        content.toolbar {
            toolbarContent
        }
        .missingModelAlert(stateBinding: $missingModelDialog)
        .downloadAlert(stateBinding: $pendingDownloadDialog)
        .errorAlert(stateBinding: $errorState)
    }

    private func buildInProgress(progress: Float, total: Int32) -> some View {
        SymmetricToolbarContent {
            CircularProgressView(value: progress)
        } middle: {
            Text("Inferring \(total) items...")
                .font(.caption)
        } trailing: {
            Spacer()
                .frame(width: 24)
        }
    }

    private func buildDownloading(progress: Float) -> some View {
        SymmetricToolbarContent {
            CircularProgressView(value: progress)
        } middle: {
            Text("Downloading model...")
                .font(.caption)
        } trailing: {
            Spacer()
                .frame(width: 24)
        }
    }

    private func buildPendingDownload(action: @escaping () -> Void) -> some View {
        SymmetricToolbarContent {
            if #available(iOS 26.0, *) {
                Image(systemName: "arrow.down.circle.dotted")
                    .symbolEffect(.wiggle.byLayer, options: .repeat(.periodic(delay: 0.5)))
                    .frame(minWidth: 24)
            } else {
                Image(systemName: "arrow.down.circle.dotted")
                    .frame(minWidth: 24)
            }
        } middle: {
            Button("Download required", action: action)
                .buttonStyle(.plain)
                .font(.caption)
        } trailing: {
            Spacer()
                .frame(width: 24)
        }
    }

    private func buildMissingDialog(action: @escaping () -> Void) -> some View {
        SymmetricToolbarContent {
            Image(systemName: "gear.badge.questionmark")
                .frame(minWidth: 24)
        } middle: {
            Button("Missing models", action: action)
                .buttonStyle(.plain)
                .font(.caption)
        } trailing: {
            Spacer()
                .frame(width: 24)
        }
    }

    private func buildCollectingItems() -> some View {
        SymmetricToolbarContent {
            Image(systemName: "text.page.badge.magnifyingglass")
                .symbolEffect(.wiggle.byLayer, options: .repeat(.periodic(delay: 0.5)))
                .frame(minWidth: 24)
        } middle: {
            Text("Collecting frames...")
                .font(.caption)
        } trailing: {
            Spacer()
                .frame(width: 24)
        }
    }

    private func buildError(message: AppMessage?, action: @escaping () -> Void) -> some View {
        SymmetricToolbarContent {
            Image(systemName: "exclamationmark.octagon")
                .frame(minWidth: 24)
        } middle: {
            Button(message != nil ? String(errorMessage: message!) : String(localized: "An error occurred"), action: action)
                .buttonStyle(.plain)
                .font(.caption)
        } trailing: {
            Spacer()
                .frame(width: 24)
        }
    }

    @ToolbarContentBuilder
    var toolbarContent: some ToolbarContent {
        switch onEnum(of: feiState) {
        case let .inProgress(progress):
            ToolbarItem(placement: .bottomBar) {
                buildInProgress(progress: Float(progress.done) / Float(progress.total), total: progress.total)
            }

        case let .downloadingInference(download):
            ToolbarItem(placement: .bottomBar) {
                buildDownloading(progress: download.progress)
            }

        case let .pendingDownload(pending):
            ToolbarItem(placement: .status) {
                buildPendingDownload {
                    pendingDownloadDialog = pending
                }
            }

        default:
            toolbarContent_2
        }
    }

    @ToolbarContentBuilder
    var toolbarContent_2: some ToolbarContent {
        switch onEnum(of: feiState) {
        case let .missingModel(mms):
            ToolbarItem(placement: .bottomBar) {
                buildMissingDialog {
                    missingModelDialog = mms
                }
            }
        case .collecting:
            ToolbarItem(placement: .bottomBar) {
                buildCollectingItems()
            }
        case let .error(error):
            ToolbarItem(placement: .bottomBar) {
                buildError(message: error.error.appMessage) {
                    errorState = error
                }
            }

        default:
            ToolbarItem(placement: .status) {
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
    fileprivate func missingModelAlert(stateBinding: Binding<FeiDbState.MissingModel?>) -> some View {
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

    fileprivate func downloadAlert(stateBinding: Binding<FeiDbState.PendingDownload?>) -> some View {
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

    fileprivate func errorAlert(stateBinding: Binding<FeiDbState.Error?>) -> some View {
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

    func statusBar(feiState: FeiDbState?) -> some View {
        modifier(StatusBarModifier(feiState: feiState))
    }
}

#Preview {
    NavigationStack {
        Text("Example text here")
    }
    .statusBar(feiState: .Collecting())
}
