import SwiftUI

// MARK: - Staggered Entrance Modifier

/// Applies a staggered fade-in + slide-up animation based on item index.
/// Respects the system's Reduce Motion accessibility setting.
struct StaggeredEntranceModifier: ViewModifier {
    let index: Int
    @State private var isVisible = false

    func body(content: Content) -> some View {
        if MotionAccessibility.isReducedMotionEnabled {
            content
        } else {
            content
                .opacity(isVisible ? 1 : 0)
                .offset(y: isVisible ? 0 : MotionStagger.initialOffsetY)
                .onAppear {
                    let delay = min(
                        Double(index) * MotionStagger.delayPerItem,
                        MotionStagger.maxDelay
                    )
                    withAnimation(MotionAnimation.enter.delay(delay)) {
                        isVisible = true
                    }
                }
        }
    }
}

extension View {
    /// Applies a staggered entrance animation to the view.
    /// - Parameter index: The item index used to calculate stagger delay.
    func staggeredEntrance(index: Int) -> some View {
        modifier(StaggeredEntranceModifier(index: index))
    }
}

// MARK: - Parallax Modifier

/// Applies a vertical parallax offset based on the view's position in the viewport.
/// Respects the system's Reduce Motion accessibility setting.
struct ParallaxModifier: ViewModifier {
    let offset: CGFloat

    func body(content: Content) -> some View {
        if MotionAccessibility.isReducedMotionEnabled {
            content
        } else {
            let parallaxY = (offset * MotionParallax.factor)
                .clamped(to: -MotionParallax.maxOffset...MotionParallax.maxOffset)
            content
                .offset(y: parallaxY)
        }
    }
}

extension View {
    /// Applies a parallax vertical shift based on scroll offset.
    /// - Parameter offset: The item's distance from the viewport center.
    func parallaxEffect(offset: CGFloat) -> some View {
        modifier(ParallaxModifier(offset: offset))
    }
}

// MARK: - Spring Press Modifier

/// Applies a spring-based scale animation on press.
/// Respects the system's Reduce Motion accessibility setting.
struct SpringPressModifier: ViewModifier {
    @State private var isPressed = false

    func body(content: Content) -> some View {
        if MotionAccessibility.isReducedMotionEnabled {
            content
        } else {
            content
                .scaleEffect(isPressed ? 0.96 : 1.0)
                .animation(MotionAnimation.springBouncy, value: isPressed)
                .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
                    isPressed = pressing
                }, perform: {})
        }
    }
}

extension View {
    /// Applies a spring-based press scale effect to the view.
    func springPress() -> some View {
        modifier(SpringPressModifier())
    }
}

// MARK: - Comparable Clamped

private extension Comparable where Self: AdditiveArithmetic {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
