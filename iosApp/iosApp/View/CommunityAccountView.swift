import AsyncAlgorithms
import CodeScanner
@preconcurrency import ComposeApp
import Foundation
import SwiftUI

struct CommunityAccountView: View {
    @State private var state: PageState = .loading
    @State private var refreshCounter = 0
    @State private var refreshChannel = AsyncChannel<Void>()

    var body: some View {
        CommunityAccountViewImpl(state: $state)
            .task(id: refreshCounter) {
                for await update in AppCommunityService.shared.getWhoami() {
                    if let update {
                        state = .some(info: update)
                    } else {
                        state = .none
                    }
                    Task {
                        await refreshChannel.send(())
                    }
                }
            }
            .refreshable {
                refreshChannel.finish()
                refreshChannel = .init()

                refreshCounter += 1
                for await _ in refreshChannel {
                    break
                }
            }
    }
}

fileprivate enum PageState {
    case loading
    case some(info: Whoami)
    case none
}

fileprivate struct CommunityAccountViewImpl: View {
    @Binding var state: PageState

    var body: some View {
        switch state {
        case .loading:
            ProgressView()
                .controlSize(.large)
        case let .some(info):
            HasAccount(info: Binding(get: {
                info
            }, set: { newValue in
                state = .some(info: newValue)
            }))
        case .none:
            NoAccount()
        }
    }
}

fileprivate struct HasAccount: View {
    @Environment(ContentView.ErrorHandler.self) var errorHandler: ContentView.ErrorHandler?
    @Environment(\.refresh) var refresh

    @Binding var info: Whoami

    @State private var changingName = false
    @State private var deactivating = false
    @State private var showRenameDialog = false
    @State private var showLogoutDialog = false
    @State private var showDeactivateDialog = false
    @State private var showCredentials = false
    @State private var userNameBuffer: String
    @State private var deviceNameBuffer: String

    init(info: Binding<Whoami>) {
        _info = info
        userNameBuffer = info.wrappedValue.name ?? ""
        deviceNameBuffer = info.wrappedValue.clientName
    }

    var body: some View {
        Form {
            Section {
                Button {
                    userNameBuffer = info.name ?? ""
                    deviceNameBuffer = info.clientName
                    showRenameDialog = true
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "person.crop.circle")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 32)
                        VStack(alignment: .leading) {
                            if let username = info.name {
                                Text(username)
                                    .font(.title3)
                                Text(info.clientName)
                                    .fontDesign(.monospaced)
                                    .foregroundStyle(.secondary)
                            } else {
                                Text(info.clientName)
                                    .font(.title3)
                            }
                        }
                        .foregroundStyle(.foreground)

                        Spacer()
                        if changingName {
                            ProgressView()
                        }
                    }
                }
                .disabled(changingName || deactivating)
            }
            Section {
                Button("Show Credentials") {
                    showCredentials = true
                }
                .popover(isPresented: $showCredentials) {
                    CredentialsView()
                        .padding()
                }
                Button("Log Out", role: .destructive) {
                    showLogoutDialog = true
                }
                Button("Deactivate", role: .destructive) {
                    showDeactivateDialog = true
                }
            }
            .disabled(deactivating)
        }
        .contentMargins(.top, .zero)
        .alert("Name change", isPresented: $showRenameDialog) {
            TextField("User name", text: $userNameBuffer)
            TextField("Device name", text: $deviceNameBuffer)

            Button("Cancel", role: .cancel) {
                showRenameDialog = false
            }
            Button("OK") {
                Task {
                    await submitNameChange(userName: userNameBuffer, deviceName: deviceNameBuffer)
                }
            }
            .disabled(deviceNameBuffer.isEmpty)
        }
        .alert("Logging out", isPresented: $showLogoutDialog) {
            Button("Cancel", role: .cancel) {
                showLogoutDialog = false
            }
            Button("Continue", role: .destructive) {
                Task {
                    do {
                        try await AppCommunityService.shared.clearIdentity()
                    } catch {
                        errorHandler?.show(error: error)
                    }
                }
            }
        } message: {
            Text("Once logged out on all your devices, your account will become inaccessible, " +
                "while your data holds still. Would you like to continue?")
        }
        .alert("Deactivating account", isPresented: $showDeactivateDialog) {
            Button("Cancel", role: .cancel) {
                showDeactivateDialog = false
            }
            Button("Continue", role: .destructive) {
                Task {
                    deactivating = true
                    do {
                        try await AppCommunityService.shared.deleteWhoami()
                        try await AppCommunityService.shared.clearIdentity()
                        await refresh?()
                    } catch {
                        errorHandler?.show(error: error)
                    }
                    deactivating = false
                }
            }
        } message: {
            Text("You are erasing your account along with all your uploaded data. Would you like to continue?")
        }
    }

    func submitNameChange(userName: String, deviceName: String) async {
        let name: SetField = if userName.isEmpty {
            if info.name != nil {
                SetFieldUpdateStringNil()
            } else {
                SetFieldUnchangedString()
            }
        } else {
            if info.name == userName {
                SetFieldUnchangedString()
            } else {
                SetFieldUpdateString(value: userName)
            }
        }
        let clientName: SetField = if deviceName != info.clientName {
            SetFieldUpdateString(value: deviceName)
        } else {
            SetFieldUnchangedString()
        }

        changingName = true
        do {
            try await AppCommunityService.shared.setWhoami(info: SetWhoami(name: name, clientName: clientName))
            info = Whoami(clientName: deviceName, name: userName.isEmpty ? nil : userName, ownerId: info.ownerId, mode: info.mode)
        } catch {
            errorHandler?.show(error: error)
        }
        changingName = false
    }
}

