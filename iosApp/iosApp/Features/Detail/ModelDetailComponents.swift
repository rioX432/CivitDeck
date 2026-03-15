import AVKit
import Photos
import SwiftUI
import Shared

// MARK: - Carousel Viewer
// Follows the same proven pattern as ImageViewerScreen (Gallery):
// - Pass all gesture callbacks to ZoomableImageView
// - Conditional control visibility via controlsVisible + dragOffset
// - Drag-to-dismiss with background opacity fade
// - Direct selectedIndex = nil for dismiss (not @Environment(\.dismiss))

struct CarouselViewer: View {
    let images: [ModelImage]
    @Binding var selectedIndex: Int?

    @State private var controlsVisible = true
    @State private var dragOffset: CGFloat = 0
    @State private var toastMessage: String?
    @State private var showShareSheet = false

    var body: some View {
        if let index = selectedIndex {
            ZStack {
                Color.civitScrim
                    .opacity(backgroundOpacity)
                    .ignoresSafeArea()

                TabView(selection: Binding(
                    get: { index },
                    set: { selectedIndex = $0 }
                )) {
                    ForEach(Array(images.enumerated()), id: \.offset) { i, image in
                        if image.contentType == .video, let videoUrl = URL(string: image.url) {
                            VideoPlayerView(url: videoUrl, autoPlay: i == index)
                                .ignoresSafeArea()
                                .tag(i)
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

                if controlsVisible && dragOffset == 0 {
                    viewerControls(currentIndex: index)
                        .transition(.opacity)
                }

                if let message = toastMessage {
                    toastView(message: message)
                }
            }
            .animation(MotionAnimation.fast, value: controlsVisible)
            .sheet(isPresented: $showShareSheet) {
                if let image = images[safe: index] {
                    ShareSheet(items: [image.url])
                }
            }
        }
    }

    private var backgroundOpacity: Double {
        let progress = abs(dragOffset) / 100
        return Double(max(1.0 - progress / 4.0, 0.0))
    }

    private func viewerControls(currentIndex: Int) -> some View {
        VStack {
            HStack {
                ViewerCircleButton(systemName: "xmark", label: "Close") {
                    selectedIndex = nil
                }
                Spacer()
            }
            .padding(Spacing.lg)

            Spacer()

            HStack {
                Spacer()
                ViewerCircleButton(systemName: "arrow.down.to.line", label: "Download") {
                    downloadImage(at: currentIndex)
                }
                ViewerCircleButton(systemName: "square.and.arrow.up", label: "Share") {
                    showShareSheet = true
                }
            }
            .padding(Spacing.lg)
        }
    }

    private func downloadImage(at index: Int) {
        guard let image = images[safe: index],
              let url = URL(string: image.url) else {
            showToast("Download failed")
            return
        }
        Task {
            do {
                let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
                let (data, _) = try await ImageURLSession.shared.data(for: request)
                if image.contentType == .video {
                    try await saveVideoToLibrary(data: data, url: url)
                } else {
                    guard let uiImage = UIImage(data: data) else {
                        showToast("Download failed")
                        return
                    }
                    try await saveImageToLibrary(image: uiImage)
                }
                showToast("Saved to Photos")
            } catch {
                showToast("Download failed")
            }
        }
    }

    private func showToast(_ message: String) {
        withAnimation(MotionAnimation.fast) { toastMessage = message }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation(MotionAnimation.fast) { toastMessage = nil }
        }
    }

    private func toastView(message: String) -> some View {
        VStack {
            Spacer()
            Text(message)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.white)
                .padding(.horizontal, Spacing.lg)
                .padding(.vertical, Spacing.smPlus)
                .background(.ultraThinMaterial, in: Capsule())
                .padding(.bottom, Spacing.floatingOffset)
        }
        .transition(.opacity)
    }
}

// MARK: - Image Grid Sheet

struct ImageGridSheet: View {
    let images: [ModelImage]
    let onDismiss: () -> Void
    let onImageSelected: (Int) -> Void
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(
                    columns: AdaptiveGrid.columns(sizeClass: sizeClass),
                    spacing: Spacing.sm
                ) {
                    ForEach(Array(images.enumerated()), id: \.element.url) { index, image in
                        gridImageCell(image: image, index: index)
                    }
                }
                .padding(Spacing.sm)
            }
            .navigationTitle("Version Images (\(images.count))")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { onDismiss() }
                }
            }
        }
    }

    private func gridImageCell(image: ModelImage, index: Int) -> some View {
        let aspectRatio = (image.width > 0 && image.height > 0)
            ? CGFloat(image.width) / CGFloat(image.height) : 1.0
        return Button {
            onDismiss()
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(300))
                onImageSelected(index)
            }
        } label: {
            ZStack {
                CachedAsyncImage(url: URL(string: image.url)) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFill().transition(.opacity)
                    case .failure:
                        Rectangle().fill(Color.civitSurfaceVariant)
                            .overlay { SwiftUI.Image(systemName: "photo")
                                .foregroundColor(.civitOnSurfaceVariant)
                                .accessibilityHidden(true) }
                    case .empty:
                        Rectangle().fill(Color.civitSurfaceVariant).shimmer()
                    @unknown default:
                        EmptyView()
                    }
                }
                .aspectRatio(aspectRatio, contentMode: .fill)
                .clipped()

                if image.contentType == .video {
                    SwiftUI.Image(systemName: "play.circle.fill")
                        .font(.civitIconLarge)
                        .foregroundColor(.white)
                        .accessibilityHidden(true)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Grid Image Viewer (same pattern as CarouselViewer)

struct GridImageViewer: View {
    let images: [ModelImage]
    @Binding var selectedIndex: Int?

    @State private var controlsVisible = true
    @State private var dragOffset: CGFloat = 0
    @State private var toastMessage: String?
    @State private var showShareSheet = false

    var body: some View {
        if let index = selectedIndex {
            ZStack {
                Color.civitScrim
                    .opacity(backgroundOpacity)
                    .ignoresSafeArea()

                TabView(selection: Binding(
                    get: { index },
                    set: { selectedIndex = $0 }
                )) {
                    ForEach(Array(images.enumerated()), id: \.offset) { i, image in
                        if image.contentType == .video, let videoUrl = URL(string: image.url) {
                            VideoPlayerView(url: videoUrl, autoPlay: i == index)
                                .ignoresSafeArea()
                                .tag(i)
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

                if controlsVisible && dragOffset == 0 {
                    viewerControls(currentIndex: index)
                        .transition(.opacity)
                }

                if let message = toastMessage {
                    VStack {
                        Spacer()
                        Text(message)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.white)
                            .padding(.horizontal, Spacing.lg)
                            .padding(.vertical, Spacing.smPlus)
                            .background(.ultraThinMaterial, in: Capsule())
                            .padding(.bottom, Spacing.floatingOffset)
                    }
                    .transition(.opacity)
                }
            }
            .animation(MotionAnimation.fast, value: controlsVisible)
            .sheet(isPresented: $showShareSheet) {
                if let image = images[safe: index] {
                    ShareSheet(items: [image.url])
                }
            }
        }
    }

    private var backgroundOpacity: Double {
        let progress = abs(dragOffset) / 100
        return Double(max(1.0 - progress / 4.0, 0.0))
    }

    private func viewerControls(currentIndex: Int) -> some View {
        VStack {
            HStack {
                ViewerCircleButton(systemName: "xmark", label: "Close") {
                    selectedIndex = nil
                }
                Spacer()
            }
            .padding(Spacing.lg)

            Spacer()

            HStack {
                Spacer()
                ViewerCircleButton(systemName: "arrow.down.to.line", label: "Download") {
                    downloadImage(at: currentIndex)
                }
                ViewerCircleButton(systemName: "square.and.arrow.up", label: "Share") {
                    showShareSheet = true
                }
            }
            .padding(Spacing.lg)
        }
    }

    private func downloadImage(at index: Int) {
        guard let image = images[safe: index],
              let url = URL(string: image.url) else {
            showToast("Download failed")
            return
        }
        Task {
            do {
                let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
                let (data, _) = try await ImageURLSession.shared.data(for: request)
                if image.contentType == .video {
                    try await saveVideoToLibrary(data: data, url: url)
                } else {
                    guard let uiImage = UIImage(data: data) else {
                        showToast("Download failed")
                        return
                    }
                    try await saveImageToLibrary(image: uiImage)
                }
                showToast("Saved to Photos")
            } catch {
                showToast("Download failed")
            }
        }
    }

    private func showToast(_ message: String) {
        withAnimation(MotionAnimation.fast) { toastMessage = message }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation(MotionAnimation.fast) { toastMessage = nil }
        }
    }
}

// MARK: - Shared Viewer Helpers

struct ViewerCircleButton: View {
    let systemName: String
    var label: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            SwiftUI.Image(systemName: systemName)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .padding(Spacing.smPlus)
                .background(.ultraThinMaterial, in: Circle())
        }
        .accessibilityLabel(label ?? systemName)
    }
}

