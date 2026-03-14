import SwiftUI
import Shared

/// Wraps a ModelCardView with horizontal swipe gesture to reveal quick action buttons.
/// Swipe left reveals: favorite toggle, hide model.
struct SwipeableModelCardView: View {
    let model: Model
    let isFavorite: Bool
    let onFavoriteToggle: () -> Void
    let onHide: () -> Void
    var isOwned: Bool = false
    var swipeThreshold: CGFloat = 0.3
    var heroNamespace: Namespace.ID?

    @State private var dragOffset: CGFloat = 0
    @State private var hasTriggeredHaptic: Bool = false

    private let actionAreaWidth: CGFloat = 120

    var body: some View {
        ZStack(alignment: .trailing) {
            actionButtons

            ModelCardView(model: model, isOwned: isOwned)
                .applyHeroSource(id: model.id, in: heroNamespace)
                .offset(x: dragOffset)
                .simultaneousGesture(swipeGesture)
        }
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
    }

    private var actionButtons: some View {
        HStack(spacing: Spacing.sm) {
            Spacer()
            Button {
                onFavoriteToggle()
                resetOffset()
            } label: {
                Image(systemName: isFavorite ? "heart.fill" : "heart")
                    .font(.civitIconMedium)
                    .foregroundColor(isFavorite ? .civitError : .civitOnError)
                    .frame(width: 44, height: 44)
                    .accessibilityLabel(isFavorite ? "Remove from favorites" : "Add to favorites")
            }
            Button {
                onHide()
                resetOffset()
            } label: {
                Image(systemName: "eye.slash.fill")
                    .font(.civitIconMedium)
                    .foregroundColor(.civitOnError)
                    .frame(width: 44, height: 44)
                    .accessibilityLabel("Hide model")
            }
        }
        .padding(.trailing, Spacing.sm)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.civitError.opacity(0.85))
    }

    private var swipeGesture: some Gesture {
        DragGesture(minimumDistance: 20, coordinateSpace: .local)
            .onChanged { value in
                // Only engage if primarily horizontal (left swipe)
                guard abs(value.translation.width) > abs(value.translation.height) else { return }

                let translation = value.translation.width
                let newOffset = min(0, max(-actionAreaWidth, translation))
                dragOffset = newOffset

                // Haptic feedback when crossing threshold
                let thresholdPx = actionAreaWidth * swipeThreshold
                if -newOffset >= thresholdPx && !hasTriggeredHaptic {
                    let impact = UIImpactFeedbackGenerator(style: .light)
                    impact.impactOccurred()
                    hasTriggeredHaptic = true
                } else if -newOffset < thresholdPx {
                    hasTriggeredHaptic = false
                }
            }
            .onEnded { value in
                let thresholdPx = actionAreaWidth * swipeThreshold
                withAnimation(MotionAnimation.spring) {
                    if abs(value.translation.width) > abs(value.translation.height)
                        && -value.translation.width >= thresholdPx {
                        dragOffset = -actionAreaWidth
                    } else {
                        dragOffset = 0
                    }
                }
                hasTriggeredHaptic = false
            }
    }

    private func resetOffset() {
        withAnimation(MotionAnimation.spring) {
            dragOffset = 0
        }
    }
}
