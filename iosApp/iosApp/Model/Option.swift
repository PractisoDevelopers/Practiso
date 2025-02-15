import ComposeApp
import Foundation

protocol Option : Identifiable, Hashable {
    associatedtype KtType where KtType : PractisoOption, KtType : PractisoOptionViewable
    var kt: KtType { get }
    var view: PractisoOptionView { get }
}

class OptionImpl<KtType> : Option where KtType : PractisoOption, KtType : PractisoOptionViewable {
    static func == (lhs: OptionImpl<KtType>, rhs: OptionImpl<KtType>) -> Bool {
        lhs.kt.id == rhs.kt.id
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(kt.id)
        hasher.combine(view)
    }
    
    let kt: KtType
    
    var id: some Option {
        self
    }
    
    var view: PractisoOptionView {
        kt.view
    }
    
    init(kt: KtType) {
        self.kt = kt
    }
}

struct PractisoOptionView : Equatable, Hashable {
    var header: String
    var title: String?
    var subtitle: String?
}

protocol PractisoOptionViewable {
    var view: PractisoOptionView { get }
}

protocol ModificationComparable {
    var modificationCompare: Date { get }
}

protocol CreationComparable {
    var creationCompare: Date { get }
}

protocol NameComparable {
    var nameCompare: String { get }
}

extension DimensionOption: PractisoOptionViewable, NameComparable, CreationComparable {
    var nameCompare: String {
        dimension.name
    }
    
    var creationCompare: Date {
        Date(timeIntervalSince1970: TimeInterval(integerLiteral: dimension.id))
    }
    
    var view: PractisoOptionView {
        PractisoOptionView(header: dimension.name,
                           subtitle: { if quizCount > 0 { String(localized: "\(quizCount) questions") } else { String(localized: "Empty") } }())
    }
}

extension QuizOption: PractisoOptionViewable, NameComparable, CreationComparable, ModificationComparable {
    var nameCompare: String {
        quiz.name ?? ""
    }
    
    var creationCompare: Date {
        Date(kt: quiz.creationTimeISO)
    }
    
    var modificationCompare: Date {
        Date(kt: quiz.modificationTimeISO ?? quiz.creationTimeISO)
    }
    
    var view: PractisoOptionView {
        PractisoOptionView(header: { if quiz.name?.isEmpty == false { quiz.name! } else { String(localized: "New question") } }(),
                           subtitle: preview ?? String(localized: "Empty"))
    }
}

extension TemplateOption: PractisoOptionViewable, NameComparable, CreationComparable, ModificationComparable {
    var nameCompare: String {
        template.name
    }
    
    var creationCompare: Date {
        Date(kt: template.creationTimeISO)
    }
    
    var modificationCompare: Date {
        Date(kt: template.modificationTimeISO ?? template.creationTimeISO)
    }
    
    var view: PractisoOptionView {
        PractisoOptionView(header: template.name, subtitle: Date(kt: template.creationTimeISO).formatted())
    }
}

