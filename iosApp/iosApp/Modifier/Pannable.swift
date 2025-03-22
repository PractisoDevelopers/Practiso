import Foundation
import SwiftUI

final class PanGesture : NSObject, UIGestureRecognizerRepresentable, UIGestureRecognizerDelegate {
    private var change: PanChange? = nil
    private var end: PanEnd? = nil
    private var source: PanGestureSource = .all
    
    let isEnabled: Bool
    init(isEnabled: Bool = true) {
        self.isEnabled = isEnabled
    }

    func makeUIGestureRecognizer(context: Context) -> UIPanGestureRecognizer {
        let pgr = UIPanGestureRecognizer()
        setTypeMask(recognizer: pgr)
        pgr.delegate = self
        context.coordinator.panStateObservation = pgr.observe(\.state) { gr, change in
            DispatchQueue.main.async {
                switch gr.state {
                case .possible:
                    fallthrough
                case .ended:
                    fallthrough
                case .cancelled:
                    context.coordinator.endingTranslation = .zero
                    if let end = self.end {
                        end()
                    }
                default:
                    break
                }
            }
        }
        
        return pgr
    }
    
    func handleUIGestureRecognizerAction(_ recognizer: UIPanGestureRecognizer, context: Context) {
        if let change = self.change {
            let location = context.converter.localLocation
            let translation = context.converter.localTranslation!
            let end = context.coordinator.endingTranslation
            let relativeTranslation = CGPoint(x: translation.x - end.x, y: translation.y - end.y)
            
            let velocity = context.converter.localVelocity!
            if change(location, relativeTranslation, velocity) {
                context.coordinator.endingTranslation = translation
            }
        }
    }
    
    func updateUIGestureRecognizer(_ recognizer: UIPanGestureRecognizer, context: Context) {
        recognizer.delegate = self
        recognizer.isEnabled = isEnabled
        setTypeMask(recognizer: recognizer)
    }
    
    func makeCoordinator(converter: CoordinateSpaceConverter) -> Coordinator {
        Coordinator()
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        return source.contains(.mouse) && source.contains(.trackpad) && touch.type == .indirect
        || source.contains(.touch) && touch.type == .direct
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }
    
    private func setTypeMask(recognizer: UIPanGestureRecognizer) {
        var mask = UIScrollTypeMask()
        if source.contains(.mouse) {
            mask.insert(.discrete)
        }
        if source.contains(.trackpad) {
            mask.insert(.continuous)
        }
        recognizer.allowedScrollTypesMask = mask
    }

    func onChange(_ change: @escaping PanChange) -> Self {
        self.change = change
        return self
    }
    
    func onEnd(_ end: @escaping PanEnd) -> Self {
        self.end = end
        return self
    }
    
    func source(_ value: PanGestureSource) -> Self {
        self.source = value
        return self
    }
    
    class Coordinator {
        var endingTranslation: CGPoint = .zero
        var panStateObservation: NSKeyValueObservation? = nil
    }
}

typealias PanChange = (_ location: CGPoint, _ translation: CGPoint, _ velocity: CGPoint) -> Bool
typealias PanEnd = () -> Void

struct PanGestureSource : OptionSet {
    let rawValue: Int
    static let mouse = PanGestureSource(rawValue: 1 << 0)
    static let trackpad = PanGestureSource(rawValue: 1 << 1)
    static let touch = PanGestureSource(rawValue: 1 << 2)
    
    static let all: PanGestureSource = [.mouse, .touch, .trackpad]
}
