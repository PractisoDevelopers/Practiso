import Foundation
import Combine
import SwiftUI
import PhotosUI
import ComposeApp

struct FrameEditor<ImageFrameEditorLabel : View, TextFrameEditor : View> : View {
    @Binding var frame: Frame
    let imageFrameEditorLabel: (ImageFrame, Binding<ImageFrameView.DataState?>) -> ImageFrameEditorLabel
    let textFrameEditor: (Binding<String>) -> TextFrameEditor
    let onDelete: () -> Void
    
    var body: some View {
        switch frame {
        case let text as FrameText:
            textFrameEditor(Binding(get: {
                text.textFrame.content
            }, set: { newValue, _ in
                if newValue == text.textFrame.content {
                    return
                }
                let textFrame = TextFrame(id: text.textFrame.id, embeddingsId: text.textFrame.embeddingsId, content: newValue)
                frame = FrameText(id: frame.id, textFrame: textFrame)
            }))
            .onKeyPress(.delete) {
                if text.textFrame.content.isEmpty {
                    onDelete()
                    return .handled
                }
                return .ignored
            }
        case let image as FrameImage:
            ImageFrameEditor(frame: Binding(get: {
                image.imageFrame
            }, set: { newValue, _ in
                frame = FrameImage(id: image.id, imageFrame: newValue)
            }), label: imageFrameEditorLabel)
            .frame(maxWidth: .infinity, alignment: .leading)
        default:
            Question.UnknownItem(frame: frame)
        }
    }
}

struct DebouncedTextField : View {
    private class Model : ObservableObject, Observable, Cancellable {
        @Published var textBuffer: String? = nil
        @Published var textOutput: String? = nil
        
        private var subscriptions = Set<AnyCancellable>()
        
        init() {
            $textBuffer.debounce(for: .milliseconds(400), scheduler: DispatchQueue.main)
                .sink { [weak self] output in
                    self?.textOutput = output
                }
                .store(in: &subscriptions)
        }
        
        func snapTo(text: String) {
            textOutput = text
            textBuffer = text
        }
        
        func cancel() {
            subscriptions.forEach { $0.cancel() }
        }
    }
    
    @Binding var text: String
    @StateObject private var model = Model()
    
    var body: some View {
        TextField(text: Binding(get: {
            model.textBuffer ?? text
        }, set: {
            model.textBuffer = $0
        }), axis: .vertical, label: {
            Text("Type text here...")
        })
        .onAppear {
            model.snapTo(text: text)
        }
        .onDisappear {
            if let buffer = model.textBuffer {
                text = buffer
            }
            model.cancel()
        }
        .onReceive(model.$textOutput) { output in
            if output != text, let output = output {
                text = output
            }
        }
        .onChange(of: text) { _, newValue in
            if newValue != model.textOutput {
                model.snapTo(text: newValue)
            }
        }
    }
}

private struct DataUrl: Transferable {
    let url: URL

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(contentType: .data) { data in
            SentTransferredFile(data.url)
        } importing: { received in
            Self(url: received.file)
        }
    }
}

struct ImageFrameEditor<Label : View> : View {
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ImageFrameView.Cache.self) private var cache
    @Environment(\.imageService) private var loader
    
    @Binding var frame: ImageFrame
    let label: (ImageFrame, Binding<ImageFrameView.DataState?>) -> Label
    var importService = ImportService(db: Database.shared.app)

    @State private var isFileImporter = false
    @State private var isPhotosPicker = false
    @State private var isEditingAltText = false
    @State private var altTextBuffer: String? = nil
    @State private var pick: PhotosPickerItem?
    @State private var data: ImageFrameView.DataState?
    
    var body: some View {
        Menu {
            Section("Replace With") {
                Button("Photo from Library", systemImage: "photo.on.rectangle.angled") {
                    isPhotosPicker = true
                }
                Button("Image File", systemImage: "document") {
                    isFileImporter = true
                }
            }
            Section {
                Button("Alternative Text", systemImage: "text.below.photo") {
                    altTextBuffer = frame.altText
                    isEditingAltText = true
                }
            }
        } label: {
            label(frame, $data)
        }
        .fileImporter(isPresented: $isFileImporter, allowedContentTypes: [.image]) { result in
            switch result {
            case .success(let url):
                errorHandler.catchAndShowImmediately {
                    try importImage(from: url)
                }
            case .failure(let error):
                errorHandler.show(error: error)
            }
        }
        .photosPicker(isPresented: $isPhotosPicker, selection: $pick, preferredItemEncoding: .compatible)
        .alert("Alternative Text", isPresented: $isEditingAltText) {
            TextField("Briefly describe this image...", text: Binding(get: {
                altTextBuffer ?? ""
            }, set: { newValue in
                altTextBuffer = newValue
            }))
            Button("OK") {
                if altTextBuffer?.isEmpty == true {
                    altTextBuffer = nil
                }
                frame = ImageFrame(id: frame.id, embeddingsId: frame.embeddingsId, filename: frame.filename, width: frame.width, height: frame.height, altText: altTextBuffer)
            }
            Button("Cancel", role: .cancel) {
            }
        }
        .onChange(of: pick) { _, newValue in
            if let photo = newValue {
                Task {
                    await errorHandler.catchAndShowImmediately {
                        switch try await photo.loadTransferable(type: CopiedTransfer.self)! {
                        case .url(let url):
                            try importImage(from: url)
                        case .binary(_):
                            errorHandler.show(message: String(localized: "Unsupported data type"))
                        case .error(let message):
                            errorHandler.show(message: message)
                        }
                    }
                }
            }
        }
    }
    
    func importImage(from: URL) throws {
        let name = importService.importImage(namedSource: NamedSource(url: from))
        let cgImage = try loader.load(fileName: name)
        Task {
            await cache.put(name: name, image: cgImage)
            frame = ImageFrame(id: frame.id, embeddingsId: frame.embeddingsId, filename: name, width: Int64(cgImage.width), height: Int64(cgImage.height), altText: nil)
        }
    }
}
