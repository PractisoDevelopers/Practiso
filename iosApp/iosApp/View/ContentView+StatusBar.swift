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
