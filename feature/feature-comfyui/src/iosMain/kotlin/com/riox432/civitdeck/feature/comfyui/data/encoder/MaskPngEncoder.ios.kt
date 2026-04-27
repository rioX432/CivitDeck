@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.feature.comfyui.data.encoder

import com.riox432.civitdeck.feature.comfyui.domain.model.PathSegment
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextAddLineToPoint
import platform.CoreGraphics.CGContextAddQuadCurveToPoint
import platform.CoreGraphics.CGContextBeginPath
import platform.CoreGraphics.CGContextMoveToPoint
import platform.CoreGraphics.CGContextSetFillColorWithColor
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGContextSetStrokeColorWithColor
import platform.CoreGraphics.CGContextStrokePath
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

// CGLineCap values: butt=0, round=1, square=2
// CGLineJoin values: miter=0, round=1, bevel=2

actual class MaskPngEncoder actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual fun encode(
        segments: List<PathSegment>,
        width: Int,
        height: Int,
        inverted: Boolean,
    ): ByteArray {
        val size = platform.CoreGraphics.CGSizeMake(width.toDouble(), height.toDouble())
        UIGraphicsBeginImageContextWithOptions(size, true, 1.0)
        val context = UIGraphicsGetCurrentContext()
            ?: run {
                UIGraphicsEndImageContext()
                return byteArrayOf()
            }

        val baseColor = if (inverted) UIColor.whiteColor else UIColor.blackColor
        CGContextSetFillColorWithColor(context, baseColor.CGColor)
        platform.CoreGraphics.CGContextFillRect(
            context,
            CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
        )

        val w = width.toDouble()
        val h = height.toDouble()

        for (segment in segments) {
            val strokeColor = if (segment.isEraser) {
                baseColor
            } else {
                if (inverted) UIColor.blackColor else UIColor.whiteColor
            }
            CGContextSetStrokeColorWithColor(context, strokeColor.CGColor)
            val strokeWidth = segment.brushSize.toDouble() * w / REFERENCE_CANVAS_SIZE
            CGContextSetLineWidth(context, strokeWidth)
            // Note: CGLineCap/CGLineJoin are K/N opaques — skip setting them.
            // Default butt cap is acceptable for mask rendering.

            drawSmoothPath(context, segment.points, w, h)
            CGContextStrokePath(context)
        }

        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        val pngData = image?.let { UIImagePNGRepresentation(it) }
            ?: return byteArrayOf()

        return pngData.toByteArray()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun drawSmoothPath(
        context: platform.CoreGraphics.CGContextRef?,
        points: List<Pair<Float, Float>>,
        w: Double,
        h: Double,
    ) {
        if (points.isEmpty() || context == null) return
        CGContextBeginPath(context)
        val first = points.first()
        CGContextMoveToPoint(context, first.first.toDouble() * w, first.second.toDouble() * h)

        if (points.size == 1) {
            CGContextAddLineToPoint(
                context,
                first.first.toDouble() * w + SINGLE_POINT_OFFSET,
                first.second.toDouble() * h + SINGLE_POINT_OFFSET,
            )
            return
        }

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.first + curr.first) / 2.0 * w
            val midY = (prev.second + curr.second) / 2.0 * h
            CGContextAddQuadCurveToPoint(
                context,
                prev.first.toDouble() * w,
                prev.second.toDouble() * h,
                midX,
                midY,
            )
        }
        val last = points.last()
        CGContextAddLineToPoint(context, last.first.toDouble() * w, last.second.toDouble() * h)
    }

    companion object {
        private const val REFERENCE_CANVAS_SIZE = 400.0
        private const val SINGLE_POINT_OFFSET = 0.1
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val bytes = ByteArray(length)
    if (length > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}
