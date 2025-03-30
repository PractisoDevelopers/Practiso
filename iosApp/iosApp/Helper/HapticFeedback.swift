//
//  Vibrator.swift
//  iosApp
//
//  Created by Steve Reed on 2025/1/6.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import ComposeApp
import CoreHaptics

enum FeatureUnavailableError : Error {
    case haptics
}

class CoreHapticFeedback : Vibrator {
    @MainActor
    static let shared: CoreHapticFeedback? = try? .init()
    
    let engine: CHHapticEngine
    
    init() throws {
        if (CHHapticEngine.capabilitiesForHardware().supportsHaptics) {
            engine = try CHHapticEngine()
            self.engine.resetHandler = {
                do {
                    try self.engine.start()
                } catch let error {
                    print("Error restarting HapticEngine: \(error)")
                }
            }
        } else {
            throw FeatureUnavailableError.haptics
        }
    }
    
    func wobble() {
        guard let url = Bundle.main.url(forResource: "AHAP/Wobble", withExtension: "ahap") else {
            return
        }
        
        do {
            try engine.start()
            try engine.playPattern(from: url)
        } catch let error {
            print("Failed to play wobble pattern: \(error)")
        }
    }
}
