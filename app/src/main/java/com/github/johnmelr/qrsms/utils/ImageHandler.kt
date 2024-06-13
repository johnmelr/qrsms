package com.github.johnmelr.qrsms.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


/**
 * Utility Class responsible for handling saving of images. Since there are two methods of saving
 * an image depending on the API level, it would be much easier to simple provide a helper class
 * than can do either approach depending on the user's device
 *
 * @property image the image file to be saved in bitmap format
 */
class ImageHandler(
    private val appContext: Context,
    private val image: Bitmap,
    private val imagesFolder: File,
    private val filename: String,
) {
    private val contentResolver = appContext.contentResolver

    fun saveImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageForAndroidApi29AndAbove()
        } else {
            saveImageForAndroidApi28AndBelow()
        }
    }

    @Throws(IOException::class)
    private fun saveImageForAndroidApi28AndBelow(): Uri? {
        var uriReturn: Uri? = null
        try {
            // Generate Filename
            imagesFolder.mkdirs()
            val file = File(imagesFolder, filename)

            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            MediaScannerConnection.scanFile(appContext, arrayOf(file.toString()), null,
                OnScanCompletedListener { path, uri ->
                    Log.i("ExternalStorage", "Scanned $path:")
                    Log.i("ExternalStorage", "-> uri=$uri")
                    uriReturn = uri
                })
        } catch (e: IOException) {
            Log.e("ImageHandler", e.message.toString())
        }

        return uriReturn
    }

    @Throws(IOException::class)
    private fun saveImageForAndroidApi29AndAbove(): Uri {
         val values = ContentValues()
         values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
         values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")

         values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)

         var uri: Uri? = null
         try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = contentResolver.insert(contentUri, values)
            if (uri == null) {
                //isSuccess = false;
                throw IOException("Failed to create new MediaStore record.")
            }
            contentResolver.openOutputStream(uri).use { stream ->
                if (stream == null) {
                    //isSuccess = false;
                    throw IOException("Failed to open output stream.")
                }
                if (!image.compress(Bitmap.CompressFormat.PNG, 95, stream)) {
                    //isSuccess = false;
                    throw IOException("Failed to save bitmap.")
                }
            }
             return uri
            //isSuccess = true;
        } catch (e: IOException) {
            if (uri != null) {
                contentResolver.delete(uri, null, null)
            }
            throw e
        }
    }
}
