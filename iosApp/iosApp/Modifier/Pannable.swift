import Foundation
import SwiftUI

struct Pannable : ViewModifier {
    let gesture: PanGesture
    
    func body(content: Content) -> some View {
        PannableView(content: { content }, gesture: gesture)
    }
}

extension View {
    func pannable(_ gesture: PanGesture) -> some View {
        modifier(Pannable(gesture: gesture))
    }
}

class PanGesture {
    var change: PanChange? = nil
    var end: PanEnd? = nil
    
    func onChange(change: @escaping PanChange) -> Self {
        self.change = change
        return self
    }
    
    func onEnd(end: @escaping PanEnd) -> Self {
        self.end = end
        return self
    }
}

typealias PanChange = (_ location: CGPoint, _ translation: CGPoint, _ velocity: CGPoint) -> Bool
typealias PanEnd = () -> Void

struct PannableView<Content : View> : UIViewRepresentable {
    @ViewBuilder let content: Content
    let gesture: PanGesture
    
    func makeUIView(context: Context) -> some UIView {
        let coordinator = context.coordinator
        let hostedView = coordinator.hostingController.view!
        let panGestureRecognizer = coordinator.withGestureRecognizer()
        let container = UIView()
        
        hostedView.addGestureRecognizer(panGestureRecognizer)
        hostedView.translatesAutoresizingMaskIntoConstraints = true
        hostedView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        
        container.addSubview(hostedView)
        return container
    }
    
    func updateUIView(_ uiView: UIViewType, context: Context) {
        context.coordinator.hostingController.rootView = self.content
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(hostingController: UIHostingController(rootView: self.content), gesture: gesture)
    }
    
    static func dismantleUIView(_ uiView: UIViewType, coordinator: Coordinator) {
        coordinator.cleanup()
    }
    
    @MainActor
    class Coordinator : NSObject, UIGestureRecognizerDelegate {
        let hostingController: UIHostingController<Content>
        private let gesture: PanGesture
        
        private var endingTranslation: CGPoint = .zero
        private var panStateObservation: NSKeyValueObservation? = nil
        
        init(hostingController: UIHostingController<Content>, gesture: PanGesture) {
            self.hostingController = hostingController
            self.gesture = gesture
        }
        
        func withGestureRecognizer() -> UIPanGestureRecognizer {
            assert(panStateObservation == nil, "withGestureRecoginzer called twice")
            
            let pgr = UIPanGestureRecognizer(target: self, action: #selector(pan(_:)))
            pgr.allowedScrollTypesMask = [.continuous]
            pgr.delegate = self
            panStateObservation = pgr.observe(\.state) { gr, change in
                DispatchQueue.main.async {
                    switch gr.state {
                    case .possible:
                        fallthrough
                    case .ended:
                        fallthrough
                    case .cancelled:
                        self.endingTranslation = .zero
                        if let end = self.gesture.end {
                            end()
                        }
                    default:
                        break
                    }
                }
            }
            
            return pgr
        }
        
        func cleanup() {
            panStateObservation?.invalidate()
        }
        
        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
            return !(gestureRecognizer is UIPanGestureRecognizer)
        }
        
        @objc
        func pan(_ gr: UIPanGestureRecognizer) {
            if let change = gesture.change {
                let location = gr.location(in: hostingController.view)
                let translation = gr.translation(in: hostingController.view)
                let relativeTranslation = CGPoint(x: translation.x - endingTranslation.x, y: translation.y - endingTranslation.y)
                
                let velocity = gr.velocity(in: hostingController.view)
                if change(location, relativeTranslation, velocity) {
                    endingTranslation = translation
                }
            }
        }
    }
}

