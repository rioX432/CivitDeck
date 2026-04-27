package com.riox432.civitdeck.feature.comfyui.data.encoder

import com.riox432.civitdeck.feature.comfyui.domain.model.PathSegment
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual class MaskPngEncoder actual constructor() {

    actual fun encode(
        segments: List<PathSegment>,
        width: Int,
        height: Int,
        inverted: Boolean,
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
        )

        // Fill with base color
        val baseColor = if (inverted) Color.WHITE else Color.BLACK
        g2d.color = baseColor
        g2d.fillRect(0, 0, width, height)

        for (segment in segments) {
            g2d.color = if (segment.isEraser) {
                baseColor
            } else {
                if (inverted) Color.BLACK else Color.WHITE
            }
            val strokeWidth = segment.brushSize * width / REFERENCE_CANVAS_SIZE
            g2d.stroke = BasicStroke(
                strokeWidth,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
            )

            val path = buildSmoothPath(segment.points, width.toFloat(), height.toFloat())
            g2d.draw(path)
        }

        g2d.dispose()

        val stream = ByteArrayOutputStream()
        ImageIO.write(image, "png", stream)
        return stream.toByteArray()
    }

    private fun buildSmoothPath(
        points: List<Pair<Float, Float>>,
        w: Float,
        h: Float,
    ): Path2D.Float {
        val path = Path2D.Float()
        if (points.isEmpty()) return path

        val first = points.first()
        path.moveTo((first.first * w).toDouble(), (first.second * h).toDouble())

        if (points.size == 1) {
            path.lineTo(
                (first.first * w + SINGLE_POINT_OFFSET).toDouble(),
                (first.second * h + SINGLE_POINT_OFFSET).toDouble(),
            )
            return path
        }

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.first + curr.first) / 2f * w
            val midY = (prev.second + curr.second) / 2f * h
            path.quadTo(
                (prev.first * w).toDouble(),
                (prev.second * h).toDouble(),
                midX.toDouble(),
                midY.toDouble(),
            )
        }
        val last = points.last()
        path.lineTo((last.first * w).toDouble(), (last.second * h).toDouble())
        return path
    }

    companion object {
        private const val REFERENCE_CANVAS_SIZE = 400f
        private const val SINGLE_POINT_OFFSET = 0.1f
    }
}
