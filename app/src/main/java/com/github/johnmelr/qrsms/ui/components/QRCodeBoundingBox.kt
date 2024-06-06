package com.github.johnmelr.qrsms.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * A Drawable that handles displaying a bounding box around the QR Code
 *
 * Code taken from
 * https://github.com/android/camera-samples/blob/main/CameraX-MLKit/app/src/main/java/com/example/camerax_mlkit/QrCodeDrawable.kt
 * Modified to better suit the needs of QRSMS
 */
class QRCodeBoundingBox(private val barcode: Barcode): Drawable() {
    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.MAGENTA
        strokeWidth = 5F
        alpha = 200
    }

    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val contentTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }

    private val contentPadding = 25
    private var textWidth = contentTextPaint.measureText(barcode.rawValue).toInt()

    override fun draw(canvas: Canvas) {
        canvas.drawRect(barcode.boundingBox!!, boundingRectPaint)
//        canvas.drawRect(
//            Rect(
//                barcode.boundingBox!!.left,
//                barcode.boundingBox!!.bottom + contentPadding/2,
//                barcode.boundingBox!!.left + textWidth + contentPadding*2,
//                barcode.boundingBox!!.bottom + contentTextPaint.textSize.toInt() + contentPadding),
//            contentRectPaint
//        )
//        canvas.drawText(
//            barcode.rawValue.toString(),
//            (barcode.boundingBox!!.left + contentPadding).toFloat(),
//            (barcode.boundingBox!!.bottom + contentPadding*2).toFloat(),
//            contentTextPaint
//        )
    }

    override fun setAlpha(alpha: Int) {
        boundingRectPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        boundingRectPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT


}