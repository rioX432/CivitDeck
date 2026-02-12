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

                // Layer 2: Image pager
                TabView(selection: Binding(
                    get: { index },
                    set: { selectedIndex = $0 }
                )) {
                    ForEach(Array(images.enumerated()), id: \.element.id) { i, image in
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
                ControlCircleButton(systemName: "xmark") {
                    selectedIndex = nil
                }
                Spacer()
            }
            .padding(16)

            Spacer()

            HStack {
                Spacer()
                ControlCircleButton(systemName: "arrow.down.to.line") {
                    downloadImage(at: currentIndex)
                }
                ControlCircleButton(systemName: "square.and.arrow.up") {
                    showShareSheet = true
                }
                if images[safe: currentIndex]?.meta != nil {
                    ControlCircleButton(systemName: "info.circle") {
                        showMetadata = true
                    }
                }
            }
            .padding(16)
        }
    }

    // MARK: - Download

    private func downloadImage(at index: Int) {
        guard let urlString = images[safe: index]?.url,
              let url = URL(string: urlString) else {
            showToast("Download failed")
            return
        }

        Task {
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                guard let image = UIImage(data: data) else {
                    showToast("Download failed")
                    return
                }
                try await saveToPhotoLibrary(image: image)
                showToast("Saved to Photos")
            } catch {
                showToast("Download failed")
            }
        }
    }

    private func saveToPhotoLibrary(image: UIImage) async throws {
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else {
            throw DownloadError.permissionDenied
        }
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.creationRequestForAsset(from: image)
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
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(.ultraThinMaterial, in: Capsule())
                .padding(.bottom, 80)
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
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            SwiftUI.Image(systemName: systemName)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .padding(10)
                .background(.ultraThinMaterial, in: Circle())
        }
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

// MARK: - Zoomable Image (UIScrollView-based)

struct ZoomableImageView: UIViewControllerRepresentable {
    let url: String
    var onFocusModeChanged: ((Bool) -> Void)?
    var onDismiss: (() -> Void)?
    var onDragYChanged: ((CGFloat) -> Void)?
    var pageIndex: Int?
    var currentPageIndex: Int?

    func makeUIViewController(context: Context) -> ZoomableImageViewController {
        let viewController = ZoomableImageViewController()
        viewController.onFocusModeChanged = onFocusModeChanged
        viewController.onDismiss = onDismiss
        viewController.onDragYChanged = onDragYChanged
        viewController.loadImage(from: url)
        return viewController
    }

    func updateUIViewController(_ viewController: ZoomableImageViewController, context: Context) {
        viewController.onFocusModeChanged = onFocusModeChanged
        viewController.onDismiss = onDismiss
        viewController.onDragYChanged = onDragYChanged

        if let pageIndex, let currentPageIndex, currentPageIndex != pageIndex {
            viewController.resetZoom()
        }
    }
}

// MARK: - ZoomableImageViewController

final class ZoomableImageViewController: UIViewController, UIScrollViewDelegate, UIGestureRecognizerDelegate {
    private let scrollView = UIScrollView()
    private let imageView = UIImageView()
    private let doubleTapRecognizer = UITapGestureRecognizer()
    private let singleTapRecognizer = UITapGestureRecognizer()
    private let panRecognizer = UIPanGestureRecognizer()
    private let spinner = UIActivityIndicatorView(style: .medium)

    private let dragDismissThreshold: CGFloat = 100
    private var isZoomedIn = false
    private var isFocusMode = false
    private var isDismissDragging = false

    var onFocusModeChanged: ((Bool) -> Void)?
    var onDismiss: (() -> Void)?
    var onDragYChanged: ((CGFloat) -> Void)?

