import Foundation
import ComposeApp

struct SessionParameters {
    let name: String
    let selection: Selection
    
    struct Selection {
        let quizIds: Set<Int64>
        let dimensionIds: Set<Int64>
        
        init(quizIds: Set<Int64> = Set(), dimensionIds: Set<Int64> = Set()) {
            self.quizIds = quizIds
            self.dimensionIds = dimensionIds
        }
        
        var isEmpty: Bool {
            get {
                dimensionIds.isEmpty && quizIds.isEmpty
            }
        }
    }
    
    init(name: String, selection: Selection = Selection()) {
        self.name = name
        self.selection = selection
    }
    
    init(from: ComposeApp.SessionCreator) {
        self.name = SessionCreatorOption.from(sessionCreator: from).view.subtitle ?? ""
        self.selection = Selection(
            quizIds: Set(from.selection.quizIds.map(\.int64Value)),
            dimensionIds: Set(from.selection.dimensionIds.map(\.int64Value))
        )
    }
}

protocol WithTimerParameters {
    var timers: [Timer] { get }
}

struct Timer : Identifiable {
    let id = UUID()
    var timeout: Duration
}

struct TimerParameters : WithTimerParameters {
    let timers: [Timer]
    
    init(timers: [Timer] = []) {
        self.timers = timers
    }
}

struct TakeParameters : WithTimerParameters {
    let timers: [Timer]
    let sessionId: Int64
    
    init(sessionId: Int64, timers: [Timer] = []) {
        self.sessionId = sessionId
        self.timers = timers
    }
}

struct SessionCreator {
    let params: SessionParameters
    let service = CreateService(db: Database.shared.app)
    
    func create() async throws -> Int64 {
        try await service.createSession(
            name: params.name.trimmingCharacters(in: .whitespacesAndNewlines),
            selection: .init(quizIds: .init(params.selection.quizIds.map(KotlinLong.init)), dimensionIds: .init(params.selection.dimensionIds.map(KotlinLong.init)))
        ).int64Value
    }
}

struct SessionTakeCreator {
    let session: SessionParameters
    let take: WithTimerParameters
    let service = CreateService(db: Database.shared.app)
    
    func create() async throws -> (sessionId: Int64, takeId: Int64) {
        let sessionId = try await SessionCreator(params: session).create()
        let takeId = try await TakeCreator(take: TakeParameters(sessionId: sessionId, timers: take.timers)).create()
        return (sessionId, takeId)
    }
}

struct TakeCreator {
    let take: TakeParameters
    let service = CreateService(db: Database.shared.app)
    
    func create() async throws -> (Int64) {
        let takeId = try await service.createTake(sessionId: take.sessionId, timers: take.timers.map { DurationKt(seconds: $0.timeout.components.seconds, attoseconds: $0.timeout.components.attoseconds) })
        return takeId.int64Value
    }
}
