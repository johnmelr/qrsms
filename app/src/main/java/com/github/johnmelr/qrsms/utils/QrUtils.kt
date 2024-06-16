package com.github.johnmelr.qrsms.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrUtils {
    /**
     * Encodes the given string into a QR Code
     *
     * @param textToEncode text to be encoded inside the qr code
     * @return a bitmap image of the QR Code
     */
    fun generateQrCode(textToEncode: String): Bitmap {
        val size = 512 //pixels
        val hints = hashMapOf<EncodeHintType, Int>().also {
            it[EncodeHintType.MARGIN] = 2
        } // Make the QR code buffer border narrower
        val bits = QRCodeWriter().encode(textToEncode, BarcodeFormat.QR_CODE, size, size, hints)

        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    it.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }
}