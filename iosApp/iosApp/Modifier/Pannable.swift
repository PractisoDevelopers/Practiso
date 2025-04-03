import Foundation
import SwiftUI

struct PanGesture : UIGestureRecognizerRepresentable {
    private let change: PanChange
    private let end: PanEnd?
    private let source: PanGestureSource
    @Binding private var isEnabled: Bool
    
    init(isEnabled: Binding<Bool> = .constant(true), source: PanGestureSource = .all, change: @escaping PanChange, end: PanEnd? = nil) {
        self._isEnabled = isEnabled
        self.source = source
        self.change = change
        self.end = end
    }
    
    func makeUIGestureRecognizer(context: Context) -> UIPanGestureRecognizer {
        let pgr = UIPanGestureRecognizer()
        setTypeMask(recognizer: pgr)
        pgr.delegate = context.coordinator.gestureDelegate
        pgr.isEnabled = isEnabled
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
    
    func makeCoordinator(converter: CoordinateSpaceConverter) -> Coordinator {
        Coordinator()
    }

    func handleUIGestureRecognizerAction(_ recognizer: UIPanGestureRecognizer, context: Context) {
        if !isEnabled {
            return
        }
        
        let location = context.converter.localLocation
        let translation = context.converter.localTranslation!
        let end = context.coordinator.endingTranslation
        let relativeTranslation = CGPoint(x: translation.x - end.x, y: translation.y - end.y)
        
        let velocity = context.converter.localVelocity!
        if change(location, relativeTranslation, velocity) {
            context.coordinator.endingTranslation = translation
        }
    }
    
    func updateUIGestureRecognizer(_ recognizer: UIPanGestureRecognizer, context: Context) {
        context.coordinator.gestureDelegate.source = source
        recognizer.isEnabled = isEnabled
        setTypeMask(recognizer: recognizer)
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

    @MainActor
    class Coordinator {
        var endingTranslation: CGPoint = .zero
        var panStateObservation: NSKeyValueObservation? = nil
        var gestureDelegate: GestureDelegate = .init(source: .all)
    }
    
    final class GestureDelegate : NSObject, UIGestureRecognizerDelegate {
        var source: PanGestureSource
        
        init(source: PanGestureSource) {
            self.source = source
        }
        
        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
            return source.contains(.mouse) && source.contains(.trackpad) && touch.type == .indirect
            || source.contains(.touch) && touch.type == .direct
        }
        
        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
            return true
        }
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
