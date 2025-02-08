import Foundation
import SwiftUI
import ComposeApp
import UniformTypeIdentifiers

private struct TransferItem: Transferable, Equatable, Sendable {
    
    public var url: URL
    
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(contentType: .item) { item in
            SentTransferredFile(item.url)
        } importing: { received in
            return Self(url: received.file)
        }
    }
}

struct QuestionView: View {
    @State var data = OptionListView.Data()
    private var importService = ImportService(db: Database.shared.app)

    var body: some View {
        OptionListView(data: data)
            .task {
                data.isRefreshing = true
                for await items in LibraryDataModel.shared.quiz {
                    data.items = items.map(Option.init)
                    data.isRefreshing = false
                }
            }
            .dropDestination(for: Data.self) { data, _ in
                var packs: [ArchivePack] = []
                packs.reserveCapacity(data.count)
                
                for dataItem in data {
                    if let pack = try? importService.unarchive(it: Importable(data: dataItem)) {
                        packs.append(pack)
                    } else {
                        return false
                    }
                }
                
                for pack in packs {
                    Task {
                        let states = importService.import(pack: pack)
                        for await state in states {
                            switch state {
                            case let c as ImportStateConfirmation:
                                try! await c.ok.send(element: nil)
                            default:
                                NSLog("Unknown state")
                            }
                        }
                    }
                }
                
                return true
            }
    }
}
