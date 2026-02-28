import SwiftUI

// MARK: - Swipe Discovery Animation

struct SwipeDiscoveryAnimationView: View {
    let accentColor: Color
    @State private var offsetX: CGFloat = Layout.swipeStartX

    private enum Layout {
        static let frameSize: CGFloat = 200
        static let cardWidth: CGFloat = 90
        static let cardHeight: CGFloat = 110
        static let fingerOuterSize: CGFloat = 24
        static let fingerInnerSize: CGFloat = 12
        static let fingerY: CGFloat = 72
        static let swipeStartX: CGFloat = -30
        static let swipeEndX: CGFloat = 30
    }

    var body: some View {
        ZStack {
            cardShape
            fingerCircle(x: offsetX, y: Layout.fingerY)
        }
        .frame(width: Layout.frameSize, height: Layout.frameSize)
        .onAppear { startAnimation() }
    }

    private var cardShape: some View {
        RoundedRectangle(cornerRadius: CornerRadius.card)
            .fill(Color.civitSurfaceContainerHigh)
            .frame(width: Layout.cardWidth, height: Layout.cardHeight)
    }

    private func fingerCircle(x: CGFloat, y: CGFloat) -> some View {
        ZStack {
            Circle()
                .fill(accentColor.opacity(0.25))
                .frame(width: Layout.fingerOuterSize, height: Layout.fingerOuterSize)
            Circle()
                .fill(accentColor)
                .frame(width: Layout.fingerInnerSize, height: Layout.fingerInnerSize)
        }
        .offset(x: x, y: y)
    }

    private func startAnimation() {
        withAnimation(
            .easeInOut(duration: 1.0)
            .repeatForever(autoreverses: true)
        ) {
            offsetX = Layout.swipeEndX
        }
    }
}

// MARK: - Quick Actions Animation

struct QuickActionsAnimationView: View {
    let accentColor: Color
    @State private var offsetX: CGFloat = 0

    private enum Layout {
        static let frameSize: CGFloat = 200
        static let cardWidth: CGFloat = 90
        static let cardHeight: CGFloat = 110
        static let fingerOuterSize: CGFloat = 24
        static let fingerInnerSize: CGFloat = 12
        static let fingerY: CGFloat = 72
        static let swipeEndX: CGFloat = 25
    }

    var body: some View {
        ZStack {
            revealArea
            cardShape
            fingerCircle
        }
        .frame(width: Layout.frameSize, height: Layout.frameSize)
        .onAppear { startAnimation() }
    }

    private var revealArea: some View {
        RoundedRectangle(cornerRadius: CornerRadius.card)
            .fill(accentColor.opacity(0.3))
            .frame(width: Layout.cardWidth, height: Layout.cardHeight)
    }

    private var cardShape: some View {
        RoundedRectangle(cornerRadius: CornerRadius.card)
            .fill(Color.civitSurfaceContainerHigh)
            .frame(width: Layout.cardWidth, height: Layout.cardHeight)
            .offset(x: offsetX)
    }

    private var fingerCircle: some View {
        ZStack {
            Circle()
                .fill(accentColor.opacity(0.25))
                .frame(width: Layout.fingerOuterSize, height: Layout.fingerOuterSize)
            Circle()
                .fill(accentColor)
                .frame(width: Layout.fingerInnerSize, height: Layout.fingerInnerSize)
        }
        .offset(x: offsetX, y: Layout.fingerY)
    }

    private func startAnimation() {
        withAnimation(
            .easeInOut(duration: 1.0)
            .repeatForever(autoreverses: true)
        ) {
            offsetX = Layout.swipeEndX
        }
    }
}

// MARK: - Image Comparison Animation

struct ImageComparisonAnimationView: View {
    let accentColor: Color
    @State private var sliderX: CGFloat = Layout.sliderStartX

    private enum Layout {
        static let frameSize: CGFloat = 200
        static let comparisonWidth: CGFloat = 140
        static let comparisonHeight: CGFloat = 120
        static let comparisonOffsetY: CGFloat = -10
        static let dividerWidth: CGFloat = 2
        static let dividerHeight: CGFloat = 120
        static let fingerOuterSize: CGFloat = 24
        static let fingerInnerSize: CGFloat = 12
        static let fingerY: CGFloat = 62
        static let sliderStartX: CGFloat = -30
        static let sliderEndX: CGFloat = 30
    }

    var body: some View {
        ZStack {
            comparisonArea
            dividerLine
            fingerCircle
        }
        .frame(width: Layout.frameSize, height: Layout.frameSize)
        .onAppear { startAnimation() }
    }

    private var comparisonArea: some View {
        HStack(spacing: 0) {
            Rectangle()
                .fill(Color.civitSurfaceContainerHigh)
            Rectangle()
                .fill(Color.civitSurfaceContainerHighest)
        }
        .frame(width: Layout.comparisonWidth, height: Layout.comparisonHeight)
        .offset(y: Layout.comparisonOffsetY)
    }

    private var dividerLine: some View {
        Rectangle()
            .fill(accentColor)
            .frame(width: Layout.dividerWidth, height: Layout.dividerHeight)
            .offset(x: sliderX, y: Layout.comparisonOffsetY)
    }

    private var fingerCircle: some View {
        ZStack {
            Circle()
                .fill(accentColor.opacity(0.25))
                .frame(width: Layout.fingerOuterSize, height: Layout.fingerOuterSize)
            Circle()
                .fill(accentColor)
                .frame(width: Layout.fingerInnerSize, height: Layout.fingerInnerSize)
        }
        .offset(x: sliderX, y: Layout.fingerY)
    }

    private func startAnimation() {
        withAnimation(
            .easeInOut(duration: 1.0)
            .repeatForever(autoreverses: true)
        ) {
            sliderX = Layout.sliderEndX
        }
    }
}
