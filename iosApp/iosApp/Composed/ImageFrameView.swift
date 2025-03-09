import Foundation
import SwiftUI
import ComposeApp

struct ImageFrameView : View {
    enum DataState {
        case pending
        case ok(image: CGImage)
        case resourceUnavailable(fileName: String)
        case invalidImage(fileName: String)
    }
    
    @State private var __data: DataState = .pending
    @State private var isFullscreen = false
    @State private var fullscreenScale: CGFloat = 1
    @State private var isAltTextShown = false

    let frame: ImageFrame
    let data: Binding<DataState>?
    @Environment(\.imageService) var loader
    @Environment(\.imageCache) var cache

    init(frame: ImageFrame, data: Binding<DataState?>? = nil) {
        self.frame = frame
        if let dataBinding = data {
            self.data = Binding(get: {
                dataBinding.wrappedValue ?? .pending
            }, set: { newValue in
                dataBinding.wrappedValue = newValue
            })
        } else {
            self.data = nil
        }
    }

    var body: some View {
        Group {
            switch data?.wrappedValue ?? __data {
            case .ok(let image):
                Image(image, scale: 1, orientation: .up, label: Text(frame.altText ?? ""))
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(idealHeight: 150)
                    .onTapGesture {
                        fullscreenScale = 1
                        isFullscreen = true
                    }
                
            case .resourceUnavailable:
                VStack {
                    Image(systemName: "photo.badge.exclamationmark")
                    Text("Resource Unavailable")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .border(.secondary, cornerRadius: 12)

            case .invalidImage:
                VStack {
                    Image(systemName: "photo.badge.exclamationmark")
                    Text("Resource is Corrupted")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .border(.secondary, cornerRadius: 12)
                
            default:
                VStack {
                    ProgressView()
                    Text("Loading Image...")
                }
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, idealHeight: 150)
                .padding()
            }
        }
        .task(id: frame) {
            if let cached = await cache.get(name: frame.filename) {
                __data = .ok(image: cached)
                data?.wrappedValue = __data
            } else {
                do {
                    let image = try loader.load(fileName: frame.filename)
                    await cache.put(name: frame.filename, image: image)
                    __data = .ok(image: image)
                    data?.wrappedValue = __data
                } catch ImageServiceError.invalidData {
                    __data = .invalidImage(fileName: frame.filename)
                    data?.wrappedValue = __data
                } catch ImageServiceError.sourceUnavailable {
                    __data = .resourceUnavailable(fileName: frame.filename)
                    data?.wrappedValue = __data
                } catch {
                    assertionFailure("\(error) is not possible")
                }
            }
        }
        .fullScreenCover(isPresented: $isFullscreen) {
            switch data?.wrappedValue ?? __data {
            case .ok(let image):
                ZoomableScrollView(scale: $fullscreenScale, maxScale: 10) {
                    Image(image, scale: 1, label: Text(frame.altText ?? ""))
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                .overlay(alignment: .topTrailing) {
                    Button("Close") {
                        isFullscreen = false
                    }
                    .padding(8)
                    .hoverEffect()
                    .padding(12)
                }
                .onTapGesture {
                    if frame.altText?.isEmpty == false {
                        isAltTextShown = true
                    }
                }
            default:
                Text("Resource Unavailable")
            }
        }
        .alert("Alternative Text", isPresented: $isAltTextShown) {
            Button("OK") {
                isAltTextShown = false
                isFullscreen = true
            }
        } message: {
            if let altText = frame.altText {
                Text(altText)
            }
        }
    }
}
