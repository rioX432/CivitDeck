import SwiftUI
import UIKit

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
                let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
                let (data, _) = try await ImageURLSession.shared.data(for: request)
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