private func saveImageToLibrary(image: UIImage) async throws {
    let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
    guard status == .authorized || status == .limited else { return }
    try await PHPhotoLibrary.shared().performChanges {
        PHAssetChangeRequest.creationRequestForAsset(from: image)
    }
}

private func saveVideoToLibrary(data: Data, url: URL) async throws {
    let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
    guard status == .authorized || status == .limited else { return }
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

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

// MARK: - Helpers

func htmlToPlainText(_ html: String) -> String {
    guard let data = html.data(using: .utf8),
          let attributedString = try? NSAttributedString(
            data: data,
            options: [
                .documentType: NSAttributedString.DocumentType.html,
                .characterEncoding: String.Encoding.utf8.rawValue,
            ],
            documentAttributes: nil
          ) else {
        return html
    }
    return attributedString.string
}

// MARK: - Wrapping HStack for Tags

struct WrappingHStack: View {
    let tags: [String]

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 80), spacing: Spacing.sm)],
            alignment: .leading,
            spacing: Spacing.sm
        ) {
            ForEach(tags, id: \.self) { tag in
                Text(tag)
                    .font(.civitLabelMedium)
                    .padding(.horizontal, Spacing.smPlus)
                    .padding(.vertical, Spacing.xs)
                    .background(Color.civitSurfaceVariant)
                    .clipShape(Capsule())
            }
        }
    }
}
