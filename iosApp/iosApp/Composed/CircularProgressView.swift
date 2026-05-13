import Foundation
import SwiftUI

/// I have to create this because Apple's implementation (ProgressView) is shit

struct CircularProgressView<Value: BinaryFloatingPoint>: View {
    let value: Value
    
    @Environment(CircularProgressViewStyle.self) private var style: CircularProgressViewStyle?
    
    var body: some View {
        Canvas { context, size in
            context.translateBy(x: size.width * 0.5, y: size.height * 0.5)
            
            let radius = min(size.width, size.height) * 0.5
            let innerRedius = switch style?.circle {
            case .none:
                fallthrough
            case .regular:
                radius * 0.618
            case .thin:
                radius * 0.7416
            }
            
            let donut = Path { p in
                p.addArc(center: .zero, radius: radius, startAngle: .zero, endAngle: .init(degrees: 360), clockwise: false)
                p.addArc(center: .zero, radius: innerRedius, startAngle: .zero, endAngle: .init(degrees: 360), clockwise: false)
            }
            context.clip(to: donut, style: .init(eoFill: true))
            
            let arcAngle = Angle(degrees: 360 * Double(value) - 90)
            let arc = Path { p in
                p.move(to: .zero)
                p.addArc(center: .zero, radius: radius, startAngle: .degrees(-90), endAngle: arcAngle, clockwise: false)
                p.closeSubpath()
            }
            context.fill(arc, with: .style(.tint))
            let closer = Path { p in
                p.move(to: .zero)
                p.addArc(center: .zero, radius: radius, startAngle: arcAngle, endAngle: .degrees(270), clockwise: false)
                p.closeSubpath()
            }
            context.fill(closer, with: .style(.foreground))
        }
        .frame(width: 24, height: 24)
    }
    
}

enum CircularProgressViewCircleStyle: Equatable {
    case thin
    case regular
}

@Observable
fileprivate class CircularProgressViewStyle {
    var circle: CircularProgressViewCircleStyle
    
    init() {
        self.circle = .regular
    }
    
    init(circle: CircularProgressViewCircleStyle) {
        self.circle = circle
    }
}

extension View {
    func circularProgressViewStyle(circle: CircularProgressViewCircleStyle) -> some View {
        self.environment(CircularProgressViewStyle(circle: circle))
    }
}

struct IndeterministicCircularProgressView: View {
    @State private var animating = false
    var body: some View {
        ZStack {
            CircularProgressView(value: 0.86)
                .rotationEffect(.degrees(animating ? 360 : 0))
                .animation(.linear(duration: 2).repeatForever(autoreverses: false), value: animating)
                .onAppear {
                    animating = true
                }
        }
    }
}

struct WithStopIcon<Content: View>: View {
    @ViewBuilder
    let content: Content
    
    var body: some View {
        ZStack {
            Image(systemName: "stop.fill")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 8)
            content
                .circularProgressViewStyle(circle: .thin)
        }
    }
}

#Preview {
    VStack {
        ForEach(0 ..< 10) { i in
            CircularProgressView(value: 0.1 * Double(i))
        }
        IndeterministicCircularProgressView()
        WithStopIcon {
            CircularProgressView(value: 0.5)
        }
        WithStopIcon {
            IndeterministicCircularProgressView()
        }
    }
}
