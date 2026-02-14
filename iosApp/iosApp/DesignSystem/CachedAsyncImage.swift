import SwiftUI
import ImageIO
import os

private let imageLogger = Logger(subsystem: "com.riox432.civitdeck", category: "ImageLoading")

/// A drop-in replacement for `AsyncImage` that uses a shared URLSession
/// with a larger URLCache for better image caching performance.
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
            // Use CGImageSource for memory-efficient downsampling.
            // Unlike UIImage(data:) + byPreparingThumbnail, this does NOT
            // decode the full-resolution image into memory first.
            guard let image = Self.downsampledImage(data: data, maxPixelSize: 400) else {
                phase = .failure(ImageLoadingError.invalidData)
                return
            }
            withAnimation(.easeIn(duration: 0.2)) {
                phase = .success(Image(uiImage: image))
            }
        } catch {
            if !Task.isCancelled {
                phase = .failure(error)
            }
        }
    }

    private static func downsampledImage(data: Data, maxPixelSize: CGFloat) -> UIImage? {
        let sourceOptions: [CFString: Any] = [kCGImageSourceShouldCache: false]
        guard let source = CGImageSourceCreateWithData(data as CFData, sourceOptions as CFDictionary) else {
            return nil
        }
        let downsampleOptions: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
            kCGImageSourceShouldCacheImmediately: true,
        ]
        guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, downsampleOptions as CFDictionary) else {
            return nil
        }
        return UIImage(cgImage: cgImage)
    }
}

// MARK: - Shared URLSession

enum ImageURLSession {
    /// Shared URLSession with a 20MB memory / 200MB disk cache.
    static let shared: URLSession = {
        let cache = URLCache(
            memoryCapacity: 20 * 1024 * 1024,
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
