import Foundation
import UniformTypeIdentifiers
import ComposeApp
import CoreTransferable

extension UTType {
    static var psarchive: UTType { UTType(exportedAs: "com.zhufucdev.psarchive") }
}

enum SingleQuizTransferError : LocalizedError {
    case containsMultiple
    case empty
    
    var errorDescription: String? {
        switch self {
        case .containsMultiple:
            String(localized: "Data is invalid because it is compound.")
        case .empty:
            String(localized: "Data represents an empty archive.")
        }
    }
}

private func importQuiz(importable: Importable) throws -> QuizOption {
    let serivce = ImportServiceSync(db: Database.shared.app)
    do {
        let quizId = try serivce.importSingleton(importable: importable)
        
        let query = QueryService(db: Database.shared.app)
        return query.getQuizOption(quizId: quizId)!
    } catch is EmptyArchiveException {
        throw SingleQuizTransferError.empty
    } catch is ArchiveAssertionException {
        throw SingleQuizTransferError.containsMultiple
    }
}

func QuizOption(data: Data) throws -> QuizOption {
    try importQuiz(importable: Importable(data: data))
}

func QuizOption(url: URL) throws -> QuizOption {
    try importQuiz(importable: Importable(url: url))
}

fileprivate struct IdProxy : Codable, Transferable {
    let id: Int64
    
    static var transferRepresentation: some TransferRepresentation {
        CodableRepresentation(for: Self.self, contentType: .psarchive)
    }
}

extension QuizOption : @retroactive Transferable {
    func data() throws -> Data {
        let service = ExportServiceSync(db: Database.shared.app)
        return service.exportOneAsData(quizId: id)
    }
    
    public static var transferRepresentation: some TransferRepresentation {
        ProxyRepresentation { option in
            IdProxy(id: option.id)
        } importing: { proxy in
            let service = QueryService(db: Database.shared.app)
            if let option = service.getQuizOption(quizId: proxy.id) {
                return option
            }
            throw CocoaError(.fileNoSuchFile)
        }

        
        DataRepresentation(contentType: .psarchive) { option in
            try option.data()
        } importing: { data in
            try QuizOption(data: data)
        }.suggestedFileName(\.view.header)
        
        FileRepresentation(exportedContentType: .psarchive) { option in
            SentTransferredFile(
                NSURL.fileURL(withPath: NSTemporaryDirectory(), isDirectory: false)
                    .appendingPathComponent(option.view.header + ".psarchive")
            )
        }
        
        DataRepresentation(contentType: .gzip) { option in
            try option.data()
        } importing: { data in
            try QuizOption(data: data)
        }.suggestedFileName(\.view.header)
    }
}

