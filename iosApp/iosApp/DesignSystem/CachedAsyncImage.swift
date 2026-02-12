import SwiftUI

/// A drop-in replacement for `AsyncImage` that uses a shared URLSession
/// with a larger URLCache for better image caching performance.
///
/// Usage is identical to `AsyncImage`:
/// ```
/// CachedAsyncImage(url: imageURL) { phase in
///     switch phase { ... }
/// }
/// ```
struct CachedAsyncImage<Content: View>: View {
    let url: URL?
    @ViewBuilder let content: (AsyncImagePhase) -> Content

    @State private var phase: AsyncImagePhase = .empty

    var body: some View {
        content(phase)
            .task(id: url) {
                await loadImage()
            }
    }

    private func loadImage() async {
        guard let url else {
            phase = .empty
            return
        }

        let request = URLRequest(
            url: url,
            cachePolicy: .returnCacheDataElseLoad
        )

        do {
            let (data, _) = try await ImageURLSession.shared.data(for: request)
            guard let uiImage = UIImage(data: data) else {
                phase = .failure(ImageLoadingError.invalidData)
                return
            }
            withAnimation(.easeIn(duration: 0.2)) {
                phase = .success(Image(uiImage: uiImage))
            }
        } catch {
            if !Task.isCancelled {
                phase = .failure(error)
            }
        }
    }
}

// MARK: - Shared URLSession

enum ImageURLSession {
    /// Shared URLSession with a 50MB memory / 200MB disk cache.
    static let shared: URLSession = {
        let cache = URLCache(
            memoryCapacity: 50 * 1024 * 1024,
            diskCapacity: 200 * 1024 * 1024
        )
        let config = URLSessionConfiguration.default
        config.urlCache = cache
        config.requestCachePolicy = .returnCacheDataElseLoad
        return URLSession(configuration: config)
    }()
}

private enum ImageLoadingError: Error {
    case invalidData
}
