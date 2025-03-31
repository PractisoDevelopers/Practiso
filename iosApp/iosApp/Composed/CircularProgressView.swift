import Foundation
import SwiftUI

/// I have to create this because Apple's implementation (ProgressView) is shit

struct CircularProgressView<Value : BinaryFloatingPoint> : View {
    let value: Value
    
    var body: some View {
        Canvas { context, size in
            context.translateBy(x: size.width * 0.5, y: size.height * 0.5)
            
            let radius = min(size.width, size.height) * 0.5
            
            let donut = Path { p in
                p.addArc(center: .zero, radius: radius, startAngle: .zero, endAngle: .init(degrees: 360), clockwise: false)
                p.addArc(center: .zero, radius: radius * 0.618, startAngle: .zero, endAngle: .init(degrees: 360), clockwise: false)
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

#Preview {
    VStack {
        ForEach(0..<10) { i in
            CircularProgressView(value: 0.1 * Double(i))
        }
    }
}
