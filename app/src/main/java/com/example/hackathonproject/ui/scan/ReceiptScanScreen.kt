package com.example.hackathonproject.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hackathonproject.AppViewModelProvider
import com.example.hackathonproject.domain.ParsedItem
import com.example.hackathonproject.domain.isAmountInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    onBack: () -> Unit,
    onImport: (List<ParsedItem>) -> Unit,
    viewModel: ReceiptScanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.recognize(uri) }

    val launchPicker: () -> Unit = {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Open the gallery automatically the first time the screen appears.
    LaunchedEffect(Unit) {
        if (state is ScanState.Idle) launchPicker()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканирование чека") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                is ScanState.Idle -> ScanPrompt(onPick = launchPicker)
                is ScanState.Loading -> LoadingState()
                is ScanState.Error -> ErrorState(message = current.message, onPick = launchPicker)
                is ScanState.Success -> ReviewList(
                    parsed = current.items,
                    onPickAnother = launchPicker,
                    onImport = onImport
                )
            }
        }
    }
}

@Composable
private fun ScanPrompt(onPick: () -> Unit) {
    CenteredMessage(
        title = "Выберите фото чека",
        subtitle = "Распознавание работает офлайн. Цены считываются точно, названия — приблизительно, поэтому их можно поправить.",
        buttonText = "Выбрать фото",
        onPick = onPick
    )
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Распознаём чек…")
    }
}

@Composable
private fun ErrorState(message: String, onPick: () -> Unit) {
    CenteredMessage(
        title = "Не получилось",
        subtitle = message,
        buttonText = "Выбрать другое фото",
        onPick = onPick
    )
}

@Composable
private fun CenteredMessage(
    title: String,
    subtitle: String,
    buttonText: String,
    onPick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ImageSearch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPick) { Text(buttonText) }
    }
}

private data class EditableItem(
    val name: String,
    val amount: String,
    val quantity: Int,
    val include: Boolean
)

@Composable
private fun ReviewList(
    parsed: List<ParsedItem>,
    onPickAnother: () -> Unit,
    onImport: (List<ParsedItem>) -> Unit
) {
    val items = remember(parsed) {
        mutableStateListOf<EditableItem>().apply {
            addAll(parsed.map { EditableItem(it.name, it.amount.toString(), it.quantity, true) })
        }
    }
    val selectedCount = items.count { it.include }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Проверьте позиции. Снимите галочки с итогов и поправьте названия при необходимости.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items) { index, item ->
                ReviewRow(
                    item = item,
                    onToggle = { items[index] = items[index].copy(include = it) },
                    onName = { items[index] = items[index].copy(name = it) },
                    onAmount = { if (it.isAmountInput()) items[index] = items[index].copy(amount = it) }
                )
            }
        }
        Surface(tonalElevation = 3.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedButton(
                    onClick = onPickAnother,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Выбрать другое фото") }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val result = items
                            .filter { it.include }
                            .mapNotNull { editable ->
                                val amount = editable.amount.toIntOrNull()
                                if (amount == null || amount <= 0) {
                                    null
                                } else {
                                    ParsedItem(editable.name.ifBlank { "Позиция" }, editable.quantity, amount)
                                }
                            }
                        onImport(result)
                    },
                    enabled = selectedCount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedCount > 0) "Добавить ($selectedCount)" else "Добавить")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewRow(
    item: EditableItem,
    onToggle: (Boolean) -> Unit,
    onName: (String) -> Unit,
    onAmount: (String) -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.include, onCheckedChange = onToggle)
            OutlinedTextField(
                value = item.name,
                onValueChange = onName,
                modifier = Modifier.weight(1f),
                label = { Text("Название") },
                supportingText = if (item.quantity > 1) {
                    { Text("количество: ${item.quantity}") }
                } else {
                    null
                },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = item.amount,
                onValueChange = onAmount,
                modifier = Modifier.width(120.dp),
                label = { Text("Сумма") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("₸") }
            )
        }
    }
}
