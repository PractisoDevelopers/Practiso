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
        case none
    }
    
    @Environment(ContentView.Model.self) private var contentModel
    
    @State private var navigationPath: [Page] = []
    @Binding var model: Model
    let onCreate: (CompletionTask) -> Void
    let onCancel: () -> Void
    
    var body: some View {
        NavigationStack(path: $navigationPath) {
            WelcomeView(sessionParams: $model.sessionParams, selectedSuggestion: $model.selectedSuggestion)
                .navigationTitle("Explore")
                .navigationDestination(for: Page.self) { page in
                    switch page {
                    case .specification:
                        SpecificationView(sessionParams: $model.sessionParams, takeParams: $model.takeParams)
                            .navigationTitle("Specification")
                            .toolbar {
                                Group {
                                    if let _ = model.takeParams {
                                        Button("Next") {
                                            navigationPath.append(.completion)
                                        }
                                    } else {
                                        Button("Done") {
                                            onCreate(.reveal)
                                        }
                                    }
                                }
                                .disabled(model.sessionParams.name.trimmingCharacters(in: .whitespaces).isEmpty)
                            }
                    case .completion:
                        CompletionView(onComplete: onCreate)
                        .toolbar {
                            Button("Cancel", action: onCancel)
                        }
                    }
                }
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        Button("Next") {
                            navigationPath.append(.specification)
                        }
                        .disabled(model.sessionParams.selection.isEmpty)
                    }
                    ToolbarItem(placement: .topBarLeading) {
                        Button("Reset") {
                            model = Model()
                        }
                        .disabled(model.isEmpty)
                    }
                    ToolbarItem(placement: .status) {
                        Text("\(model.sessionParams.selection.quizIds.count + model.sessionParams.selection.dimensionIds.count) items selected")
                            .font(.footnote)
                    }
                }
        }
    }
    
    struct WelcomeView : View {
        @Binding var sessionParams: SessionParameters
        @Binding var selectedSuggestion: (any Option)?
        @State private var searchText = ""
        @State private var isSearchExpanded = false
        @State private var isBrowserExpanded = false
        @State private var browserState: QuestionSelector.DataState?

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
                    if case .ok(let quizzes, _) = browserState, quizzes.count > 5 && !isSearchExpanded {
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
                        .ok(isBrowserExpanded || isSearchExpanded ? quizzes : Array(quizzes[0..<min(5, quizzes.count)]), dims)
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
            .searchable(text: $searchText, isPresented: $isSearchExpanded)
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
                        TimerView(value: Binding(get: {
                            takeParams.timers
                        }, set: { timers, _ in
                            updateTimer(timers: timers)
                        }))
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
        
    }
}

#Preview {
    @Previewable @State var model = SessionCreatorView.Model()

    SessionCreatorView(model: $model) { task in
        
    } onCancel: {
        
    }
}
