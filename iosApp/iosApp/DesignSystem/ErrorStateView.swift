import SwiftUI

struct ErrorStateView: View {
    let message: String
    var onRetry: (() -> Void)?

    var body: some View {
        VStack(spacing: Spacing.lg) {
            Text(message)
                .foregroundColor(.civitError)
                .multilineTextAlignment(.center)
            if let onRetry {
                Button("Retry", action: onRetry)
                    .buttonStyle(.bordered)
            }
        }
        .padding()
    }
}
