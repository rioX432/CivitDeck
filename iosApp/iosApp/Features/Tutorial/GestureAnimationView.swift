import SwiftUI

// MARK: - Swipe Discovery Animation

struct SwipeDiscoveryAnimationView: View {
    let accentColor: Color
    @State private var offsetX: CGFloat = -30

    var body: some View {
        ZStack {
            cardShape
            fingerCircle(x: offsetX, y: 72)
        }
        .frame(width: 200, height: 200)
        .onAppear { startAnimation() }
    }

    private var cardShape: some View {
        RoundedRectangle(cornerRadius: CornerRadius.card)
            .fill(Color.civitSurfaceContainerHigh)
            .frame(width: 90, height: 110)
    }

    private func fingerCircle(x: CGFloat, y: CGFloat) -> some View {
        ZStack {
            Circle()
                .fill(accentColor.opacity(0.25))
                .frame(width: 24, height: 24)
            Circle()
                .fill(accentColor)
                .frame(width: 12, height: 12)
        }
        .offset(x: x, y: y)
    }

    private func startAnimation() {
        withAnimation(
            .easeInOut(duration: 1.0)
            .repeatForever(autoreverses: true)
        ) {
            offsetX = 30
        }
    }
}

// MARK: - Quick Actions Animation

struct QuickActionsAnimationView: View {
    let accentColor: Color
    @State private var offsetX: CGFloat = 0

    var body: some View {
        ZStack {
            revealArea
            cardShape
            fingerCircle
        }
        .frame(width: 200, height: 200)
        .onAppear { startAnimation() }
    }

    private var revealArea: some View {
        RoundedRectangle(cornerRadius: CornerRadius.card)
            .fill(accentColor.opacity(0.3))
            .frame(width: 90, height: 110)
    }

    private var cardShape: some View {
        RoundedRectangle(cornerRadius: CornerRadius.card)
            .fill(Color.civitSurfaceContainerHigh)
            .frame(width: 90, height: 110)
            .offset(x: offsetX)
    }

    private var fingerCircle: some View {
        ZStack {
            Circle()
                .fill(accentColor.opacity(0.25))
                .frame(width: 24, height: 24)
            Circle()
                .fill(accentColor)
                .frame(width: 12, height: 12)
        }
        .offset(x: offsetX, y: 72)
    }

    private func startAnimation() {
        withAnimation(
            .easeInOut(duration: 1.0)
            .repeatForever(autoreverses: true)
        ) {
            offsetX = 25
        }
    }
}

// MARK: - Image Comparison Animation

struct ImageComparisonAnimationView: View {
    let accentColor: Color
    @State private var sliderX: CGFloat = -30

    var body: some View {
        ZStack {
            comparisonArea
            dividerLine
            fingerCircle
        }
        .frame(width: 200, height: 200)
        .onAppear { startAnimation() }
    }

    private var comparisonArea: some View {
        HStack(spacing: 0) {
            Rectangle()
                .fill(Color.civitSurfaceContainerHigh)
            Rectangle()
                .fill(Color.civitSurfaceContainerHighest)
        }
        .frame(width: 140, height: 120)
        .offset(y: -10)
    }

    private var dividerLine: some View {
        Rectangle()
            .fill(accentColor)
            .frame(width: 2, height: 120)
            .offset(x: sliderX, y: -10)
    }

    private var fingerCircle: some View {
        ZStack {
            Circle()
                .fill(accentColor.opacity(0.25))
                .frame(width: 24, height: 24)
            Circle()
                .fill(accentColor)
                .frame(width: 12, height: 12)
        }
        .offset(x: sliderX, y: 62)
    }

    private func startAnimation() {
        withAnimation(
            .easeInOut(duration: 1.0)
            .repeatForever(autoreverses: true)
        ) {
            sliderX = 30
        }
    }
}
