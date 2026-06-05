package com.example.hackathonproject.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.IOException

/**
 * Offline OCR via Tesseract with the Russian (Cyrillic) model. The receipt photo is
 * downsampled, rotated by EXIF, and turned grayscale with a contrast boost before recognition,
 * which matters a lot for pale thermal receipts.
 */
object TesseractOcr {

    private const val LANG = "rus"
    private const val TESSDATA_DIR = "tessdata"
    private const val TRAINED_DATA = "rus.traineddata"
    private const val MAX_DIMENSION = 2200
    private const val CONTRAST = 1.5f

    @Volatile
    private var api: TessBaseAPI? = null

    /** Recognises text and returns it as trimmed, non-empty rows for the parser. */
    fun recognizeRows(context: Context, uri: Uri): List<String> {
        val engine = ensureApi(context)
        val bitmap = loadAndPreprocess(context, uri)
        val text = synchronized(engine) {
            engine.setImage(bitmap)
            val recognised = engine.getUTF8Text() ?: ""
            engine.clear()
            recognised
        }
        bitmap.recycle()
        return text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun ensureApi(context: Context): TessBaseAPI {
        api?.let { return it }
        return synchronized(this) {
            api ?: run {
                val dataPath = copyTrainedDataIfNeeded(context)
                val engine = TessBaseAPI()
                if (!engine.init(dataPath, LANG)) {
                    engine.recycle()
                    throw IOException("Не удалось инициализировать Tesseract")
                }
                // Receipts are a single column of variable-size text.
                engine.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN
                api = engine
                engine
            }
        }
    }

    /** Tesseract needs a real file path, so copy the bundled asset into internal storage once. */
    private fun copyTrainedDataIfNeeded(context: Context): String {
        val dataPath = context.filesDir.absolutePath
        val tessDir = File(dataPath, TESSDATA_DIR).apply { if (!exists()) mkdirs() }
        val target = File(tessDir, TRAINED_DATA)
        if (!target.exists() || target.length() == 0L) {
            context.assets.open("$TESSDATA_DIR/$TRAINED_DATA").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return dataPath
    }

    private fun loadAndPreprocess(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (bounds.outWidth / sample > MAX_DIMENSION || bounds.outHeight / sample > MAX_DIMENSION) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Не удалось открыть изображение")

        val rotated = applyExifRotation(context, uri, decoded)
        return toGrayscale(rotated)
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val translate = 128f * (1f - CONTRAST)
        val matrix = ColorMatrix().apply {
            setSaturation(0f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        CONTRAST, 0f, 0f, 0f, translate,
                        0f, CONTRAST, 0f, 0f, translate,
                        0f, 0f, CONTRAST, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        Canvas(output).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        if (output != src) src.recycle()
        return output
    }
}
