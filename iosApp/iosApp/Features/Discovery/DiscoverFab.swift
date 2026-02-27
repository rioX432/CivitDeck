import SwiftUI

struct DiscoveryDestination: Hashable {}

struct DiscoverFab: View {
    var visible: Bool = true

    var body: some View {
        NavigationLink(value: DiscoveryDestination()) {
            Image(systemName: "rectangle.stack")
                .font(.body)
                .foregroundColor(.civitPrimary)
                .frame(width: 44, height: 44)
                .background(Color.civitPrimaryContainer)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
        }
        .padding(.trailing, Spacing.lg)
        .opacity(visible ? 1 : 0)
        .animation(MotionAnimation.fast, value: visible)
    }
}
