import SwiftUI

/// A selectable chip button used in filter rows.
struct ChipButton: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void
    @Environment(\.civitTheme) private var theme

    var body: some View {
        Button(action: { HapticFeedback.selection.trigger(); action() }) {
            Text(label)
                .font(.civitLabelMedium)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, Spacing.md)
                .padding(.vertical, Spacing.xsPlus)
                .background(isSelected ? theme.primary.opacity(0.2) : Color.civitSurfaceVariant)
                .foregroundColor(isSelected ? theme.primary : .civitOnSurface)
                .clipShape(Capsule())
                .animation(MotionAnimation.spring, value: isSelected)
        }
    }
}
