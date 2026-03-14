import SwiftUI

/// Slider orientation for image comparison.
enum SliderOrientation {
    case horizontal
    case vertical
}

/// A before/after image comparison slider with drag-to-reveal and pinch-to-zoom.
///
/// The before image is clipped by the slider position, revealing the after
/// image underneath. Users can drag the divider to compare and pinch to zoom.
struct ImageComparisonSlider: View {
    let beforeImageUrl: String
    let afterImageUrl: String
    var orientation: SliderOrientation = .horizontal

    @State private var sliderFraction: CGFloat = initialFraction
    @State private var scale: CGFloat = 1.0
    @State private var offset: CGSize = .zero

    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size

            ZStack {
                // After image (bottom layer, fully visible)
                comparisonImage(url: afterImageUrl, size: size)

                // Before image (top layer, clipped by slider)
                comparisonImage(url: beforeImageUrl, size: size)
                    .clipShape(SliderClipShape(fraction: sliderFraction, orientation: orientation))

                // Divider line
                dividerLine(size: size)

                // Drag handle
                sliderHandle(size: size)
            }
            .clipped()
            .gesture(magnificationGesture)
        }
    }

    // MARK: - Image

    private func comparisonImage(url: String, size: CGSize) -> some View {
        CachedAsyncImage(url: URL(string: url), maxPixelSize: imageMaxPixelSize) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFit()
                    .scaleEffect(scale)
                    .offset(offset)
            case .failure:
                Color.civitSurfaceVariant
                    .overlay {
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                            .accessibilityHidden(true)
                    }
            case .empty:
                Color.civitSurfaceVariant.shimmer()
            @unknown default:
                Color.civitSurfaceVariant
            }
        }
        .frame(width: size.width, height: size.height)
    }

    // MARK: - Divider

    private func dividerLine(size: CGSize) -> some View {
        Canvas { context, canvasSize in
            var path = Path()
            switch orientation {
            case .horizontal:
                let posX = canvasSize.width * sliderFraction
                path.move(to: CGPoint(x: posX, y: 0))
                path.addLine(to: CGPoint(x: posX, y: canvasSize.height))
            case .vertical:
                let posY = canvasSize.height * sliderFraction
                path.move(to: CGPoint(x: 0, y: posY))
                path.addLine(to: CGPoint(x: canvasSize.width, y: posY))
            }
            context.stroke(path, with: .color(.white), lineWidth: dividerWidth)
        }
        .allowsHitTesting(false)
    }

    // MARK: - Handle

    private func sliderHandle(size: CGSize) -> some View {
        let posX: CGFloat
        let posY: CGFloat

        switch orientation {
        case .horizontal:
            posX = size.width * sliderFraction
            posY = size.height / 2
        case .vertical:
            posX = size.width / 2
            posY = size.height * sliderFraction
        }

        return Circle()
            .fill(Color.civitSurface.opacity(handleAlpha))
            .frame(width: handleSize, height: handleSize)
            .overlay {
                Image(systemName: orientation == .horizontal
                      ? "arrow.left.and.right"
                      : "arrow.up.and.down")
                    .font(.civitIconSmallSemibold)
                    .foregroundColor(.civitOnSurface)
                    .accessibilityHidden(true)
            }
            .position(x: posX, y: posY)
            .gesture(dragGesture(size: size))
            .accessibilityLabel("Comparison slider")
            .accessibilityValue("\(Int(sliderFraction * 100))%")
    }

    // MARK: - Gestures

    private func dragGesture(size: CGSize) -> some Gesture {
        DragGesture()
            .onChanged { value in
                switch orientation {
                case .horizontal:
                    sliderFraction = (value.location.x / size.width).clamped(to: 0...1)
                case .vertical:
                    sliderFraction = (value.location.y / size.height).clamped(to: 0...1)
                }
            }
    }

    private var magnificationGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                scale = min(max(value, minScale), maxScale)
            }
            .onEnded { _ in
                withAnimation(MotionAnimation.fast) {
                    if scale < 1.2 {
                        scale = 1.0
                        offset = .zero
                    }
                }
            }
    }
}

// MARK: - Clip Shape

private struct SliderClipShape: Shape {
    let fraction: CGFloat
    let orientation: SliderOrientation

    func path(in rect: CGRect) -> Path {
        switch orientation {
        case .horizontal:
            return Path(CGRect(
                x: rect.minX, y: rect.minY,
                width: rect.width * fraction, height: rect.height
            ))
        case .vertical:
            return Path(CGRect(
                x: rect.minX, y: rect.minY,
                width: rect.width, height: rect.height * fraction
            ))
        }
    }
}

// MARK: - Constants

private let initialFraction: CGFloat = 0.5
private let minScale: CGFloat = 1.0
private let maxScale: CGFloat = 5.0
private let dividerWidth: CGFloat = 3
private let handleSize: CGFloat = 40
private let iconSize: CGFloat = 16
private let handleAlpha: Double = 0.85
private let imageMaxPixelSize: CGFloat = 1200

// MARK: - Comparable Clamping

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
