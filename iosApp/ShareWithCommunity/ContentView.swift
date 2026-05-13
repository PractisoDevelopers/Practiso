import AsyncAlgorithms
import Combine
@preconcurrency import ComposeApp
import Foundation
import SwiftUI
@_spi(Advanced) import SwiftUIIntrospect

struct ContentView: View {
    @Environment(\.archiveResource) private var archive
    @Environment(\.finishTask) private var finishTask

    enum Step: Hashable {
        case fileName(submission: Ref<AsyncChannel<String>>)
        case signUp(submission: Ref<AsyncChannel<CommunityRegistrationInfo>>)
        case upload(progess: UploadArchive.InProgress)
        case invalidIdentity(UploadArchive.SignOffRequired)
        case success(archiveId: String)
        case failure(UploadArchive.Failure)
    }

    @State private var path: [Step] = []
    @State private var errorMessage: String? = nil
    @State private var stateChange = AsyncChannel<Void>()
    @State private var retryCounter = 0

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if case let .failure(error) = archive.resource {
                    Text(error.localizedDescription)
                        .padding()
                } else {
                    ProgressView()
                        .controlSize(.large)
                        .task {
                            for await _ in stateChange {
                                break
                            }
                        }
                        .onAppear {
                            retryCounter += 1
                        }
                }
            }
            .navigationDestination(for: Step.self) { step in
                currentView(step)
                    .environment(\.archiveResource, archive)
                    .navigationBarBackButtonHidden()
                    .toolbar {
                        Button("Cancel", systemImage: "xmark", role: .cancel) {
                            finishTask()
                        }
                    }
            }
            .toolbar {
                Button("Cancel", systemImage: "xmark", role: .cancel) {
                    finishTask()
                }
            }
        }
        .onChange(of: path) { oldValue, newValue in
            if oldValue.count > 1 && newValue.count == 1 {
                retryCounter += 1
            }
        }
        .task(id: Pair(a: archive.resource, b: retryCounter)) {
            guard case let .success(resource) = archive.resource else {
                return
            }

            path.removeAll()
            let updates = switch resource {
            case let .data(data, _):
                AppCommunityService.shared.upload(data: data, contentName: nil)
            case let .url(url):
                AppCommunityService.shared.upload(contentsOf: url, contentName: nil)
            }
            for await update in updates {
                await stateChange.send(())
                stateChange.finish()
                stateChange = .init()

                switch onEnum(of: update) {
                case let .archiveNameRequired(state):
                    let receiver = AsyncChannel<String>()
                    path.append(.fileName(submission: Ref(value: receiver)))
                    for await submission in receiver {
                        await catchAndShow {
                            try await state.submission.send(element: submission)
                        }
                        break
                    }

                case let .registrationRequired(state):
                    let receiver = AsyncChannel<CommunityRegistrationInfo>()
                    path.append(.signUp(submission: Ref(value: receiver)))
                    for await submission in receiver {
                        await catchAndShow {
                            try await state.submission.send(element: submission)
                        }
                        break
                    }

                case let .inProgress(state):
                    path.append(.upload(progess: state))

                case let .failure(state):
                    path.append(.failure(state))

                case let .signOffRequired(state):
                    path.append(.invalidIdentity(state))

                case let .success(state):
                    path.append(.success(archiveId: state.archiveId))
                }
            }
        }
        .alert("Unexpected error", isPresented: Binding(get: {
            errorMessage != nil
        }, set: { newValue in
            if !newValue {
                errorMessage = nil
            }
        })) {
            Button("Cancel", role: .cancel) {
                finishTask()
            }
        } message: {
            if let errorMessage {
                Text(errorMessage)
            }
        }
    }

    func currentView(_ step: Step) -> some View {
        Group {
            switch step {
            case let .fileName(submission):
                FileNameView(onContinue: { name in
                    await submission.value.send(name)
                    for await _ in stateChange {
                        break
                    }
                })
                .navigationTitle("Archive name")
                .navigationBarTitleDisplayMode(.large)

            case let .signUp(submission):
                SignUpView(onContinue: { registration in
                    await submission.value.send(registration)
                    for await _ in stateChange {
                        break
                    }
                })
                .navigationTitle("Create account")
                .navigationBarTitleDisplayMode(.large)

            case let .upload(progress):
                UploadView(progress: progress)
                    .task {
                        for await _ in stateChange {
                            break
                        }
                    }

            case let .invalidIdentity(state):
                InvalidIdentityView()

            case let .success(archiveId):
                SuccessView(archiveId: archiveId, onClose: {
                    finishTask()
                })
                .task {
                    try? await Task.sleep(for: .seconds(5))
                    finishTask()
                }

            case let .failure(state):
                FailureView(message: state.message, code: state.statusCode.value) {
                    retryCounter += 1
                } onCancel: {
                    finishTask()
                }
            }
        }
    }

    func catchAndShow(block: () async throws -> Void) async {
        do {
            try await block()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

fileprivate struct FileNameView: View {
    @Environment(\.archiveResource) private var archive
    @State private var value = ""
    @State private var loading = false
    @State private var wobbling = false
    @State private var shake = PassthroughSubject<Void, Never>()

    let onContinue: (String) async -> Void

    var body: some View {
        VStack {
            Form {
                Section {
                    TextField("Archive name", text: $value)
                        .shakeAnimation($wobbling, sink: shake)
                        .textInputAutocapitalization(.words)
                        .introspect(.textField, on: .iOS(.v13...)) { textField in
                            textField.clearButtonMode = .whileEditing
                        }
                } footer: {
                    Text("Used to identify the content within for others")
                }
            }

            Spacer()
            FullWidthButton {
                loading = true
                Task {
                    await onContinue(value)
                    loading = false
                }
            } label: {
                HStack {
                    if loading {
                        ProgressView()
                    }
                    Text("Continue")
                }
            }
            .disabled(loading || value.isEmpty)
            .padding()
        }
        .onAppear {
            if case let .success(resource) = archive.resource {
                if case let .url(url) = resource,
                   let name = url.lastPathComponent.split(separator: ".").first {
                    value = String(name)
                } else if case let .data(_, fileName) = resource, let fileName {
                    value = fileName
                }
            }
        }
    }
}

fileprivate struct SignUpView: View {
    let onContinue: (CommunityRegistrationInfo) async -> Void

    @State private var userName = ""
    @State private var deviceName = "\(DeviceIdentifier.default)"
    @State private var loading = false

    var body: some View {
        VStack {
            Form {
                Section {
                    TextField("Device name", text: $deviceName)
                    TextField("User name", text: $userName)
                } footer: {
                    Text("Default user name is the same as device name.")
                }
            }
            Spacer()
            FullWidthButton {
                loading = true
                Task {
                    await onContinue(.init(clientName: deviceName, ownerName: userName.isEmpty ? deviceName : userName))
                    loading = false
                }
            } label: {
                HStack {
                    if loading {
                        ProgressView()
                    }
                    Text("Continue")
                }
            }
            .disabled(loading || deviceName.isEmpty)
            .padding()
        }
    }
}

fileprivate struct UploadView: View {
    let progress: UploadArchive.InProgress

    @State private var timeEstimator = TimeRemainingEstimator()

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "arrow.trianglehead.2.clockwise.rotate.90")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 48)
                .foregroundStyle(Color.accentColor)
                .symbolEffect(.rotate, options: .repeat(.continuous))
                .padding(.top, 18)

            Spacer()
            Observing(progress.stats) { stats in
                if let bytesTotal = stats.bytesTotal?.int64Value {
                    ProgressView(value: Float(stats.bytesSent) / Float(bytesTotal)) {
                        if let eta = timeEstimator.elapsed(done: stats.bytesSent, total: bytesTotal) {
                            Text("\(eta.formatted()) remaining")
                        } else {
                            Text("Estimating time remaining...")
                        }
                    }
                } else {
                    ProgressView {
                        Text("Uploading...")
                    }
                }
            }
            Spacer()
        }
        .padding()
    }
}