    override func viewDidLoad() {
        super.viewDidLoad()

        scrollView.delegate = self
        scrollView.minimumZoomScale = 1.0
        scrollView.maximumZoomScale = 3.0
        scrollView.showsVerticalScrollIndicator = false
        scrollView.showsHorizontalScrollIndicator = false

        view.addSubview(scrollView)
        scrollView.addSubview(imageView)

        doubleTapRecognizer.numberOfTapsRequired = 2
        doubleTapRecognizer.addTarget(self, action: #selector(onDoubleTap(_:)))
        scrollView.addGestureRecognizer(doubleTapRecognizer)

        singleTapRecognizer.numberOfTapsRequired = 1
        singleTapRecognizer.addTarget(self, action: #selector(onSingleTap(_:)))
        singleTapRecognizer.require(toFail: doubleTapRecognizer)
        scrollView.addGestureRecognizer(singleTapRecognizer)

        panRecognizer.addTarget(self, action: #selector(onPan(_:)))
        panRecognizer.delegate = self
        scrollView.addGestureRecognizer(panRecognizer)

        // Loading spinner
        spinner.color = .white
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }

    func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        true
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        scrollView.frame = view.bounds

        guard let imageSize = imageView.image?.size else { return }

        let wRate = scrollView.frame.width / imageSize.width
        let hRate = scrollView.frame.height / imageSize.height
        let rate = min(wRate, hRate, 1)

        imageView.frame.size = CGSize(
            width: imageSize.width * rate,
            height: imageSize.height * rate
        )

        scrollView.contentSize = imageView.frame.size
        updateScrollInset()
    }

    func viewForZooming(in scrollView: UIScrollView) -> UIView? {
        imageView
    }

    func scrollViewDidZoom(_ scrollView: UIScrollView) {
        updateScrollInset()

        let currentlyZoomedIn = scrollView.zoomScale > scrollView.minimumZoomScale
        if currentlyZoomedIn != isZoomedIn {
            isZoomedIn = currentlyZoomedIn
            setFocusMode(isZoomedIn)
        }
    }

    private func setFocusMode(_ enabled: Bool) {
        guard isFocusMode != enabled else { return }
        isFocusMode = enabled
        onFocusModeChanged?(enabled)
    }

    private func updateScrollInset() {
        scrollView.contentInset = UIEdgeInsets(
            top: max((scrollView.frame.height - imageView.frame.height) / 2, 0),
            left: max((scrollView.frame.width - imageView.frame.width) / 2, 0),
            bottom: 0,
            right: 0
        )
    }

    func resetZoom() {
        scrollView.setZoomScale(scrollView.minimumZoomScale, animated: false)
        updateScrollInset()
    }

    func loadImage(from urlString: String) {
        guard let url = URL(string: urlString) else { return }

        spinner.startAnimating()

        Task { @MainActor [weak self] in
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                self?.spinner.stopAnimating()
                guard let image = UIImage(data: data) else { return }
                self?.imageView.image = image
                self?.view.setNeedsLayout()
            } catch {
                self?.spinner.stopAnimating()
            }
        }
    }

    @objc private func onDoubleTap(_ sender: UITapGestureRecognizer) {
        let targetScale = scrollView.maximumZoomScale

        if targetScale != scrollView.zoomScale {
            let tapPoint = sender.location(in: imageView)
            let size = CGSize(
                width: scrollView.frame.size.width / targetScale,
                height: scrollView.frame.size.height / targetScale
            )
            let origin = CGPoint(
                x: tapPoint.x - size.width / 2,
                y: tapPoint.y - size.height / 2
            )
            scrollView.zoom(to: CGRect(origin: origin, size: size), animated: true)
        } else {
            scrollView.setZoomScale(scrollView.minimumZoomScale, animated: true)
            updateScrollInset()
        }
    }

    @objc private func onSingleTap(_ sender: UITapGestureRecognizer) {
        setFocusMode(!isFocusMode)
    }

    @objc private func onPan(_ sender: UIPanGestureRecognizer) {
        // Only handle drag when not zoomed
        guard scrollView.zoomScale == scrollView.minimumZoomScale else { return }
        guard onDismiss != nil else { return }

        let translation = sender.translation(in: view)

        switch sender.state {
        case .changed:
            if !isDismissDragging {
                guard abs(translation.y) > abs(translation.x) else { return }
                isDismissDragging = true
                // Disable scroll view bouncing to prevent it from fighting the dismiss transform
                scrollView.bounces = false
            }
            scrollView.transform = CGAffineTransform(translationX: 0, y: translation.y)
            onDragYChanged?(translation.y)

        case .ended, .cancelled:
            if isDismissDragging {
                isDismissDragging = false
                scrollView.bounces = true
                if abs(translation.y) > dragDismissThreshold {
                    let direction: CGFloat = translation.y > 0 ? 1 : -1
                    UIView.animate(withDuration: 0.2) {
                        self.scrollView.transform = CGAffineTransform(
                            translationX: 0,
                            y: direction * self.view.bounds.height
                        )
                    } completion: { _ in
                        self.onDismiss?()
                    }
                } else {
                    UIView.animate(withDuration: 0.25) {
                        self.scrollView.transform = .identity
                    }
                    onDragYChanged?(0)
                }
            }

        default:
            break
        }
    }
}

// MARK: - Safe Array Access

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
