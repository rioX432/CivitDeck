package com.riox432.civitdeck.feature.comfyui.data.encoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.riox432.civitdeck.feature.comfyui.domain.model.PathSegment
import java.io.ByteArrayOutputStream

actual class MaskPngEncoder actual constructor() {

    actual fun encode(
        segments: List<PathSegment>,
        width: Int,
        height: Int,
        inverted: Boolean,
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill with base color
        val baseColor = if (inverted) Color.WHITE else Color.BLACK
        canvas.drawColor(baseColor)

        val drawPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (segment in segments) {
            drawPaint.strokeWidth = segment.brushSize * width / REFERENCE_CANVAS_SIZE
            if (segment.isEraser) {
                drawPaint.color = baseColor
            } else {
                val maskColor = if (inverted) Color.BLACK else Color.WHITE
                drawPaint.color = maskColor
            }
            drawPaint.xfermode = null

            val path = buildSmoothPath(segment.points, width.toFloat(), height.toFloat())
            canvas.drawPath(path, drawPaint)
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESS_QUALITY, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    private fun buildSmoothPath(
        points: List<Pair<Float, Float>>,
        w: Float,
        h: Float,
    ): Path {
        val path = Path()
        if (points.isEmpty()) return path
        val first = points.first()
        path.moveTo(first.first * w, first.second * h)
        if (points.size == 1) {
            // Single point: draw a tiny line to make it visible
            path.lineTo(first.first * w + 0.1f, first.second * h + 0.1f)
            return path
        }
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.first + curr.first) / 2f * w
            val midY = (prev.second + curr.second) / 2f * h
            path.quadTo(prev.first * w, prev.second * h, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.first * w, last.second * h)
        return path
    }

    companion object {
        private const val REFERENCE_CANVAS_SIZE = 400f
        private const val COMPRESS_QUALITY = 100
    }
}
