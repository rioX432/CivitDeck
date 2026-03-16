import SwiftUI

struct DiscoveryDestination: Hashable {}

struct DiscoverFab: View {
    var visible: Bool = true
    @Environment(\.civitTheme) private var theme

    var body: some View {
        NavigationLink(value: DiscoveryDestination()) {
            Image(systemName: "rectangle.stack")
                .accessibilityLabel("Discover")
                .font(.body)
                .foregroundColor(theme.primary)
                .frame(width: 44, height: 44)
                .background(theme.primaryContainer)
                .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
                .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
        }
        .padding(.trailing, Spacing.lg)
        .opacity(visible ? 1 : 0)
        .animation(MotionAnimation.fast, value: visible)
    }
}
