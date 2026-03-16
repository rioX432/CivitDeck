import SwiftUI

/// Standard image view with shimmer placeholder and error fallback.
/// Wraps CachedAsyncImage with the consistent loading/error/success pattern.
struct CivitAsyncImageView: View {
    let imageUrl: String?
    var contentMode: ContentMode = .fill
    var aspectRatio: CGFloat?

    var body: some View {
        if let urlString = imageUrl, let url = URL(string: urlString) {
            container {
                CachedAsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable()
                            .aspectRatio(contentMode: contentMode)
                            .transition(.opacity)
                    case .failure:
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                            .accessibilityHidden(true)
                    case .empty:
                        Rectangle()
                            .fill(Color.civitSurfaceVariant)
                            .shimmer()
                    @unknown default:
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                            .accessibilityHidden(true)
                    }
                }
            }
        } else {
            container {
                Image(systemName: "photo")
                    .foregroundColor(.civitOnSurfaceVariant)
                    .accessibilityHidden(true)
            }
        }
    }

    @ViewBuilder
    private func container<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        let base = Color.civitSurfaceVariant
        if let ratio = aspectRatio {
            base.aspectRatio(ratio, contentMode: .fit)
                .overlay { content() }
                .clipped()
        } else {
            base.overlay { content() }
                .clipped()
        }
    }
}
