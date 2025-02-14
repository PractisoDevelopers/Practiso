import SwiftUI

struct ZoomableScrollView<Content: View>: UIViewRepresentable {
    
    private var content: Content
    @Binding private var scale: CGFloat
    let maxScale: CGFloat

    init(scale: Binding<CGFloat>, maxScale: CGFloat, @ViewBuilder content: () -> Content) {
        self._scale = scale
        self.maxScale = maxScale
        self.content = content()
    }

    func makeUIView(context: Context) -> UIScrollView {
        // set up the UIScrollView
        let scrollView = UIScrollView()
        scrollView.delegate = context.coordinator  // for viewForZooming(in:)
        scrollView.maximumZoomScale = maxScale
        scrollView.minimumZoomScale = 1
        scrollView.showsVerticalScrollIndicator = false
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.bouncesZoom = true

//      Create a UIHostingController to hold our SwiftUI content
        let hostedView = context.coordinator.hostingController.view!
        hostedView.translatesAutoresizingMaskIntoConstraints = true
        hostedView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        hostedView.frame = scrollView.bounds
        scrollView.addSubview(hostedView)

        return scrollView
    }

    func makeCoordinator() -> Coordinator {
        return Coordinator(hostingController: UIHostingController(rootView: self.content), scale: $scale)
    }

    func updateUIView(_ uiView: UIScrollView, context: Context) {
        // update the hosting controller's SwiftUI content
        context.coordinator.hostingController.rootView = self.content
        uiView.zoomScale = scale
        assert(context.coordinator.hostingController.view.superview == uiView)
    }
    
    class Coordinator: NSObject, UIScrollViewDelegate {

        var hostingController: UIHostingController<Content>
        @Binding var scale: CGFloat

        init(hostingController: UIHostingController<Content>, scale: Binding<CGFloat>) {
            self.hostingController = hostingController
            self._scale = scale
        }

        func viewForZooming(in scrollView: UIScrollView) -> UIView? {
            return hostingController.view
        }

        func scrollViewDidEndZooming(_ scrollView: UIScrollView, with view: UIView?, atScale scale: CGFloat) {
            self.scale = scale
        }
    }
}
