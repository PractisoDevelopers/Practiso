import Foundation
import SwiftUI

typealias PanChange = (_ location: CGPoint, _ translation: CGPoint, _ velocity: CGPoint) -> Bool

struct PannableView<Content : View> : UIViewRepresentable {
    @ViewBuilder let content: Content
    let onPan: PanChange
    
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
        Coordinator(hostingController: UIHostingController(rootView: self.content), onPan: onPan)
    }
    
    @MainActor
    class Coordinator : NSObject {
        let hostingController: UIHostingController<Content>
        private let onPan: PanChange
        
        private var endingTranslation: CGPoint = .zero
        private var panStateObservation: NSKeyValueObservation? = nil
        
        init(hostingController: UIHostingController<Content>, onPan: @escaping PanChange) {
            self.hostingController = hostingController
            self.onPan = onPan
        }
        
        func withGestureRecognizer() -> UIPanGestureRecognizer {
            assert(panStateObservation == nil, "withGestureRecoginzer called twice")
            
            let pgr = UIPanGestureRecognizer(target: self, action: #selector(pan(_:)))
            pgr.allowedScrollTypesMask = [.continuous]
            panStateObservation = pgr.observe(\.state) { gr, change in
                DispatchQueue.main.async {
                    switch gr.state {
                    case .possible:
                        fallthrough
                    case .ended:
                        fallthrough
                    case .cancelled:
                        self.endingTranslation = .zero
                    default:
                        break
                    }
                }
            }
            
            return pgr
        }
        
        @objc
        func pan(_ gr: UIPanGestureRecognizer) {
            let location = gr.location(in: hostingController.view)
            let translation = gr.translation(in: hostingController.view)
            let relativeTranslation = CGPoint(x: translation.x - endingTranslation.x, y: translation.y - endingTranslation.y)
            
            let velocity = gr.velocity(in: hostingController.view)
            if self.onPan(location, relativeTranslation, velocity) {
                endingTranslation = translation
            }
        }
    }
}

