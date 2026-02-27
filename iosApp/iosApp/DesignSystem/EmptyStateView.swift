import SwiftUI

struct EmptyStateView: View {
    let icon: String
    let title: String
    var subtitle: String?
    var actionLabel: String?
    var onAction: (() -> Void)?

    var body: some View {
        VStack(spacing: Spacing.sm) {
            Image(systemName: icon)
                .font(.largeTitle)
                .foregroundColor(.civitOnSurfaceVariant)
            Text(title)
                .font(.civitTitleMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            if let subtitle {
                Text(subtitle)
                    .font(.civitBodyMedium)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .multilineTextAlignment(.center)
            }
            if let actionLabel, let onAction {
                Button(actionLabel, action: onAction)
                    .buttonStyle(.bordered)
                    .padding(.top, Spacing.sm)
            }
        }
        .padding()
    }
}
