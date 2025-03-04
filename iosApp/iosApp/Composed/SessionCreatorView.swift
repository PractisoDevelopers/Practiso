import Foundation
import SwiftUI
import ComposeApp

struct SessionCreatorView : View {
    private enum Page {
        case specification
        case completion
    }
    
    enum CompletionTask {
        case startTake
        case reveal
    }
    
    @State private var navigationPath: [Page] = []
    @State private var sessionParams = SessionParameters(name: "")
    @State private var takeParams: TimerParameters?
    
    let onCreate: (SessionParameters, TimerParameters?, CompletionTask) -> Void
    let onCancel: () -> Void
    
    var body: some View {
        NavigationStack(path: $navigationPath) {
            WelcomeView(sessionParams: $sessionParams)
                .navigationTitle("Welcome")
                .navigationDestination(for: Page.self) { page in
                    switch page {
                    case .specification:
                        SpecificationView(sessionParams: $sessionParams, takeParams: $takeParams)
                            .navigationTitle("Specification")
                            .toolbar {
                                Button("Next") {
                                    navigationPath.append(.completion)
                                }
                                .disabled(sessionParams.name.trimmingCharacters(in: .whitespaces).isEmpty)
                            }
                    case .completion:
                        CompletionView {
                            onCreate(sessionParams, takeParams, $0)
                        }
                        .toolbar {
                            Button("Cancel", action: onCancel)
                        }
                    }
                }
                .toolbar {
                    Button("Next") {
                        navigationPath.append(.specification)
                    }
                    .disabled(sessionParams.selection.isEmpty)
                }
        }
    }
    
    struct WelcomeView : View {
        @Binding var sessionParams: SessionParameters
        @State private var searchText = ""
        @State private var isBrowserExpanded = false
        @State private var browserState: QuestionSelector.DataState?
        @State private var selectedSuggestion: (any Option)? = nil

        var body: some View {
            ScrollView {
                HStack {
                    Text("Suggestions")
                        .padding(.horizontal)
                        .foregroundStyle(.secondary)
                        .font(.subheadline.bold())
                    Spacer()
                }
                SuggestionSelector(selection: Binding(get: {
                    selectedSuggestion
                }, set: { newValue in
                    selectedSuggestion = newValue
                    if let creator = newValue?.kt as? ComposeApp.SessionCreator {
                        sessionParams = .init(from: creator)
                    }
                }), searchText: searchText)
                
                Divider()
                    .padding(.leading)
                    .padding(.bottom)
                HStack {
                    Text("Browse")
                        .foregroundStyle(.secondary)
                    Spacer()
                    if case .ok(let quizzes, _) = browserState, quizzes.count > 5 {
                        Button(isBrowserExpanded ? "Less" : "More") {
                            isBrowserExpanded = !isBrowserExpanded
                        }
                        .buttonStyle(.borderless)
                        .animation(.default, value: isBrowserExpanded)
                    }
                }
                .padding(.horizontal)
                .font(.subheadline.bold())
                Divider()
                    .padding(.leading)
                QuestionSelector(selection: Binding(get: {
                    sessionParams.selection
                }, set: { newValue in
                    sessionParams = SessionParameters(name: sessionParams.name, selection: newValue)
                    selectedSuggestion = nil
                }), searchText: searchText, data: Binding(get: {
                    if case .ok(let quizzes, let dims) = browserState {
                        .ok(isBrowserExpanded ? quizzes : Array(quizzes[0..<5]), dims)
                    } else {
                        browserState
                    }
                }, set: { newValue in
                    browserState = newValue
                }))
                .animation(.default, value: isBrowserExpanded)
                .padding()
            }
            .listStyle(.inset)
            .searchable(text: $searchText)
        }
    }
    
    struct CompletionView : View {
        let onComplete: (CompletionTask) -> Void
        var body: some View {
            VStack {
                VStack {
                    Image(systemName: "checkmark.circle.fill")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .foregroundStyle(.green)
                        .frame(width: 64, height: 64)
                        .padding(.bottom, 20)
                    Text("Almost There")
                        .font(.title)
                        .padding(.bottom, 10)
                    Text("Would you like to start the newly created session immediately?")
                        .multilineTextAlignment(.center)
                }
                .frame(maxHeight: .infinity)
                
                Button {
                    onComplete(.startTake)
                } label: {
                    Text("Start now")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.borderedProminent)
                .padding(.horizontal, 32)
                .padding(.bottom, 12)
                
                Button("Continue without starting") {
                    onComplete(.reveal)
                }
                .buttonStyle(.borderless)
                .padding(.bottom, 32)
            }
        }
    }
    
    struct SpecificationView : View {
        @Binding var sessionParams: SessionParameters
        @Binding var takeParams: TimerParameters?
        
