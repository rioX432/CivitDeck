import SwiftUI

/// A selectable chip button used in filter rows.
struct ChipButton: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: { HapticFeedback.selection.trigger(); action() }) {
            Text(label)
                .font(.civitLabelMedium)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, Spacing.md)
                .padding(.vertical, 6)
                .background(isSelected ? Color.civitPrimary.opacity(0.2) : Color.civitSurfaceVariant)
                .foregroundColor(isSelected ? .civitPrimary : .civitOnSurface)
                .clipShape(Capsule())
                .animation(MotionAnimation.spring, value: isSelected)
        }
    }
}
