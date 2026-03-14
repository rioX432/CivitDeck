// NOTE: This file belongs to the CivitDeckWidget extension target.
// Create the target in Xcode: File > New > Target > Widget Extension,
// then add this file and CivitDeckWidgetBundle.swift to that target.
// App group entitlement "group.com.riox432.civitdeck" must be added to both targets.

import WidgetKit
import SwiftUI
import ImageIO

// MARK: - Data model

struct TrendingModelEntry: TimelineEntry {
    let date: Date
    let modelId: Int64?
    let modelName: String
    let thumbnailURL: URL?
}

// MARK: - Provider

struct TrendingModelProvider: TimelineProvider {
    func placeholder(in context: Context) -> TrendingModelEntry {
        TrendingModelEntry(date: .now, modelId: nil, modelName: "Trending Model", thumbnailURL: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (TrendingModelEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TrendingModelEntry>) -> Void) {
        let entry = loadEntry()
        let nextUpdate = Calendar.current.date(byAdding: .hour, value: 1, to: .now) ?? .now
        completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
    }

    private func loadEntry() -> TrendingModelEntry {
        let defaults = UserDefaults(suiteName: WidgetConstants.appGroupSuite)
        let idValue = defaults?.object(forKey: WidgetConstants.keyModelId)
        let modelId = idValue.flatMap { $0 as? Int64 ?? ($0 as? NSNumber).map { Int64(truncating: $0) } }
        let name = defaults?.string(forKey: WidgetConstants.keyModelName) ?? "Trending Today"
        let thumbStr = defaults?.string(forKey: WidgetConstants.keyModelThumb)
        let thumbURL = thumbStr.flatMap { URL(string: $0) }
        return TrendingModelEntry(date: .now, modelId: modelId, modelName: name, thumbnailURL: thumbURL)
    }
}

// MARK: - Views

struct TrendingModelWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: TrendingModelEntry

    var body: some View {
        switch family {
        case .systemMedium:
            mediumView
        default:
            smallView
        }
    }

    private var smallView: some View {
        VStack(alignment: .leading, spacing: 0) {
            thumbnailView
                .frame(maxWidth: .infinity)
                .frame(height: 80)
                .clipped()
            Text(entry.modelName)
                .font(.caption)
                .fontWeight(.medium)
                .lineLimit(2)
                .padding(.horizontal, 8)
                .padding(.vertical, 6)
        }
        .widgetURL(deepLinkURL)
    }

    private var mediumView: some View {
        HStack(spacing: 0) {
            thumbnailView
                .frame(width: 100)
                .clipped()
            VStack(alignment: .leading, spacing: 4) {
                Text("Trending Today")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(entry.modelName)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(3)
                Spacer()
                if let _ = entry.modelId {
                    Label("View model", systemImage: "arrow.right.circle")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(12)
            Spacer()
        }
        .widgetURL(deepLinkURL)
    }

    @ViewBuilder
    private var thumbnailView: some View {
        if let url = entry.thumbnailURL {
            WidgetCachedImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    placeholderView
                default:
                    placeholderView
                }
            }
        } else {
            placeholderView
        }
    }

    private var placeholderView: some View {
        ZStack {
            Color.gray.opacity(0.2)
            Image(systemName: "photo")
                .foregroundStyle(.tertiary)
        }
    }

    private var deepLinkURL: URL? {
        guard let id = entry.modelId else { return nil }
        return URL(string: "civitdeck://model/\(id)")
    }
}

// MARK: - Widget configuration

struct CivitDeckTrendingWidget: Widget {
    let kind = "CivitDeckTrendingWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: TrendingModelProvider()) { entry in
            TrendingModelWidgetView(entry: entry)
                .containerBackground(.background, for: .widget)
        }
        .configurationDisplayName("Trending Model")
        .description("Shows today's trending AI model from CivitAI.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Cached image loader for widgets

/// Drop-in replacement for AsyncImage with disk caching via URLSession.
/// Mirrors the app's CachedAsyncImage but self-contained for the widget target.
private struct WidgetCachedImage<Content: View>: View {
    let url: URL?
    @ViewBuilder let content: (AsyncImagePhase) -> Content
    @State private var phase: AsyncImagePhase = .empty

    var body: some View {
        content(phase)
            .task(id: url) { await loadImage() }
    }

    private func loadImage() async {
        guard let url else {
            phase = .empty
            return
        }
        let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
        do {
            let (data, _) = try await WidgetImageSession.shared.data(for: request)
            guard let image = downsampledImage(data: data) else {
                phase = .failure(URLError(.cannotDecodeContentData))
                return
            }
            phase = .success(Image(uiImage: image))
        } catch {
            if !Task.isCancelled { phase = .failure(error) }
        }
    }

    private func downsampledImage(data: Data) -> UIImage? {
        let opts: [CFString: Any] = [kCGImageSourceShouldCache: false]
        guard let src = CGImageSourceCreateWithData(data as CFData, opts as CFDictionary) else { return nil }
        let thumbOpts: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: 300,
            kCGImageSourceShouldCacheImmediately: true,
        ]
        guard let cg = CGImageSourceCreateThumbnailAtIndex(src, 0, thumbOpts as CFDictionary) else { return nil }
        return UIImage(cgImage: cg)
    }
}

private enum WidgetImageSession {
    static let shared: URLSession = {
        let cache = URLCache(memoryCapacity: 5 * 1024 * 1024, diskCapacity: 50 * 1024 * 1024)
        let config = URLSessionConfiguration.default
        config.urlCache = cache
        config.requestCachePolicy = .returnCacheDataElseLoad
        return URLSession(configuration: config)
    }()
}
