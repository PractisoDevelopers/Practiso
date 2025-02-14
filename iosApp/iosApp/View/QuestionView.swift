import Foundation
import SwiftUI
@preconcurrency import ComposeApp
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
    private var removeService = RemoveServiceSync(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var contentModel
    
    @State var data = OptionListData<OptionImpl<QuizOption>>()
    
    @State private var isGenericErrorShown = false
    @State private var genericErrorMessage: String?
    @State private var isArchiveImporterShown = false
    @State private var editMode: EditMode = .inactive
    @State private var selection = Set<Int64>()

    var body: some View {
        OptionList(
            data: data, selection: $selection,
            onDelete: { ids in
                for id in ids {
                    errorHandler.catchAndShowImmediately {
                        try removeService.removeQuizWithResources(id: id)
                    }
                }
            }
        ) { option in
            OptionListItem(data: option)
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) {
                        errorHandler.catchAndShowImmediately {
                            try removeService.removeQuizWithResources(id: option.id)
                        }
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
        }
        .environment(\.editMode, $editMode)
        .dropDestination(for: TransferItem.self) { data, _ in
            processImport(items: data)
        }
        .task {
            data.isRefreshing = true
            let service = LibraryService(db: Database.shared.app)
            for await items in service.getQuizzes() {
                DispatchQueue.main.schedule {
                    withAnimation {
                        data.items = items.map(OptionImpl.init)
                    }
                }
                data.isRefreshing = false
            }
        }
        .fileImporter(isPresented: $isArchiveImporterShown, allowedContentTypes: [.data], allowsMultipleSelection: true) { result in
            if let data = try? result.get() {
                _ = processImport(items: data.map { url in .url(url: url) })
            }
        }
        .toolbar {
            if editMode == .inactive {
                ToolbarItem {
                    Menu("Add", systemImage: "plus") {
                        Button("Import Archive", systemImage: "square.and.arrow.down.on.square") {
                            isArchiveImporterShown = true
                        }
                    }
                }
            }
        }
        .onChange(of: selection) { _, newValue in
            if !editMode.isEditing, let id = selection.first {
                contentModel.detail = .question(data.items.first(where: { option in
                    option.id == id
                })!.kt)
            }
        }
    }
    
    private func processImport(items: [TransferItem]) -> Bool {
        var packs: [ArchivePack] = []
        packs.reserveCapacity(items.count)
        
        for item in items {
            switch item {
            case .binary(let data):
                if let pack = (errorHandler.catchAndShowImmediately {
                    try importService.unarchive(it: Importable(data: data))
                }) {
                    packs.append(pack)
                } else {
                    return true
                }
            case .url(let url):
                if let pack = (errorHandler.catchAndShowImmediately {
                   try importService.unarchive(it: Importable(url: url))
                }) {
                    packs.append(pack)
                } else {
                    return true
                }
            case .error(let description):
                genericErrorMessage = description
                isGenericErrorShown = true
                return true
            }
        }
        
        for pack in packs {
            let states = importService.import(pack: pack)
            Task {
                for await state in states {
                    switch state {
                    case let c as ImportStateConfirmation:
                        await errorHandler.catchAndShowImmediately {
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
