import SwiftUI

struct ErrorStateView: View {
    let message: String
    var onRetry: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: CivitDeckSpacing.lg) {
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
