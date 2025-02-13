import Foundation
import SwiftUI
import ComposeApp

extension Question {
    struct Item : View {
        let frame: Frame
        
        var body: some View {
            switch frame {
            case let text as FrameText:
                TextFrameView(frame: text.textFrame)
            case let image as FrameImage:
                ImageFrameView(frame: image.imageFrame)
            case let options as FrameOptions:
                VStack {
                    OptionsFrameView(frame: options) { item in
                        Checkmark(isOn: item.isKey) {
                            OptionsFrameViewItem(frame: item.frame)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
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
