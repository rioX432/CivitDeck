import SwiftUI

/// Full-screen overlay for comparing two images with a slider.
///
/// Shows a close button, orientation toggle, and before/after labels.
struct ImageComparisonOverlay: View {
    let beforeImageUrl: String
    let afterImageUrl: String
    var beforeLabel: String = "Before"
    var afterLabel: String = "After"
    let onDismiss: () -> Void

    @State private var orientation: SliderOrientation = .horizontal

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            ImageComparisonSlider(
                beforeImageUrl: beforeImageUrl,
                afterImageUrl: afterImageUrl,
                orientation: orientation
            )

            overlayControls
        }
    }

    // MARK: - Controls

    private var overlayControls: some View {
        VStack {
            topBar
            Spacer()
            bottomLabels
        }
        .padding(controlPadding)
    }

    private var topBar: some View {
        HStack {
            OverlayCircleButton(systemName: "xmark", action: onDismiss)
            Spacer()
            OverlayCircleButton(
                systemName: orientation == .horizontal
                    ? "arrow.up.and.down"
                    : "arrow.left.and.right"
            ) {
                withAnimation(MotionAnimation.fast) {
                    orientation = orientation == .horizontal ? .vertical : .horizontal
                }
            }
            .accessibilityLabel("Toggle comparison orientation")
        }
    }

    private var bottomLabels: some View {
        HStack {
            ComparisonLabelChip(text: beforeLabel)
            Spacer()
            ComparisonLabelChip(text: afterLabel)
        }
    }
}

// MARK: - Overlay Circle Button

private struct OverlayCircleButton: View {
    let systemName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.civitInverseOnSurface)
                .padding(buttonPadding)
                .background(.ultraThinMaterial, in: Circle())
        }
    }
}

// MARK: - Label Chip

private struct ComparisonLabelChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.civitLabelMedium)
            .foregroundColor(.civitInverseOnSurface)
            .padding(.horizontal, chipHPadding)
            .padding(.vertical, chipVPadding)
            .background(Color.black.opacity(chipAlpha), in: Capsule())
    }
}

// MARK: - Constants

private let controlPadding: CGFloat = 16
private let buttonPadding: CGFloat = 10
private let chipHPadding: CGFloat = 12
private let chipVPadding: CGFloat = 6
private let chipAlpha: Double = 0.7
