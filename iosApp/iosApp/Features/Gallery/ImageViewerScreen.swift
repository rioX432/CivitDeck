import SwiftUI
import Shared
import Photos

struct ImageViewerScreen: View {
    let images: [CivitImage]
    @Binding var selectedIndex: Int?
    var onSavePrompt: (ImageGenerationMeta, String) -> Void = { _, _ in }

    @State private var showMetadata = false
    @State private var controlsVisible = true
    @State private var showShareSheet = false

    // Swipe-to-dismiss state
    @State private var dragOffset: CGFloat = 0

    // Download toast
    @State private var toastMessage: String?

    var body: some View {
        if let index = selectedIndex {
            ZStack {
                // Layer 1: Background (stays in place, fades)
                Color.black
                    .opacity(backgroundOpacity)
                    .ignoresSafeArea()

                // Layer 2: Image/Video pager
                TabView(selection: Binding(
                    get: { index },
                    set: { selectedIndex = $0 }
                )) {
                    ForEach(Array(images.enumerated()), id: \.element.id) { i, image in
                        if image.contentType == .video, let videoUrl = URL(string: image.url) {
                            if image.url.hasSuffix(".webm") {
                                VStack {
                                    Spacer()
                                    SwiftUI.Image(systemName: "play.slash")
                                        .font(.largeTitle)
                                        .foregroundColor(.civitInverseOnSurface)
                                    Text("WebM format not supported on iOS")
                                        .font(.civitBodyMedium)
                                        .foregroundColor(.civitInverseOnSurface)
                                        .padding(.top, Spacing.sm)
                                    Spacer()
                                }
                                .tag(i)
                            } else {
                                VideoPlayerView(url: videoUrl, autoPlay: i == index)
                                    .ignoresSafeArea()
                                    .tag(i)
                            }
                        } else {
                            ZoomableImageView(
                                url: image.url,
                                onFocusModeChanged: { isFocusMode in
                                    controlsVisible = !isFocusMode
                                },
                                onDismiss: {
                                    selectedIndex = nil
                                },
                                onDragYChanged: { dragOffset = $0 },
                                pageIndex: i,
                                currentPageIndex: index
                            )
                            .ignoresSafeArea()
                            .tag(i)
                        }
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))

                // Layer 3: Controls (stays in place)
                if controlsVisible && dragOffset == 0 {
                    viewerControls(currentIndex: index)
                        .transition(.opacity)
                }

                if let message = toastMessage {
                    toastView(message: message)
                }
            }
            .animation(MotionAnimation.fast, value: controlsVisible)
            .sheet(isPresented: $showMetadata) {
                if let meta = images[safe: index]?.meta {
                    MetadataSheet(meta: meta) {
                        onSavePrompt(meta, images[safe: index]?.url ?? "")
                    }
                    .presentationDetents([.medium, .large])
                }
            }
            .sheet(isPresented: $showShareSheet) {
                if let image = images[safe: index] {
                    let text = Self.formatShareText(imageUrl: image.url, meta: image.meta)
                    ShareSheet(items: [text])
                }
            }
        }
    }

    // MARK: - Background Opacity

    private var backgroundOpacity: Double {
        let progress = abs(dragOffset) / dismissThreshold
        return Double(max(1.0 - progress / bgFadeFactor, 0.0))
    }

    // MARK: - Controls

    private func viewerControls(currentIndex: Int) -> some View {
        VStack {
            HStack {
                ControlCircleButton(systemName: "xmark", label: "Close") {
                    selectedIndex = nil
                }
                Spacer()
            }
            .padding(Spacing.lg)

            Spacer()

            HStack {
                Spacer()
                ControlCircleButton(systemName: "arrow.down.to.line", label: "Download") {
                    downloadImage(at: currentIndex)
                }
                ControlCircleButton(systemName: "square.and.arrow.up", label: "Share") {
                    showShareSheet = true
                }
                if images[safe: currentIndex]?.meta != nil {
                    ControlCircleButton(systemName: "info.circle", label: "Metadata") {
                        showMetadata = true
                    }
                }
            }
            .padding(Spacing.lg)
        }
    }

    // MARK: - Download

    private func downloadImage(at index: Int) {
        guard let image = images[safe: index],
              let url = URL(string: image.url) else {
            showToast("Download failed")
            return
        }

        let isVideo = image.contentType == .video

        Task {
            do {
                let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
                let (data, _) = try await ImageURLSession.shared.data(for: request)

                if isVideo {
                    try await saveVideoToPhotoLibrary(data: data, url: url)
                } else {
                    guard let uiImage = UIImage(data: data) else {
                        showToast("Download failed")
                        return
                    }
                    try await saveImageToPhotoLibrary(image: uiImage)
                }
                showToast("Saved to Photos")
            } catch {
                showToast("Download failed")
            }
        }
    }

    private func saveImageToPhotoLibrary(image: UIImage) async throws {
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else {
            throw DownloadError.permissionDenied
        }
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.creationRequestForAsset(from: image)
        }
    }

    private func saveVideoToPhotoLibrary(data: Data, url: URL) async throws {
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else {
            throw DownloadError.permissionDenied
        }
        let ext = url.pathExtension.isEmpty ? "mp4" : url.pathExtension
        let tempURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension(ext)
        try data.write(to: tempURL)
        defer { try? FileManager.default.removeItem(at: tempURL) }
        try await PHPhotoLibrary.shared().performChanges {
            let request = PHAssetCreationRequest.forAsset()
            request.addResource(with: .video, fileURL: tempURL, options: nil)
        }
    }

    // MARK: - Toast

    private func showToast(_ message: String) {
        withAnimation(MotionAnimation.fast) {
            toastMessage = message
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation(MotionAnimation.fast) {
                toastMessage = nil
            }
        }
    }

    private func toastView(message: String) -> some View {
        VStack {
            Spacer()
            Text(message)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.civitInverseOnSurface)
                .padding(.horizontal, Spacing.lg)
                .padding(.vertical, Spacing.smPlus)
                .background(.ultraThinMaterial, in: Capsule())
                .padding(.bottom, Spacing.floatingOffset)
        }
        .transition(.opacity)
    }

    // MARK: - Share Text

    static func formatShareText(imageUrl: String, meta: ImageGenerationMeta?) -> String {
        var text = imageUrl + "\n"
        if let meta {
            text += "\n"
            if let prompt = meta.prompt {
                text += "Prompt: \(prompt)\n"
            }
            if let negative = meta.negativePrompt {
                text += "Negative: \(negative)\n"
            }
            var params: [String] = []
            if let model = meta.model { params.append("Model: \(model)") }
            if let steps = meta.steps { params.append("Steps: \(steps)") }
            if let cfg = meta.cfgScale { params.append("CFG: \(cfg)") }
            if let sampler = meta.sampler { params.append("Sampler: \(sampler)") }
            if !params.isEmpty {
                text += params.joined(separator: " | ") + "\n"
            }
        }
        text += "\nShared via CivitDeck"
        return text
    }
}

// MARK: - Constants

private let dismissThreshold: CGFloat = 100
private let bgFadeFactor: CGFloat = 4

// MARK: - Errors

private enum DownloadError: Error {
    case permissionDenied
}

// MARK: - Control Button

private struct ControlCircleButton: View {
    let systemName: String
    var label: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            SwiftUI.Image(systemName: systemName)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.civitInverseOnSurface)
                .padding(Spacing.smPlus)
                .background(.ultraThinMaterial, in: Circle())
        }
        .accessibilityLabel(label ?? systemName)
    }
}

// MARK: - Share Sheet

private struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Safe Array Access

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
