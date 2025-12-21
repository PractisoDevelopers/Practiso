//
//  Delay.swift
//  iosApp
//
//  Created by Steve Reed on 2025/12/21.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import SwiftUI

struct Delay<Content: View>: View {
    let `for`: Duration = .seconds(2)
    @ViewBuilder
    let content: (Bool) -> Content
    @State
    var pass: Bool = false

    var body: some View {
        content(pass)
            .task {
                try? await Task.sleep(for: `for`)
                pass = true
            }
    }
}
