import Foundation
import SwiftUI

extension SessionCreatorView {
    @Observable
    class Model {
        var sessionParams: SessionParameters = SessionParameters(name: "")
        var takeParams: TimerParameters? = nil
        var selectedSuggestion: (any Option)? = nil
        
        var isEmpty: Bool {
            return sessionParams.name.isEmpty && sessionParams.selection.isEmpty && takeParams == nil && selectedSuggestion == nil
        }
    }
}
