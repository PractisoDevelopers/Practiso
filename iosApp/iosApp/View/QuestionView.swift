import Foundation
import SwiftUI
import ComposeApp
import UniformTypeIdentifiers

private enum TransferItem: Transferable, Codable {
    case url(url: URL)
    case binary(data: Data)
    case error(String)
    
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(importedContentType: .data) { representation in
            let resultingUrl = NSURL.fileURL(withPath: NSTemporaryDirectory(), isDirectory: true)
                .appendingPathComponent(representation.file.lastPathComponent)
            do {
                try FileManager.default.copyItem(at: representation.file, to: resultingUrl)
                return TransferItem.url(url: representation.file)
            } catch {
                return TransferItem.error(error.localizedDescription)
            }
        }
        ProxyRepresentation {
            TransferItem.binary(data: $0)
        }
    }
}

private struct ListOrPlaceholder: View {
    @Binding var data: [Option]
    var processDrop: ([TransferItem]) -> Bool
    
    var body: some View {
        if data.isEmpty {
            OptionListPlaceholder()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(.background)
                .dropDestination(for: TransferItem.self) { data, _ in
                    processDrop(data)
                }
        } else {
            ScrollView {
                HStack {
                    Spacer()
                        .frame(width: 22)
                    LazyVStack(spacing: 8) {
                        ForEach(data) { option in
                            Color.secondary.opacity(0.3)
                                .frame(maxWidth: .infinity, maxHeight: 0.5)
                            OptionListView.Item(data: option)
                        }
                    }
                }
            }
            .dropDestination(for: TransferItem.self) { data, _ in
                processDrop(data)
            }
        }
    }
}

struct QuestionView: View {
    private var importService = ImportService(db: Database.shared.app)
    @State var data: [Option] = []
    @State private var isRefreshing = false
    @State private var isGenericImportFail = false
    @State private var genericImportErrorMessage: String?
    
    var body: some View {
        ListOrPlaceholder(data: $data, processDrop: processDrop)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    if isRefreshing {
                        ProgressView()
                    }
                }
            }
            .task {
                isRefreshing = true
                for await items in LibraryDataModel.shared.quiz {
                    data = items.map(Option.init)
                    isRefreshing = false
                }
            }
            .alert(
                "Failed to import the archive",
                isPresented: $isGenericImportFail,
                presenting: genericImportErrorMessage
            ) { _ in
                Button("Cancel", role: .cancel) {
                    isGenericImportFail = false
                    genericImportErrorMessage = nil
                }
            } message: { message in
                Text(message)
            }
    }
    
    private func catchAndShowImmediately(action: () async throws -> Void) async {
        do {
            try await action()
        } catch {
            genericImportErrorMessage = error.localizedDescription
            isGenericImportFail = true
        }
    }
    
    private func processDrop(items: [TransferItem]) -> Bool {
        var packs: [ArchivePack] = []
        packs.reserveCapacity(items.count)
        
        for item in items {
            switch item {
            case .binary(let data):
                if let pack = try? importService.unarchive(it: Importable(data: data)) {
                    packs.append(pack)
                } else {
                    return false
                }
            case .url(let url):
                if let pack = try? importService.unarchive(it: Importable(url: url)) {
                    packs.append(pack)
                } else {
                    return false
                }
            case .error(let description):
                genericImportErrorMessage = description
                isGenericImportFail = true
                return true
            }
        }
        
        for pack in packs {
            Task {
                let states = importService.import(pack: pack)
                for await state in states {
                    switch state {
                    case let c as ImportStateConfirmation:
                        await catchAndShowImmediately {
                            try await c.ok.send(element: nil)
                        }
                    default:
                        NSLog("Unknown state")
                    }
                }
            }
        }
        
        return true
    }
}

#Preview {
    @Previewable @State var data = (0..<10).map { i in
        Option(kt: QuizOption(quiz: Quiz(id: i, name: "Sample \(i+1)", creationTimeISO: Kotlinx_datetimeInstant.Companion.shared.DISTANT_FUTURE, modificationTimeISO: Kotlinx_datetimeInstant.Companion.shared.DISTANT_FUTURE), preview: "Sample preview for this quiz"))
    }
    ListOrPlaceholder(data: $data, processDrop: { _ in false })
}
