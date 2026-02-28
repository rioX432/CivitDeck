import SwiftUI

struct GestureTutorialView: View {
    let onDismiss: () -> Void
    @State private var currentPage = 0

    private enum Layout {
        static let tabViewHeight: CGFloat = 340
        static let indicatorSize: CGFloat = 8
        static let buttonMaxWidth: CGFloat = 200
    }

    var body: some View {
        VStack(spacing: 0) {
            skipButton
            Spacer()
            tutorialContent
            Spacer()
            pageIndicator
                .padding(.bottom, Spacing.lg)
            navigationButton
                .padding(.bottom, Spacing.lg)
            Spacer()
        }
        .padding(Spacing.lg)
        .background(Color.civitSurface)
    }

    private var skipButton: some View {
        HStack {
            Spacer()
            Button("Skip") {
                onDismiss()
            }
            .foregroundColor(.civitPrimary)
        }
    }

    private var tutorialContent: some View {
        TabView(selection: $currentPage) {
            ForEach(tutorialSteps) { step in
                TutorialPageView(step: step)
                    .tag(step.id)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .frame(height: Layout.tabViewHeight)
    }

    private var pageIndicator: some View {
        HStack(spacing: Spacing.sm) {
            ForEach(tutorialSteps) { step in
                Circle()
                    .fill(step.id == currentPage ? Color.civitPrimary : Color.civitOutlineVariant)
                    .frame(width: Layout.indicatorSize, height: Layout.indicatorSize)
            }
        }
    }

    private var navigationButton: some View {
        Button(action: {
            if currentPage < tutorialSteps.count - 1 {
                withAnimation(MotionAnimation.standard) {
                    currentPage += 1
                }
            } else {
                onDismiss()
            }
        }) {
            Text(currentPage == tutorialSteps.count - 1 ? "Get Started" : "Next")
                .font(.civitTitleMedium)
                .foregroundColor(.white)
                .frame(maxWidth: Layout.buttonMaxWidth)
                .padding(.vertical, Spacing.md)
                .background(Color.civitPrimary)
                .cornerRadius(CornerRadius.card)
        }
    }
}

// MARK: - Tutorial Page

private struct TutorialPageView: View {
    let step: TutorialStep

    var body: some View {
        VStack(spacing: 0) {
            stepAnimation
                .padding(.bottom, Spacing.xxl)
            Text(step.title)
                .font(.civitHeadlineSmall)
                .foregroundColor(.civitOnSurface)
                .multilineTextAlignment(.center)
                .padding(.bottom, Spacing.md)
            Text(step.description)
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, Spacing.xl)
        }
    }

    @ViewBuilder
    private var stepAnimation: some View {
        switch step.animationType {
        case .swipeDiscovery:
            SwipeDiscoveryAnimationView(accentColor: step.accentColor)
        case .quickActions:
            QuickActionsAnimationView(accentColor: step.accentColor)
        case .imageComparison:
            ImageComparisonAnimationView(accentColor: step.accentColor)
        }
    }
}