        @State private var takeParamsBuffer: TimerParameters
        @State private var shownTimerPicker: UUID? = nil
        
        init(sessionParams: Binding<SessionParameters>, takeParams: Binding<TimerParameters?>) {
            self._sessionParams = sessionParams
            self._takeParams = takeParams
            
            if let takeParams = takeParams.wrappedValue {
                takeParamsBuffer = takeParams
            } else {
                takeParamsBuffer = TimerParameters()
            }
        }
        
        var body: some View {
            List {
                Section("Session") {
                    TextField(text: Binding(get: {
                        sessionParams.name
                    }, set: { newValue in
                        sessionParams = SessionParameters(name: newValue, selection: sessionParams.selection)
                    }), prompt: Text("Name of the session")) {
                        Text("Session name")
                    }
                }
                Section("Take") {
                    Picker(selection: Binding(get: {
                        takeParams != nil
                    }, set: { newValue in
                        withAnimation {
                            if newValue {
                                takeParams = takeParamsBuffer
                            } else {
                                takeParams = nil
                            }
                        }
                    })) {
                        Text("Don't create takes for now").tag(false)
                        Text("Begin a new take as well").tag(true)
                    } label: {
                        EmptyView()
                    }
                    .pickerStyle(.inline)
                }
                
                
                if let takeParams = takeParams {
                    Section("Timers") {
                        ForEach(takeParams.timers) { timer in
                            TimerItem(
                                timer: Binding(get: {
                                    timer
                                }, set: { newValue in
                                    let index = takeParams.timers.firstIndex(where: {$0.id == timer.id})!
                                    updateTimer(timers: Array(takeParams.timers[0..<index] + [newValue] + takeParams.timers[(index+1)...]))
                                }),
                                isPickerShown: Binding(get: {
                                    shownTimerPicker == timer.id
                                }, set: { newValue in
                                    if newValue {
                                        shownTimerPicker = timer.id
                                    } else {
                                        shownTimerPicker = nil
                                    }
                                })
                            )
                            .swipeActions {
                                Button("Remove", systemImage: "trash", role: .destructive) {
                                    let index = takeParams.timers.firstIndex(where: {$0.id == timer.id})!
                                    updateTimer(timers: Array(takeParams.timers[0..<index] + takeParams.timers[(index+1)...]))
                                }
                            }
                        }
                        Button("Add Timer") {
                            withAnimation {
                                updateTimer(timers: takeParams.timers + [Timer(timeout: Duration.seconds(600))])
                            }
                        }
                    }
                }
            }
        }
        
        func updateTimer(timers: [Timer]) {
            takeParamsBuffer = TimerParameters(timers: timers)
            if takeParams != nil {
                takeParams = takeParamsBuffer
            }
        }
        
        private struct TimerItem : View {
            @Binding var timer: Timer
            @Binding var isPickerShown: Bool
            @State private var hours: Int
            @State private var minutes: Int
            @State private var seconds: Int
            
            init(timer: Binding<Timer>, isPickerShown: Binding<Bool>) {
                self._timer = timer
                self._isPickerShown = isPickerShown
                
                let hour = Duration.seconds(60*60)
                let minute = Duration.seconds(60)
                let hours = Int(timer.wrappedValue.timeout / hour)
                let minutes = Int((timer.wrappedValue.timeout - hour * hours) / minute)
                let seconds = Int((timer.wrappedValue.timeout - hour * hours - minute * minutes) / Duration.seconds(1))
                
                self.hours = hours
                self.minutes = minutes
                self.seconds = seconds
            }
            
            var body: some View {
                VStack(alignment: .leading) {
                    Button {
                        withAnimation {
                            isPickerShown = !isPickerShown
                        }
                    } label: {
                        HStack {
                            Text(timer.timeout.formatted())
                            Spacer()
                            Image(systemName: "chevron.right")
                                .rotationEffect(isPickerShown ? .degrees(90) : .degrees(0))
                                .foregroundStyle(.secondary)
                        }
                        .foregroundStyle(.foreground)
                    }
                }
                
                if isPickerShown {
                    DurationPicker(
                        hourSelection: $hours,
                        minuteSelection: $minutes,
                        secondSelection: $seconds
                    )
                    .frame(height: 200)
                    .onChange(of: hours) { _, newValue in
                        timer.timeout = Duration(hours: newValue, minutes: minutes, seconds: seconds)
                    }
                    .onChange(of: minutes) { _, newValue in
                        timer.timeout = Duration(hours: hours, minutes: newValue, seconds: seconds)
                    }
                    .onChange(of: seconds) { _, newValue in
                        timer.timeout = Duration(hours: hours, minutes: minutes, seconds: newValue)
                    }
                }
            }
        }
    }
}

#Preview {
    SessionCreatorView { session, timer, task in
        
    } onCancel: {
        
    }
}
