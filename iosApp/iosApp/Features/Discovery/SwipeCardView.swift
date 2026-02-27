import SwiftUI
import Shared

enum SwipeDirection {
    case left
    case right
    case up
}

private let swipeThresholdFraction: CGFloat = 0.3
private let upSwipeThresholdFraction: CGFloat = 0.25
private let maxRotationDegrees: Double = 15.0

struct SwipeCardView: View {
    let model: Model
    let screenWidth: CGFloat
    let onSwiped: (SwipeDirection) -> Void

    @State private var offset: CGSize = .zero
    @State private var isDragging = false

    private var swipeThreshold: CGFloat { screenWidth * swipeThresholdFraction }

    var body: some View {
        DiscoveryCardContent(model: model)
            .offset(x: offset.width, y: offset.height)
            .rotationEffect(rotation)
            .simultaneousGesture(dragGesture)
            .animation(isDragging ? nil : MotionAnimation.spring, value: offset)
    }

    private var rotation: Angle {
        .degrees(Double(offset.width / screenWidth) * maxRotationDegrees)
    }

    private var dragGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                isDragging = true
                offset = value.translation
            }
            .onEnded { value in
                isDragging = false
                let translation = value.translation
                let upThreshold = screenWidth * upSwipeThresholdFraction

                if translation.height < -upThreshold {
                    dismissCard(direction: .up)
                } else if translation.width > swipeThreshold {
                    dismissCard(direction: .right)
                } else if translation.width < -swipeThreshold {
                    dismissCard(direction: .left)
                } else {
                    offset = .zero
                }
            }
    }

    private func dismissCard(direction: SwipeDirection) {
        let targetOffset: CGSize
        switch direction {
        case .left:
            targetOffset = CGSize(width: -screenWidth * 2, height: offset.height)
        case .right:
            targetOffset = CGSize(width: screenWidth * 2, height: offset.height)
        case .up:
            targetOffset = CGSize(width: offset.width, height: -screenWidth * 2)
        }

        withAnimation(.easeOut(duration: MotionDuration.normal)) {
            offset = targetOffset
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + MotionDuration.normal) {
            onSwiped(direction)
        }
    }
}
