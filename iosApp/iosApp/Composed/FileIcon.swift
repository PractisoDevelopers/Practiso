import Foundation
import SwiftUI

struct FileIcon : View {
    var body: some View {
        Image("Document")
            .resizable()
            .shadow(radius: 1)
            .aspectRatio(contentMode: .fit)
            .frame(width: 54, height: 54)
            .contentShape(.contextMenuPreview, path(in: .init(origin: .zero, size: .init(width: 54, height: 54))))
    }
    
    func path(in rect: CGRect) -> Path {
            var path = Path()
            let width = rect.size.width
            let height = rect.size.height
            path.move(to: CGPoint(x: 0.47697*width, y: 0.01389*height))
            path.addCurve(to: CGPoint(x: 0.58192*width, y: 0.06469*height), control1: CGPoint(x: 0.4858*width, y: 0.01389*height), control2: CGPoint(x: 0.5321*width, y: 0.01486*height))
            path.addCurve(to: CGPoint(x: 0.6495*width, y: 0.12824*height), control1: CGPoint(x: 0.59975*width, y: 0.08165*height), control2: CGPoint(x: 0.62382*width, y: 0.10419*height))
            path.addCurve(to: CGPoint(x: 0.78184*width, y: 0.25399*height), control1: CGPoint(x: 0.69842*width, y: 0.17405*height), control2: CGPoint(x: 0.7532*width, y: 0.22535*height))
            path.addCurve(to: CGPoint(x: 0.85627*width, y: 0.38972*height), control1: CGPoint(x: 0.85564*width, y: 0.32779*height), control2: CGPoint(x: 0.85602*width, y: 0.36549*height))
            path.addCurve(to: CGPoint(x: 0.85643*width, y: 0.39695*height), control1: CGPoint(x: 0.85629*width, y: 0.39227*height), control2: CGPoint(x: 0.85632*width, y: 0.39467*height))
            path.addCurve(to: CGPoint(x: 0.85722*width, y: 0.9351*height), control1: CGPoint(x: 0.85689*width, y: 0.46507*height), control2: CGPoint(x: 0.85716*width, y: 0.84001*height))
            path.addCurve(to: CGPoint(x: 0.84942*width, y: 0.94244*height), control1: CGPoint(x: 0.85698*width, y: 0.93919*height), control2: CGPoint(x: 0.85358*width, y: 0.94244*height))
            path.addLine(to: CGPoint(x: 0.15556*width, y: 0.94244*height))
            path.addCurve(to: CGPoint(x: 0.14775*width, y: 0.93463*height), control1: CGPoint(x: 0.15125*width, y: 0.94244*height), control2: CGPoint(x: 0.14775*width, y: 0.93894*height))
            path.addLine(to: CGPoint(x: 0.14775*width, y: 0.0217*height))
            path.addCurve(to: CGPoint(x: 0.15556*width, y: 0.01389*height), control1: CGPoint(x: 0.14775*width, y: 0.01739*height), control2: CGPoint(x: 0.15125*width, y: 0.01389*height))
            path.addLine(to: CGPoint(x: 0.47239*width, y: 0.01389*height))
            path.addCurve(to: CGPoint(x: 0.47608*width, y: 0.01389*height), control1: CGPoint(x: 0.47363*width, y: 0.01389*height), control2: CGPoint(x: 0.47486*width, y: 0.01389*height))
            path.addCurve(to: CGPoint(x: 0.47643*width, y: 0.01389*height), control1: CGPoint(x: 0.47618*width, y: 0.01389*height), control2: CGPoint(x: 0.4763*width, y: 0.01389*height))
            path.addCurve(to: CGPoint(x: 0.47696*width, y: 0.01389*height), control1: CGPoint(x: 0.47658*width, y: 0.01389*height), control2: CGPoint(x: 0.47676*width, y: 0.01389*height))
            path.addCurve(to: CGPoint(x: 0.47697*width, y: 0.01389*height), control1: CGPoint(x: 0.47696*width, y: 0.01389*height), control2: CGPoint(x: 0.47696*width, y: 0.01389*height))
            path.closeSubpath()
            return path
        }
}