fileprivate struct NoAccount: View {
    @State private var showTypeInDialog = false
    @State private var showScanningDialog = false
    @State private var showEmptyScanDialog = false
    @State private var accessTokenBuffer = ""
    @State private var tokenToAdd: String? = nil
    @State private var addAccountStateChannel = AsyncChannel<AddAccountState>()
    @State private var disabled = false
    @State private var addAccountError: AddAccountError? = nil

    var body: some View {
        Form {
            Section {
                Text("No account")
            }
            Section {
                Button("Via QR Code") {
                    showScanningDialog = true
                    Task {
                        await subscribeToAddAccountNotifications()
                    }
                }
                Button("Type In Token") {
                    accessTokenBuffer = ""
                    showTypeInDialog = true
                    Task {
                        await subscribeToAddAccountNotifications()
                    }
                }
            } header: {
                Text("Add existing")
            } footer: {
                Text("You may create new accounts by sharing your content")
            }
            .disabled(disabled)
        }
        .contentMargins(.top, .zero)
        .alert("Add account", isPresented: $showTypeInDialog) {
            TextField("Access token", text: $accessTokenBuffer)
            Button("Continue") {
                tokenToAdd = accessTokenBuffer
            }
            Button("Cancel", role: .cancel) {
                showTypeInDialog = false
            }
        }
        .addAccountAlert($tokenToAdd, stateNotifier: addAccountStateChannel)
        .alert(isPresented: $addAccountError.isNotNil(), error: addAccountError) {
            Button("Cancel", role: .cancel) {
            }
        }
        .sheet(isPresented: $showScanningDialog) {
            CodeScannerView(codeTypes: [.qr]) { result in
                switch result {
                case let .success(success):
                    guard let url = URL(string: success.string) else {
                        finishScanningEmptyhanded()
                        return
                    }
                    guard let dest = AppLinkDestination(url: url), case let .addAccount(token) = dest else {
                        finishScanningEmptyhanded()
                        return
                    }
                    tokenToAdd = token

                case .failure:
                    finishScanningEmptyhanded()
                }
            }
        }
        .alert("Add account", isPresented: $showEmptyScanDialog) {
            Button("OK", role: .cancel) {
                showEmptyScanDialog = false
            }
        } message: {
            Text("Nothing was scanned")
        }
    }

    func finishScanningEmptyhanded() {
        showScanningDialog = false
        showEmptyScanDialog = true
    }

    func subscribeToAddAccountNotifications() async {
        addAccountStateChannel = .init()
        for await update in addAccountStateChannel {
            switch update {
            case .working:
                disabled = true
            case .success:
                disabled = false
            case let .error(err):
                disabled = false
                addAccountError = err
            }
        }
    }
}

fileprivate struct CredentialsView: View {
    var body: some View {
        Observing(AppCommunityService.shared.getAuthToken()) {
            ProgressView()
        } content: { token in
            if let token {
                CredentialsViewImpl(token: token)
            } else {
                Text("You are logged out")
                    .foregroundStyle(.secondary)
            }
        }
    }
}

fileprivate struct CredentialsViewImpl: View {
    let token: String

    @Environment(\.displayScale) private var displayScale
    @State private var qrCodeImage: CGImage? = nil

    var body: some View {
        VStack(spacing: 12) {
            VStack {
                if let qrCodeImage {
                    Image(decorative: qrCodeImage, scale: displayScale)
                        .interpolation(.none)
                        .antialiased(false)
                } else {
                    ProgressView()
                    Text("Generating QR code...")
                        .foregroundStyle(.secondary)
                        .font(.caption)
                }
            }
            .frame(minHeight: 200)
            Text(token)
                .textSelection(.enabled)
        }
        .onAppear {
            qrCodeImage = qrCode(from: "\(ComposeApp.Protocol_.Companion.shared.importAuthToken(token: token))",
                                 targetSize: .init(width: 200, height: 200), displayScale: displayScale)
        }
    }
}

#Preview {
    VStack {
        Text("None:")
        CommunityAccountViewImpl(state: Binding.constant(.none))

        Text("No name:")
        CommunityAccountViewImpl(state: Binding.constant(.some(info: Whoami(clientName: "iPhone", name: nil, ownerId: 1, mode: 111))))

        Text("Named:")
        CommunityAccountViewImpl(state: Binding.constant(.some(info: Whoami(clientName: "iPhone", name: "Caturday", ownerId: 2, mode: 111))))

        Text("Credentials:")
        CredentialsViewImpl(token: "example-token")
    }
}
