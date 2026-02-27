import SwiftUI

/// Wraps content with a blur overlay based on a blur radius.
/// Supports tap-to-reveal: tapping the blurred area temporarily removes the blur.
struct NsfwBlurView<Content: View>: View {
    let blurRadius: CGFloat
    @ViewBuilder let content: () -> Content
    @State private var isRevealed: Bool = false

    var body: some View {
        ZStack {
            content()
                .blur(radius: isRevealed ? 0 : blurRadius)

            if !isRevealed && blurRadius > 0 {
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation(MotionAnimation.standard) {
                            isRevealed = true
                        }
                    }
                    .overlay {
                        Text("Tap to reveal")
                            .font(.civitLabelMedium)
                            .foregroundColor(.civitOnSurface.opacity(0.7))
                    }
            }
        }
    }
}
