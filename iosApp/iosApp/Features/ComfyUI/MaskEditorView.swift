import SwiftUI
import Shared

private let eraserActiveColor = Color.orange

struct MaskEditorView: View {
    @StateObject private var viewModel = MaskEditorViewModelOwner()
    @Environment(\.dismiss) private var dismiss

    let sourceImageUrl: String
    let imageWidth: Int
    let imageHeight: Int
    let onMaskReady: (String) -> Void

    var body: some View {
        VStack(spacing: 0) {
            canvasArea
            toolbar
            brushSizeSlider
        }
        .navigationTitle("Mask Editor")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                doneButton
            }
        }
        .task { await viewModel.observeUiState() }
        .onChange(of: viewModel.uploadedMaskFilename) { newValue in
            if let filename = newValue {
                onMaskReady(filename)
                dismiss()
            }
        }
    }

    @ViewBuilder
    private var doneButton: some View {
        if viewModel.isUploading {
            ProgressView()
        } else {
            Button("Done") {
                encodeMaskAndUpload()
            }
            .disabled(!viewModel.hasContent)
        }
    }

    private var canvasArea: some View {
        ZStack {
            // Source image background
            if !sourceImageUrl.isEmpty {
                CachedAsyncImage(
                    url: URL(string: sourceImageUrl),
                    maxPixelSize: 1200
                ) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().aspectRatio(contentMode: .fit)
                    default:
                        Color.civitSurfaceVariant
                    }
                }
            } else {
                Color.civitSurfaceVariant
            }

            // Mask painting canvas
            MaskPaintCanvasView(
                segments: viewModel.pathSegments,
                brushSize: viewModel.brushSize,
                isEraserMode: viewModel.isEraserMode,
                onStrokeCompleted: { points in
                    viewModel.onStrokeCompleted(points)
                }
            )
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
        .padding(Spacing.md)
    }

    private var toolbar: some View {
        HStack(spacing: Spacing.lg) {
            Button(action: viewModel.onToggleEraser) {
                Image(systemName: "paintbrush")
                    .foregroundColor(
                        viewModel.isEraserMode ? eraserActiveColor : .civitOnSurface
                    )
            }
            .accessibilityLabel("Toggle eraser")

            Button(action: viewModel.onUndo) {
                Image(systemName: "arrow.uturn.backward")
            }
            .disabled(!viewModel.canUndo)
            .accessibilityLabel("Undo")

            Button(action: viewModel.onRedo) {
                Image(systemName: "arrow.uturn.forward")
            }
            .disabled(!viewModel.canRedo)
            .accessibilityLabel("Redo")

            Button(action: viewModel.onClear) {
                Image(systemName: "trash")
            }
            .disabled(!viewModel.hasContent)
            .accessibilityLabel("Clear")

            Button(action: viewModel.onInvertMask) {
                Image(systemName: "circle.lefthalf.filled")
            }
            .accessibilityLabel("Invert mask")
        }
        .padding(.horizontal, Spacing.md)
    }

    private var brushSizeSlider: some View {
        HStack {
            Text("Brush: \(Int(viewModel.brushSize))")
                .font(.civitBodySmall)
            Slider(
                value: Binding(
                    get: { Double(viewModel.brushSize) },
                    set: { viewModel.onBrushSizeChanged(Float($0)) }
                ),
                in: 5...150
            )
        }
        .padding(.horizontal, Spacing.md)
        .padding(.bottom, Spacing.sm)
    }

    private func encodeMaskAndUpload() {
        // Encode mask on a background thread using UIGraphicsImageRenderer
        let segments = viewModel.pathSegments
        let inverted = viewModel.isInverted
        let w = imageWidth
        let h = imageHeight

        Task.detached {
            let pngData = renderMaskPng(
                segments: segments,
                width: w,
                height: h,
                inverted: inverted
            )
            await MainActor.run {
                viewModel.onUploadMask(pngData)
            }
        }
    }
}

// MARK: - Mask PNG Rendering (iOS)

private let iosReferenceCanvasSize: CGFloat = 400

private func renderMaskPng(
    segments: [Feature_comfyuiPathSegment],
    width: Int,
    height: Int,
    inverted: Bool
) -> Data {
    let size = CGSize(width: width, height: height)
    let renderer = UIGraphicsImageRenderer(size: size)
    let image = renderer.image { ctx in
        let cgContext = ctx.cgContext
        // Fill with base color
        let baseColor: UIColor = inverted ? .white : .black
        cgContext.setFillColor(baseColor.cgColor)
        cgContext.fill(CGRect(origin: .zero, size: size))

        for segment in segments {
            let maskColor: UIColor = segment.isEraser ? baseColor :
                (inverted ? .black : .white)
            cgContext.setStrokeColor(maskColor.cgColor)
            let strokeW = CGFloat(segment.brushSize) * CGFloat(width) / iosReferenceCanvasSize
            cgContext.setLineWidth(strokeW)
            cgContext.setLineCap(.round)
            cgContext.setLineJoin(.round)

            let points = extractPoints(segment)
            drawSmoothPath(context: cgContext, points: points, w: CGFloat(width), h: CGFloat(height))
            cgContext.strokePath()
        }
    }
    return image.pngData() ?? Data()
}

private func extractPoints(_ segment: Feature_comfyuiPathSegment) -> [(CGFloat, CGFloat)] {
    guard let points = segment.points as? [KotlinPair<NSNumber, NSNumber>] else { return [] }
    return points.map { (CGFloat($0.first?.floatValue ?? 0), CGFloat($0.second?.floatValue ?? 0)) }
}

private func drawSmoothPath(
    context: CGContext,
    points: [(CGFloat, CGFloat)],
    w: CGFloat,
    h: CGFloat
) {
    guard !points.isEmpty else { return }
    context.beginPath()
    let first = points[0]
    context.move(to: CGPoint(x: first.0 * w, y: first.1 * h))
    if points.count == 1 {
        context.addLine(to: CGPoint(x: first.0 * w + 0.1, y: first.1 * h + 0.1))
        return
    }
    for i in 1..<points.count {
        let prev = points[i - 1]
        let curr = points[i]
        let midX = (prev.0 + curr.0) / 2 * w
        let midY = (prev.1 + curr.1) / 2 * h
        context.addQuadCurve(
            to: CGPoint(x: midX, y: midY),
            control: CGPoint(x: prev.0 * w, y: prev.1 * h)
        )
    }
    let last = points.last ?? (0, 0)
    context.addLine(to: CGPoint(x: last.0 * w, y: last.1 * h))
}
