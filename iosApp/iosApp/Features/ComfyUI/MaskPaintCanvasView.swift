import SwiftUI
import Shared

private let maskColor = Color.red.opacity(0.5)
private let referenceCanvasSize: CGFloat = 400

/// A canvas view that renders mask paint strokes and captures new drawing input
/// via a DragGesture. Points are normalized to [0, 1] for cross-platform consistency.
struct MaskPaintCanvasView: View {
    let segments: [Feature_comfyuiPathSegment]
    let brushSize: Float
    let isEraserMode: Bool
    let onStrokeCompleted: ([(Float, Float)]) -> Void

    @State private var currentPoints: [CGPoint] = []

    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size
            Canvas { context, canvasSize in
                drawCommittedSegments(context: &context, canvasSize: canvasSize)
                drawCurrentStroke(context: &context, canvasSize: canvasSize)
            }
            .gesture(
                DragGesture(minimumDistance: 1)
                    .onChanged { value in
                        currentPoints.append(value.location)
                    }
                    .onEnded { _ in
                        let w = size.width
                        let h = size.height
                        guard w > 0, h > 0 else { return }
                        let normalized = currentPoints.map { pt in
                            (Float(pt.x / w), Float(pt.y / h))
                        }
                        onStrokeCompleted(normalized)
                        currentPoints = []
                    }
            )
        }
    }

    private func drawCommittedSegments(
        context: inout GraphicsContext,
        canvasSize: CGSize
    ) {
        for segment in segments {
            let points = segmentPoints(segment)
            let strokeW = CGFloat(segment.brushSize) * canvasSize.width / referenceCanvasSize
            let color: Color = segment.isEraser ? .clear : maskColor
            var path = buildSmoothPath(
                points: points,
                canvasSize: canvasSize
            )
            context.stroke(
                path,
                with: .color(color),
                style: StrokeStyle(
                    lineWidth: strokeW,
                    lineCap: .round,
                    lineJoin: .round
                )
            )
        }
    }

    private func drawCurrentStroke(
        context: inout GraphicsContext,
        canvasSize: CGSize
    ) {
        guard !currentPoints.isEmpty else { return }
        let strokeW = CGFloat(brushSize) * canvasSize.width / referenceCanvasSize
        let color: Color = isEraserMode ? .clear : maskColor
        var path = Path()
        path.move(to: currentPoints[0])
        if currentPoints.count == 1 {
            let pt = currentPoints[0]
            path.addLine(to: CGPoint(x: pt.x + 0.1, y: pt.y + 0.1))
        } else {
            for i in 1..<currentPoints.count {
                let prev = currentPoints[i - 1]
                let curr = currentPoints[i]
                let mid = CGPoint(
                    x: (prev.x + curr.x) / 2,
                    y: (prev.y + curr.y) / 2
                )
                path.addQuadCurve(to: mid, control: prev)
            }
            path.addLine(to: currentPoints.last ?? .zero)
        }
        context.stroke(
            path,
            with: .color(color),
            style: StrokeStyle(
                lineWidth: strokeW,
                lineCap: .round,
                lineJoin: .round
            )
        )
    }

    private func segmentPoints(_ segment: Feature_comfyuiPathSegment) -> [(CGFloat, CGFloat)] {
        guard let points = segment.points as? [KotlinPair<NSNumber, NSNumber>] else {
            return []
        }
        return points.map { pair in
            (CGFloat(pair.first?.floatValue ?? 0), CGFloat(pair.second?.floatValue ?? 0))
        }
    }

    private func buildSmoothPath(
        points: [(CGFloat, CGFloat)],
        canvasSize: CGSize
    ) -> Path {
        var path = Path()
        guard !points.isEmpty else { return path }
        let first = points[0]
        path.move(to: CGPoint(x: first.0 * canvasSize.width, y: first.1 * canvasSize.height))
        if points.count == 1 {
            path.addLine(to: CGPoint(
                x: first.0 * canvasSize.width + 0.1,
                y: first.1 * canvasSize.height + 0.1
            ))
            return path
        }
        for i in 1..<points.count {
            let prev = points[i - 1]
            let curr = points[i]
            let midX = (prev.0 + curr.0) / 2 * canvasSize.width
            let midY = (prev.1 + curr.1) / 2 * canvasSize.height
            path.addQuadCurve(
                to: CGPoint(x: midX, y: midY),
                control: CGPoint(x: prev.0 * canvasSize.width, y: prev.1 * canvasSize.height)
            )
        }
        let last = points.last ?? (0, 0)
        path.addLine(to: CGPoint(x: last.0 * canvasSize.width, y: last.1 * canvasSize.height))
        return path
    }
}
