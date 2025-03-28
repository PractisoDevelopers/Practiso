import Foundation
import SwiftUI
@preconcurrency import ComposeApp

extension AnswerView {
    struct Timer : View {
        @Environment(\.takeService) private var service
        @Environment(\.scenePhase) private var scenePhase
        @Environment(ContentView.ErrorHandler.self) private var errorHandler
        
        let takeId: Int64
        @State private var data: DataState = .pending
        @State private var buffer = Buffer()
        
        enum DataState : Equatable {
            case pending
            case empty
            case ok(timers: [Double], duration: Double, start: Date)
        }
        
        var body: some View {
            Group {
                switch data {
                case .pending:
                    EmptyView()
                case .empty:
                    Text("Take is removed")
                case .ok(let timers, let duration, let start):
                    TimerDisplay(timers: timers, duration: duration, start: start)
                        .onDisappear {
                            Task.detached {
                                _ = await updateDbDuration()
                            }
                        }
                }
            }
            .task(id: takeId) {
                for await timers in service.getTimersInSecond() {
                    buffer.timers = timers.map(\.doubleValue)
                    initative()
                }
            }
            .task(id: takeId) {
                if let duration = await service.getTake().makeAsyncIterator().next()?.durationSeconds {
                    buffer.duration = Double(duration)
                    initative()
                } else {
                    data = .empty
                }
            }
            .task(id: takeId) {
                while true {
                    if let _ = try? await Task.sleep(for: .seconds(10)) {
                        _ = await updateDbDuration()
                    } else {
                        break
                    }
                }
            }
            .onChange(of: scenePhase) { oldValue, newValue in
                switch newValue {
                case .background:
                    if case .ok(_, let duration, let start) = data {
                        let duration = Date.now.timeIntervalSince(start) + TimeInterval(integerLiteral: duration)
                        buffer.duration = duration
                    }
                case .active:
                    initative()
                default:
                    break
                }
            }
        }
        
        func initative() {
            let newState = buffer.dataState()
            if case .ok(_, _, _) = newState {
                data = newState
            }
        }
        
        func updateDbDuration() async -> Bool {
            if case .ok(_, let duration, let start) = data {
                let duration = Date.now.timeIntervalSince(start) + TimeInterval(integerLiteral: duration)
                await errorHandler.catchAndShowImmediately {
                    try await service.updateDuration(durationInSeconds: Int64(round(duration)))
                }
                return true
            } else {
                return false
            }
        }
        
        struct TimerDisplay : View {
            @Environment(\.scenePhase) private var scenePhase
            
            let timers: [Double]
            let duration: Double
            let start: Date
            
            @State private var isActive: Bool = true
            @State private var selectedTimer: Int = -1
            
            init(timers: [Double], duration: Double, start: Date) {
                self.timers = timers.sorted()
                self.duration = duration
                self.start = start
            }

            var body: some View {
                Menu {
                    Label {
                        Text(start.addingTimeInterval(-duration), style: .relative)
                            .animation(.none)
                    } icon: {
                        Image(systemName: "timer")
                    }

                    Menu("All Timers", systemImage: "rectangle.stack") {
                        ForEach(Array(timers.enumerated()), id: \.element) { (index, timer) in
                            Toggle(isOn: Binding<Bool>(get: {
                                selectedTimer == index
                            }, set: { newValue, _ in
                                if newValue {
                                    selectedTimer = index
                                } else {
                                    selectedTimer = -1
                                }
                            })) {
                                Text(Duration.seconds(timer), format: .time(pattern: .hourMinuteSecond))
                            }
                        }
                    }
                } label: {
                    HStack {
                        if let next = nextTimer, isActive {
                            if next <= Date.now {
                                Group {
                                    Image(systemName: "alarm")
                                    Text("Time is Up")
                                }
                                .foregroundStyle(.tint)
                            } else {
                                Image(systemName: "alarm")
                                Text(timerInterval: Date.now...next, countsDown: true)
                            }
                        } else {
                            Image(systemName: "timer")
                            Text("Timer")
                        }
                    }
                    .shadow(color: .black, radius: 14)
                    .animation(.default, value: isActive)
                    .onChange(of: scenePhase) { oldValue, newValue in
                        if newValue == .active {
                            isActive = true
                        } else if newValue == .inactive {
                            isActive = false
                        }
                    }
                }
                .buttonStyle(.plain)
            }
            
            var nextTimer: Date? {
                let duration = Date.now.timeIntervalSince(start) + TimeInterval(integerLiteral: duration)
                let timer =
                if selectedTimer < 0 {
                    timers.first(where: { $0 > duration })
                } else {
                    timers[selectedTimer]
                }
                return if let t = timer {
                    Date.now.addingTimeInterval(t - duration)
                } else {
                    nil
                }
            }
        }
        
        struct Buffer {
            var timers: [Double]? = nil
            var duration: Double? = nil
            
            func dataState() -> DataState {
                if let timers = timers, let duration = duration {
                    .ok(timers: timers, duration: duration, start: Date.now)
                } else {
                    .pending
                }
            }
        }
    }
}
