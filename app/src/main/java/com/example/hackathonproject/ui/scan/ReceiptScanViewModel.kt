package com.example.hackathonproject.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackathonproject.domain.ParsedItem
import com.example.hackathonproject.domain.ReceiptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Success(val items: List<ParsedItem>) : ScanState
    data class Error(val message: String) : ScanState
}

class ReceiptScanViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun recognize(uri: Uri) {
        _state.value = ScanState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val rows = TesseractOcr.recognizeRows(context, uri)
                    ReceiptParser.parse(rows)
                }
            }
            _state.value = result.fold(
                onSuccess = { items ->
                    if (items.isEmpty()) {
                        ScanState.Error("Не удалось распознать позиции. Попробуйте более чёткое фото.")
                    } else {
                        ScanState.Success(items)
                    }
                },
                onFailure = { ScanState.Error(it.message ?: "Ошибка распознавания") }
            )
        }
    }

    fun reset() {
        _state.value = ScanState.Idle
    }
}
