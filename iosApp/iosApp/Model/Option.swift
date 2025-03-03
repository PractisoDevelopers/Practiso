import ComposeApp
import Foundation

protocol Option : Identifiable, Hashable {
    associatedtype KtType where KtType : PractisoOption, KtType : PractisoOptionViewable
    var kt: KtType { get }
    var id: Int64 { get }
    var view: PractisoOptionView { get }
}

class OptionImpl<KtType> : Option where KtType : PractisoOption & PractisoOptionViewable & Hashable {
    static func == (lhs: OptionImpl<KtType>, rhs: OptionImpl<KtType>) -> Bool {
        lhs.kt == rhs.kt
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(kt)
    }
    
    let kt: KtType
    let id: Int64
    
    var view: PractisoOptionView {
        kt.view
    }
    
    init(kt: KtType) {
        self.kt = kt
        self.id = kt.id
    }
}

struct SessionCreatorOption {
    static func from(sessionCreator: ComposeApp.SessionCreator) -> any Option {
        if let kt = sessionCreator as? SessionCreatorViaSelection {
            return OptionImpl<SessionCreatorViaSelection>(kt: kt)
        } else {
            fatalError()
        }
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

extension SessionOption : PractisoOptionViewable {
    var view: PractisoOptionView {
        let timeFormatter = RelativeDateTimeFormatter()
        let relativeTime = timeFormatter.localizedString(for: Date(kt: session.creationTimeISO), relativeTo: Date())
        return PractisoOptionView(
            header: session.name,
            title: String(localized: "Created \(relativeTime)"),
            subtitle: String(localized: "\(quizCount) questions")
        )
    }
}

extension SessionCreatorViaSelection : PractisoOptionViewable {
    var view: PractisoOptionView {
        let header = switch self.type {
            case .recentlyCreated:
                String(localized: "Recently created")
            case .failMuch:
                String(localized: "Recommended for you")
            case .leastAccessed:
                String(localized: "Least accessed")
            }
        return PractisoOptionView(
            header: header,
            subtitle: preview
        )
    }
}
