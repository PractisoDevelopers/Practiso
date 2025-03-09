import Foundation
import SwiftUI
import ComposeApp

extension Question {
    struct Item : View {
        let frame: Frame
        let namespace: Namespace.ID
        
        var body: some View {
            switch frame {
            case let text as FrameText:
                TextFrameView(frame: text.textFrame)
                    .matchedGeometryEffect(id: frame.utid, in: namespace)
            case let image as FrameImage:
                ImageFrameView(frame: image.imageFrame)
                    .matchedGeometryEffect(id: frame.utid, in: namespace)
            case let options as FrameOptions:
                VStack {
                    OptionsFrameView(frame: options) { item in
                        Checkmark(isOn: item.isKey) {
                            OptionsFrameViewItem(frame: item.frame)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .matchedGeometryEffect(id: item.frame.utid, in: namespace)
                    }
                }
            default:
                UnknownItem(frame: frame)
            }
        }
    }
    
    struct UnknownItem : View {
        let frame: Frame
        
        var body: some View {
            Text("Unknown frame type \(String(describing: frame.self))")
                .foregroundStyle(.secondary)
                .padding()
                .border(.secondary, cornerRadius: 12)
        }
    }
}
