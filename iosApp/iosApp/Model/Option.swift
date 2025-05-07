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
        switch onEnum(of: sessionCreator) {
        case .dimensionOption(let kt):
            OptionImpl(kt: kt)
        case .recentlyCreatedDimension(let kt):
            OptionImpl(kt: kt)
        case .recentlyCreatedQuizzes(let kt):
            OptionImpl(kt: kt)
        case .failMuch(let kt):
            OptionImpl(kt: kt)
        case .leastAccessed(let kt):
            OptionImpl(kt: kt)
        case .failMuchDimension(let kt):
            OptionImpl(kt: kt)
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

extension SessionCreatorRecentlyCreatedQuizzes : PractisoOptionViewable {
    var view: PractisoOptionView {
        let name = self.leadingQuizName ?? String(localized: "New question")
        return PractisoOptionView(
            header: String(localized: "Recently created"),
            subtitle: selection.quizIds.count > 1 ? String(localized: "\(name) and \(selection.quizIds.count - 1) more") : name
        )
    }
}

extension SessionCreatorRecentlyCreatedDimension : PractisoOptionViewable {
    var view: PractisoOptionView {
        PractisoOptionView(
            header: String(localized: "Recently created"),
            subtitle: String(localized: "\(quizCount) questions in \(dimensionName)")
        )
    }
}

extension SessionCreatorFailMuch : PractisoOptionViewable {
    var view: PractisoOptionView {
        let subtitle = if let itemName = self.leadingItemName {
            if itemCount > 1 {
                String(localized: "\(itemName) and \(itemCount - 1) more")
            } else {
                itemName
            }
        } else {
            String(localized: "\(itemCount) items")
        }
        return PractisoOptionView(
            header: String(localized: "Have a review"),
            subtitle: subtitle
        )
    }
}

extension SessionCreatorLeastAccessed : PractisoOptionViewable {
    var view: PractisoOptionView {
        let subtitle = if let itemName = self.leadingItemName {
            if itemCount > 1 {
                String(localized: "\(itemName) and \(itemCount - 1) more")
            } else {
                itemName
            }
        } else {
            String(localized: "\(itemCount) items")
        }
        return PractisoOptionView(
            header: String(localized: "Have a review"),
            subtitle: subtitle
        )
    }
}

extension SessionCreatorFailMuchDimension : PractisoOptionViewable {
    var view: PractisoOptionView {
        return PractisoOptionView(header: String(localized: "\(itemCount) items in \(dimension.name)"))
    }
}
