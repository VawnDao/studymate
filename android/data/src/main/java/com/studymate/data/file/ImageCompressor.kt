package com.studymate.data.file

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.studymate.domain.common.ErrorCode
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Giảm kích thước ảnh trước khi upload: tiết kiệm băng thông và token OCR, đồng thời
 * đưa mọi định dạng (kể cả HEIC) về JPEG mà backend Go đọc được.
 */
class ImageCompressor @Inject constructor() {

    fun compress(open: () -> InputStream, maxEdge: Int = MAX_EDGE, quality: Int = JPEG_QUALITY): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) throw LocalFileException(ErrorCode.UNSUPPORTED_TYPE)
        if (max(width, height) > MAX_SOURCE_EDGE) throw LocalFileException(ErrorCode.FILE_TOO_LARGE)

        val decoded = open().use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize(width, height, maxEdge) })
        } ?: throw LocalFileException(ErrorCode.UNSUPPORTED_TYPE)

        val orientation = runCatching {
            open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = orientationMatrix(orientation)
        val scale = min(1f, maxEdge.toFloat() / max(decoded.width, decoded.height))
        if (scale < 1f) matrix.postScale(scale, scale)

        val output = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        }
        return try {
            ByteArrayOutputStream().use { stream ->
                output.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
        } finally {
            if (output !== decoded) output.recycle()
            decoded.recycle()
        }
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    private fun orientationMatrix(orientation: Int) = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            else -> Unit
        }
    }

    companion object {
        const val MAX_EDGE = 1600
        const val JPEG_QUALITY = 85
        /** Chặn ảnh quá lớn (ảnh "bom") trước khi giải mã. */
        private const val MAX_SOURCE_EDGE = 12_000
    }
}
