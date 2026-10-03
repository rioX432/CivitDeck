import SwiftUI
import ImageIO
import os
import Shared

private let imageLogger = Logger(subsystem: "com.riox432.civitdeck", category: "ImageLoading")

/// A drop-in replacement for `AsyncImage` that uses a shared URLSession
/// with a larger URLCache for better image caching performance.
struct CachedAsyncImage<Content: View>: View {
    let url: URL?
    /// Low-res variant expected to already be in the URLCache (e.g. the grid
    /// thumbnail). Shown instantly while the full image loads so list -> detail
    /// navigation never drops to a blank placeholder. Cache-only: never fetched.
    var placeholderURL: URL?
    var maxPixelSize: CGFloat = defaultMaxPixelSize
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

        imageLogger.debug("Loading image: \(url.absoluteString)")

        let request = URLRequest(
            url: url,
            cachePolicy: .returnCacheDataElseLoad
        )

        showCachedPlaceholderIfAvailable(finalRequest: request)

        do {
            let (data, response) = try await ImageURLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse {
                imageLogger.debug("HTTP \(http.statusCode) for \(url.absoluteString)")
            }
            // Use CGImageSource for memory-efficient downsampling.
            // Unlike UIImage(data:) + byPreparingThumbnail, this does NOT
            // decode the full-resolution image into memory first.
            guard let image = Self.downsampledImage(data: data, maxPixelSize: maxPixelSize) else {
                imageLogger.error("Downsampling failed for \(url.absoluteString), data size: \(data.count)")
                phase = .failure(ImageLoadingError.invalidData)
                return
            }
            withAnimation(.easeIn(duration: 0.2)) {
                phase = .success(Image(uiImage: image))
            }
        } catch {
            if !Task.isCancelled {
                imageLogger.error("Failed to load \(url.absoluteString): \(error)")
                phase = .failure(error)
            }
        }
    }

    /// Decodes the placeholder from the URLCache if present (no network) and
    /// shows it as an early success phase. Skipped when the final image itself
    /// is already cached, since it will render immediately anyway.
    private func showCachedPlaceholderIfAvailable(finalRequest: URLRequest) {
        guard let placeholderURL,
              let cache = ImageURLSession.shared.configuration.urlCache,
              cache.cachedResponse(for: finalRequest) == nil else { return }
        let placeholderRequest = URLRequest(
            url: placeholderURL,
            cachePolicy: .returnCacheDataElseLoad
        )
        guard let data = cache.cachedResponse(for: placeholderRequest)?.data,
              let image = Self.downsampledImage(data: data, maxPixelSize: maxPixelSize) else { return }
        phase = .success(Image(uiImage: image))
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
    static let memoryCacheCapacity = 20 * 1024 * 1024   // 20 MB
    static let diskCacheCapacity   = 200 * 1024 * 1024   // 200 MB

    /// Shared URLSession with a dedicated URLCache.
    static let shared: URLSession = {
        let cache = URLCache(
            memoryCapacity: memoryCacheCapacity,
            diskCapacity: diskCacheCapacity
        )
        let config = URLSessionConfiguration.default
        config.urlCache = cache
        config.requestCachePolicy = .returnCacheDataElseLoad
        return URLSession(configuration: config, delegate: ImageSessionDelegate(), delegateQueue: nil)
    }()

    private static let cachedComfyUIPinsKey = "imageCache.comfyUIPins"

    /// Clears the cache whenever a ComfyUI server's pinned certificate is confirmed, changed or
    /// cleared, so an image fetched under the old trust is not served again for the same URL.
    /// URLCache keys by URL only and cannot remove entries by host, so the whole cache goes;
    /// pin changes are rare user actions. In-flight tasks to a changed host:port may still be on
    /// the old trust, and open connections would serve later requests without a new trust
    /// challenge, so both go too. The pins the cache was filled under are persisted because the
    /// disk cache outlives the process.
    static func evictOnComfyUIPinChanges() async {
        let defaults = UserDefaults.standard
        for await pins in KoinHelper.shared.observeComfyUIImagePins() {
            let cachedPins = defaults.dictionary(forKey: cachedComfyUIPinsKey) as? [String: String] ?? [:]
            guard pins != cachedPins else { continue }
            let changedHostPorts = Set(pins.keys).union(cachedPins.keys).filter { pins[$0] != cachedPins[$0] }
            for task in await shared.allTasks {
                guard let url = task.originalRequest?.url, let host = url.host,
                      changedHostPorts.contains("\(host.lowercased()):\(url.port ?? 443)") else { continue }
                task.cancel()
            }
            shared.configuration.urlCache?.removeAllCachedResponses()
            await shared.reset()
            defaults.set(pins, forKey: cachedComfyUIPinsKey)
        }
    }
}

/// Answers server-trust challenges from a ComfyUI host:port with a confirmed pin through the
/// shared Kotlin evaluator. Every other challenge gets default handling, so CivitAI images keep
/// system trust.
private final class ImageSessionDelegate: NSObject, URLSessionDelegate {
    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        let space = challenge.protectionSpace
        guard space.authenticationMethod == NSURLAuthenticationMethodServerTrust,
              let evaluator = KoinHelper.shared.getComfyUIImageTrustEvaluator(
                  host: space.host,
                  port: Int32(clamping: space.port)
              )
        else {
            completionHandler(.performDefaultHandling, nil)
            return
        }
        let decision = evaluator.evaluate(challenge: challenge)
        let disposition = URLSession.AuthChallengeDisposition(rawValue: Int(decision.disposition))
            ?? .cancelAuthenticationChallenge
        completionHandler(disposition, decision.credential)
    }
}

// MARK: - Prefetching

enum ImagePrefetcher {
    /// Prefetch images into the URL cache for instant display later.
    static func prefetch(urls: [URL]) {
        for url in urls {
            let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
            // Skip if already cached
            if ImageURLSession.shared.configuration.urlCache?.cachedResponse(for: request) != nil {
                continue
            }
            Task.detached(priority: .utility) {
                _ = try? await ImageURLSession.shared.data(for: request)
            }
        }
    }
}

let defaultMaxPixelSize: CGFloat = 400

private enum ImageLoadingError: Error {
    case invalidData
}