fileprivate struct FailureView: View {
    let message: String?
    let code: Int32
    let onRetry: () -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Image(systemName: "square.and.arrow.up.trianglebadge.exclamationmark")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 48)
            }
            .frame(maxWidth: .infinity)
            .padding(.bottom, 24)

            Text("Upload failed")
                .font(.title)
            Text("Status code \(code)")
            if let message {
                Text(message)
            }

            Spacer()

            FullWidthButton {
                onRetry()
            } label: {
                Text("Retry")
            }

            FullWidthButton {
                onCancel()
            } label: {
                Text("Cancel")
            }
            .tint(.clear)
        }
        .padding(.horizontal)
    }
}

fileprivate struct InvalidIdentityView: View {
    var body: some View {
    }
}

fileprivate struct SuccessView: View {
    @Environment(\.openURL) private var openURL

    let archiveId: String
    let onClose: () -> Void

    @State private var secsUntilClose = 5

    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Image(systemName: "checkmark.circle")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 48)
                    .foregroundStyle(.green)
            }
            .frame(maxWidth: .infinity)
            .padding(.bottom, 24)
            Text("Completed!")
                .font(.title)
                .bold()
            Text("Others can view and download your quizzes, and share with their friends")
            Spacer()

            FullWidthButton(action: onClose, label: {
                Text("Closing in \(secsUntilClose)")
            })
            .task(id: archiveId) {
                while secsUntilClose > 0 {
                    try? await Task.sleep(for: .seconds(1))
                    secsUntilClose -= 1
                }
            }
            FullWidthButton {
                openURL(URL(string: "\(ComposeApp.Protocol_.Companion.shared.revealCommunityArchive(id: archiveId))")!)
            } label: {
                Text("Reveal in Practiso")
            }
            .tint(.clear)
        }
        .padding(.horizontal)
    }
}

fileprivate struct FullWidthButton<Label: View>: View {
    let action: () -> Void
    @ViewBuilder let label: () -> Label

    var body: some View {
        if #available(iOS 26.0, *) {
            Button(action: action) {
                wrappedLabel
            }
            .buttonStyle(.glassProminent)
            .buttonBorderShape(.capsule)
        } else {
            Button(action: action) {
                wrappedLabel
            }
            .buttonStyle(.borderedProminent)
            .buttonBorderShape(.capsule)
        }
    }

    var wrappedLabel: some View {
        label()
            .bold()
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
    }
}

fileprivate struct Pair<A, B>: Equatable where A: Equatable, B: Equatable {
    var a: A
    var b: B
}
