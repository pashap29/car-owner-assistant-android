package com.carownerassistant.core.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

fun compressImageToJpeg(
    context: Context,
    sourceUri: Uri,
    destinationFile: File,
    quality: Int = 75,
    maxDimension: Int = 1600,
): String? {
    return runCatching {
        val originalBitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null

        val scaledBitmap = scaleBitmapIfNeeded(
            bitmap = originalBitmap,
            maxDimension = maxDimension,
        )

        FileOutputStream(destinationFile).use { output ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }

        if (scaledBitmap !== originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()

        destinationFile.absolutePath
    }.getOrNull()
}

private fun scaleBitmapIfNeeded(
    bitmap: Bitmap,
    maxDimension: Int,
): Bitmap {
    val longestSide = max(bitmap.width, bitmap.height)
    if (longestSide <= maxDimension) {
        return bitmap
    }

    val scale = maxDimension.toFloat() / longestSide.toFloat()
    val newWidth = (bitmap.width * scale).roundToInt()
    val newHeight = (bitmap.height * scale).roundToInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}
