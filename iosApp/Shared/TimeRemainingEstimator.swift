//
//  TimeRemainingEstimator.swift
//  iosApp
//
//  Created by Claude on 2026/5/11.
//


import Foundation

/// Estimates remaining time for a job using an exponential moving average
/// over observed throughput samples.
///
/// Usage:
/// ```swift
/// let estimator = TimeRemainingEstimator()
/// estimator.start(total: 1_000_000)
///
/// // Call as work completes:
/// if let remaining = estimator.elapsed(done: 250_000, total: 1_000_000) {
///     print("ETA: \(remaining.formatted())")
/// }
/// ```
public final class TimeRemainingEstimator {

    // MARK: - Configuration

    /// Controls how quickly the EMA adapts to throughput changes.
    /// - Closer to 1.0 → heavily weights recent samples (reactive, noisier).
    /// - Closer to 0.0 → heavily weights history (stable, slower to adapt).
    public var smoothingFactor: Double

    /// Minimum number of samples required before returning an estimate.
    public var minimumSamples: Int

    // MARK: - Private state

    private var startTime: Date?
    private var lastSampleTime: Date?
    private var lastDone: Int64 = 0

    /// Exponential moving average of units-per-second throughput.
    private var emaThroughput: Double?

    private var sampleCount: Int = 0

    // MARK: - Init

    /// - Parameters:
    ///   - smoothingFactor: EMA α in (0, 1]. Default `0.25`.
    ///   - minimumSamples: Samples needed before producing an estimate. Default `2`.
    public init(smoothingFactor: Double = 0.25, minimumSamples: Int = 2) {
        precondition((0.0...1.0).contains(smoothingFactor),
                     "smoothingFactor must be in 0...1")
        precondition(minimumSamples >= 1, "minimumSamples must be ≥ 1")
        self.smoothingFactor = smoothingFactor
        self.minimumSamples = minimumSamples
    }

    // MARK: - Public API

    /// Explicitly mark the start of the job and reset all state.
    /// Optional — `elapsed(done:total:)` auto-starts on first call.
    public func start(total: Int64 = 0) {
        reset()
        startTime = Date()
    }

    /// Reset estimator back to its initial state.
    public func reset() {
        startTime = nil
        lastSampleTime = nil
        lastDone = 0
        emaThroughput = nil
        sampleCount = 0
    }

    /// Feed a progress update and receive an estimated remaining duration.
    ///
    /// - Parameters:
    ///   - done:  Units completed so far.
    ///   - total: Total units in the job.
    /// - Returns: Estimated `TimeInterval` (seconds) remaining, or `nil` if
    ///            insufficient data is available yet.
    @discardableResult
    public func elapsed(done: Int64, total: Int64) -> TimeInterval? {
        guard total > 0, done >= 0, done <= total else { return nil }

        let now = Date()

        // Auto-start on first observation.
        if startTime == nil {
            startTime = now
            lastSampleTime = now
            lastDone = done
            return nil
        }

        let sampleStart = lastSampleTime ?? startTime ?? now
        let dt = now.timeIntervalSince(sampleStart)
        let unitsDelta = done - lastDone

        // Only record a sample when meaningful time and work have passed.
        if dt > 0, unitsDelta > 0 {
            let throughput = Double(unitsDelta) / dt   // units / second

            if let prev = emaThroughput {
                emaThroughput = smoothingFactor * throughput + (1.0 - smoothingFactor) * prev
            } else {
                emaThroughput = throughput
            }

            sampleCount += 1
            lastSampleTime = now
            lastDone = done
        }

        guard sampleCount >= minimumSamples,
              let rate = emaThroughput, rate > 0 else { return nil }

        let remaining = Int64(total) - done
        return Double(remaining) / rate
    }

    // MARK: - Convenience

    /// Elapsed wall-clock time since `start()` or first `elapsed(done:total:)` call.
    public var elapsedTime: TimeInterval? {
        startTime.map { Date().timeIntervalSince($0) }
    }

    /// Most recent EMA throughput estimate (units per second).
    public var throughputPerSecond: Double? { emaThroughput }
}

// MARK: - TimeInterval formatting helper

public extension TimeInterval {
    /// Returns a human-readable string such as "2h 4m 37s" or "45s".
    func formatted() -> String {
        guard self >= 0, !isNaN, !isInfinite else { return "Unknown" }

        let total   = Int(self.rounded())
        let hours   = total / 3600
        let minutes = (total % 3600) / 60
        let seconds = total % 60

        switch (hours, minutes) {
        case (0, 0): return "\(seconds)s"
        case (0, _): return "\(minutes)m \(seconds)s"
        default:     return "\(hours)h \(minutes)m \(seconds)s"
        }
    }
}

// MARK: - Example / smoke-test

#if DEBUG
func runDemo() {
    let estimator = TimeRemainingEstimator(smoothingFactor: 0.3, minimumSamples: 2)
    let total: Int64 = 10_000

    print("Simulating a \(total)-unit job...\n")

    var done: Int64 = 0
    var tick = 0

    // Simulate uneven throughput: fast start, slow middle, fast end.
    while done < total {
        let phase = Double(done) / Double(total)
        let rate: Int64 = phase < 0.4 ? 400 : (phase < 0.7 ? 150 : 500)
        done = min(done + rate, total)
        tick += 1

        // Pretend 0.25 s passes each iteration (demo only; real code uses wall time).
        if let eta = estimator.elapsed(done: done, total: total) {
            let pct = 100.0 * Double(done) / Double(total)
            print(String(format: "  tick %2d | %6.1f%% done | ETA: %@",
                         tick, pct, eta.formatted()))
        }
    }

    print("\nDone! Total wall time: \(estimator.elapsedTime.map { "\($0.formatted())" } ?? "n/a")")
}

#endif
