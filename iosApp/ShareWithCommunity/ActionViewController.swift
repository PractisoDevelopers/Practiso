@preconcurrency import ComposeApp
import MobileCoreServices
import Shared
import SwiftUI
import UIKit
import UniformTypeIdentifiers

class ActionViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()

        let archive = ArchiveResourceInfo()
        
        let swiftUIView = ContentView()
            .environment(\.archiveResource, archive)
            .environment(\.finishTask, {
                self.extensionContext!.completeRequest(returningItems: nil, completionHandler: nil)
            })
        let hostingController = UIHostingController(rootView: swiftUIView)
        addChild(hostingController)
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(hostingController.view)
        // Set constraints for the SwiftUI view
        NSLayoutConstraint.activate([
            hostingController.view.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            hostingController.view.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            hostingController.view.widthAnchor.constraint(equalTo: view.widthAnchor),
            hostingController.view.heightAnchor.constraint(equalTo: view.heightAnchor),
        ])
        hostingController.didMove(toParent: self)

        // Get the item we're handling from the extension context.
        var archiveFound = false
        for item in extensionContext!.inputItems as! [NSExtensionItem] {
            for provider in item.attachments! {
                if !provider.hasItemConformingToTypeIdentifier(UTType.psarchive.identifier) {
                    continue
                }

                provider.loadItem(forTypeIdentifier: UTType.psarchive.identifier) { res, error in
                    if let archiveURL = res as? URL {
                        archive.resource = .success(.url(archiveURL))
                    } else if let data = res as? Data {
                        archive.resource = .success(.data(data, fileName: provider.suggestedName))
                    } else {
                        archive.resource = .failure(.init(error ?? CocoaError(.fileReadUnsupportedScheme)))
                    }
                }

                archiveFound = true
                break
            }

            if archiveFound {
                // We only handle one archive, so stop looking for more.
                break
            }
        }
    }
}
