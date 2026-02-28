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
                    case .empty:
                        Rectangle()
                            .fill(Color.civitSurfaceVariant)
                            .shimmer()
                    @unknown default:
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
                }
            }
        } else {
            container {
                Image(systemName: "photo")
                    .foregroundColor(.civitOnSurfaceVariant)
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
