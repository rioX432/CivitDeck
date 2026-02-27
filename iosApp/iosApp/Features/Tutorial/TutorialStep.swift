import SwiftUI

struct TutorialStep: Identifiable {
    let id: Int
    let title: String
    let description: String
    let accentColor: Color
    let animationType: AnimationType

    enum AnimationType {
        case swipeDiscovery
        case quickActions
        case imageComparison
    }
}

let tutorialSteps: [TutorialStep] = [
    TutorialStep(
        id: 0,
        title: "Swipe to Discover",
        description: "Swipe left or right on model cards to discover new models. Swipe up to skip a model.",
        accentColor: .civitPrimary,
        animationType: .swipeDiscovery
    ),
    TutorialStep(
        id: 1,
        title: "Quick Actions",
        description: "Swipe a model card to reveal quick actions like favorite, download, or hide.",
        accentColor: .civitTertiary,
        animationType: .quickActions
    ),
    TutorialStep(
        id: 2,
        title: "Image Comparison",
        description: "Drag the slider on image comparisons to reveal before and after results side by side.",
        accentColor: .civitSecondary,
        animationType: .imageComparison
    ),
]
