package com.example.hackathonproject.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.hackathonproject.domain.ParsedItem
import com.example.hackathonproject.domain.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Success(val items: List<ParsedItem>) : ScanState
    data class Error(val message: String) : ScanState
}

class ReceiptScanViewModel(private val context: Context) : ViewModel() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun recognize(uri: Uri) {
        _state.value = ScanState.Loading
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            _state.value = ScanState.Error("Не удалось открыть изображение")
            return
        }
        recognizer.process(image)
            .addOnSuccessListener { text ->
                val items = ReceiptParser.parse(text.toRows())
                _state.value = if (items.isEmpty()) {
                    ScanState.Error("Не удалось распознать позиции. Попробуйте более чёткое фото.")
                } else {
                    ScanState.Success(items)
                }
            }
            .addOnFailureListener {
                _state.value = ScanState.Error(it.message ?: "Ошибка распознавания")
            }
    }

    fun reset() {
        _state.value = ScanState.Idle
    }

    override fun onCleared() {
        recognizer.close()
    }
}

/**
 * Rebuilds visual rows from recognised lines. ML Kit may split a receipt row into separate
 * column fragments, so we cluster lines by vertical position and order each cluster left-to-right.
 */
private fun Text.toRows(): List<String> {
    data class Fragment(val text: String, val top: Int, val left: Int, val height: Int)

    val fragments = textBlocks
        .flatMap { it.lines }
        .mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            Fragment(line.text, box.top, box.left, box.height())
        }
        .sortedBy { it.top }

    if (fragments.isEmpty()) return emptyList()

    val rows = mutableListOf<MutableList<Fragment>>()
    for (fragment in fragments) {
        val currentRow = rows.lastOrNull()
        val tolerance = (fragment.height * 0.6).toInt().coerceAtLeast(8)
        if (currentRow != null && abs(currentRow.first().top - fragment.top) <= tolerance) {
            currentRow.add(fragment)
        } else {
            rows.add(mutableListOf(fragment))
        }
    }

    return rows.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } }
}
