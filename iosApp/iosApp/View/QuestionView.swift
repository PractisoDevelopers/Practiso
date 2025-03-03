import Foundation
import SwiftUI
@preconcurrency import ComposeApp
import UniformTypeIdentifiers

struct QuestionView: View {
    private let archiveService = ImportService(db: Database.shared.app)
    private let importService = ImportServiceSync(db: Database.shared.app)
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    private let createService = CreateService(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var contentModel
    
    @State var data = OptionListData<OptionImpl<QuizOption>>()
    
    @State private var isArchiveImporterShown = false
    @State private var editMode: EditMode = .inactive
    @State private var selection = Set<Int64>()

    var body: some View {
        OptionList(
            data: data, selection: Binding(get: {
                selection
            }, set: { newValue in
                if !editMode.isEditing, let id = newValue.first {
                    contentModel.detail = .question(data.items.first(where: {$0.id == id})!.kt)
                }
                selection = newValue
            }),
            onDelete: { options in
                for option in options {
                    errorHandler.catchAndShowImmediately {
                        try removeService.removeQuizWithResources(id: option)
                    }
                }
            }
        ) { option in
            OptionListItem(data: option)
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) {
                        errorHandler.catchAndShowImmediately {
                            try removeService.removeQuizWithResources(id: option.kt.id)
                        }
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
                .draggable(option.kt)
                .contextMenu {
                    Button("Delete \(option.view.header)", systemImage: "trash", role: .destructive) {
                        errorHandler.catchAndShowImmediately {
                            try removeService.removeQuizWithResources(id: option.kt.id)
                        }
                    }
                } preview: {
                    QuestionPreview(data: option.kt.quiz)
                }

        }
        .toolbar {
            if editMode == .inactive {
                ToolbarItem {
                    Menu("Add", systemImage: "plus") {
                        Button("Import Archive", systemImage: "square.and.arrow.down.on.square") {
                            isArchiveImporterShown = true
                        }
                        
                        Button("Create", systemImage: "document.badge.plus") {
                            Task {
                                let created = try await createService.createNewQuiz()
                                contentModel.detail = .question(created)
                            }
                        }
                    }
                }
            }
        }
        .environment(\.editMode, $editMode)
        .dropDestination(for: CopiedTransfer.self) { data, _ in
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
    }
    
    private func processImport(items: [CopiedTransfer]) -> Bool {
        var packs: [ArchivePack] = []
        packs.reserveCapacity(items.count)
        
        for item in items {
            switch item {
            case .binary(let data):
                if let pack = (errorHandler.catchAndShowImmediately {
                    try archiveService.unarchive(it: Importable(data: data))
                }) {
                    packs.append(pack)
                } else {
                    return true
                }
            case .url(let url):
                if let pack = (errorHandler.catchAndShowImmediately {
                   try archiveService.unarchive(it: Importable(url: url))
                }) {
                    packs.append(pack)
                } else {
                    return true
                }
            case .error(let description):
                errorHandler.show(message: description)
                return true
            }
        }
        
        for pack in packs {
            errorHandler.catchAndShowImmediately {
                try importService.importAll(pack: pack)
            }
        }
        
        return true
    }
}
