package com.example.snapcal.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageCompressor {
    suspend fun compressImage(context: Context, uri: Uri): Uri = withContext(Dispatchers.IO) {
        try {
            // 1. Read EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }

            // 2. Decode Bitmap
            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                bitmap = BitmapFactory.decodeStream(inputStream)
            }

            if (bitmap == null) return@withContext uri

            // 3. Rotate Bitmap if needed based on EXIF
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.postRotate(180f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(-90f)
                    matrix.preScale(-1f, 1f)
                }
            }

            val rotatedBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
            } else {
                bitmap!!
            }

            // 4. Compress and save
            val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            outputStream.flush()
            outputStream.close()

            // 5. Cleanup
            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap!!.recycle()

            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            uri // Return original URI if compression fails
        }
    }
}
