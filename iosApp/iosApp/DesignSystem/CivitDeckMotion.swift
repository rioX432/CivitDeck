import SwiftUI

// MARK: - Duration
enum MotionDuration {
    static let fast: Double = 0.15
    static let normal: Double = 0.3
    static let slow: Double = 0.5
}

// MARK: - Animation presets
enum MotionAnimation {
    static let standard = Animation.easeInOut(duration: MotionDuration.normal)
    static let fast = Animation.easeInOut(duration: MotionDuration.fast)
    static let slow = Animation.easeInOut(duration: MotionDuration.slow)

    // Enter (decelerate)
    static let enter = Animation.easeOut(duration: MotionDuration.normal)
    static let enterFast = Animation.easeOut(duration: MotionDuration.fast)

    // Exit (accelerate)
    static let exit = Animation.easeIn(duration: MotionDuration.normal)
    static let exitFast = Animation.easeIn(duration: MotionDuration.fast)

    // Spring
    static let spring = Animation.spring(response: 0.3, dampingFraction: 0.7)
    static let springBouncy = Animation.spring(response: 0.3, dampingFraction: 0.5)
    static let springStiff = Animation.spring(response: 0.2, dampingFraction: 1.0)
    static let springGentle = Animation.spring(response: 0.5, dampingFraction: 0.6)
}

// MARK: - Stagger
enum MotionStagger {
    static let delayPerItem: Double = 0.05
    static let maxDelay: Double = 0.3
    static let initialOffsetY: CGFloat = 24
}

// MARK: - Parallax
enum MotionParallax {
    static let factor: CGFloat = 0.15
    static let maxOffset: CGFloat = 20
}

// MARK: - Accessibility
enum MotionAccessibility {
    static var isReducedMotionEnabled: Bool {
        UIAccessibility.isReduceMotionEnabled
    }
}
