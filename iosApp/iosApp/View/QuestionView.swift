import Foundation
import SwiftUI
import ComposeApp
import UniformTypeIdentifiers

enum TransferItem: Transferable, Codable {
    case url(url: URL)
    case binary(data: Data)
    case error(String)
    
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(importedContentType: .data) { representation in
            let resultingUrl = NSURL.fileURL(withPath: NSTemporaryDirectory(), isDirectory: true)
                .appendingPathComponent(representation.file.lastPathComponent)
            do {
                _ = try FileManager.default.replaceItemAt(resultingUrl, withItemAt: representation.file)
                return TransferItem.url(url: resultingUrl)
            } catch {
                return TransferItem.error(error.localizedDescription)
            }
        }
        ProxyRepresentation {
            TransferItem.binary(data: $0)
        }
    }
}

struct QuestionView: View {
    private var importService = ImportService(db: Database.shared.app)
    private var removeService = RemoveService(db: Database.shared.app)
    @State var data = OptionListData()
    @State private var isGenericErrorShown = false
    @State private var genericErrorMessage: String?
    
    var body: some View {
        OptionListView(data: data) { option in
            OptionListItem(data: option)
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) {
                        Task {
                            await catchAndShowImmediately {
                                try await removeService.removeQuizWithResources(id: option.id)
                            }
                        }
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
        }
            .dropDestination(for: TransferItem.self) { data, _ in
                processDrop(items: data)
            }
            .task {
                data.isRefreshing = true
                for await items in LibraryDataModel.shared.quiz {
                    data.items = items.map(Option.init)
                    data.isRefreshing = false
                }
            }
            .alert(
                "Operation failed",
                isPresented: $isGenericErrorShown,
                presenting: genericErrorMessage
            ) { _ in
                Button("Cancel", role: .cancel) {
                    isGenericErrorShown = false
                    genericErrorMessage = nil
                }
            } message: { message in
                Text(message)
            }
    }
    
    private func catchAndShowImmediately(action: () async throws -> Void) async {
        do {
            try await action()
        } catch {
            genericErrorMessage = error.localizedDescription
            isGenericErrorShown = true
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
                genericErrorMessage = description
                isGenericErrorShown = true
                return true
            }
        }
        
        for pack in packs {
            let states = importService.import(pack: pack)
            Task.detached {
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
