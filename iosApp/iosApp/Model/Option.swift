import ComposeApp
import Foundation

class Option: Identifiable {
    var id: Int64
    let kt: PractisoOption
    lazy var view: PractisoOptionView = (kt as! PractisoOptionViewable).view
    
    init(kt: PractisoOption) {
        self.id = kt.id
        self.kt = kt
    }
}

struct PractisoOptionView {
    var header: String
    var title: String?
    var subtitle: String?
}

protocol PractisoOptionViewable {
    var view: PractisoOptionView { get }
}

extension DimensionOption: PractisoOptionViewable {
    var view: PractisoOptionView {
        PractisoOptionView(header: dimension.name,
                           subtitle: { if quizCount > 0 { String(localized: "\(quizCount) questions") } else { String(localized: "Empty") } }())
    }
}

extension QuizOption: PractisoOptionViewable {
    var view: PractisoOptionView {
        PractisoOptionView(header: { if quiz.name?.isEmpty == false { quiz.name! } else { String(localized: "New question") } }(),
                           subtitle: preview ?? String(localized: "Empty"))
    }
}

extension Template: PractisoOptionViewable {
    var view: PractisoOptionView {
        PractisoOptionView(header: name, subtitle: Date(kt: creationTimeISO).formatted())
    }
}
