package com.example.hackathonproject.ui.billsplitter

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hackathonproject.AppViewModelProvider
import com.example.hackathonproject.domain.BillCalculator
import com.example.hackathonproject.domain.Person
import com.example.hackathonproject.domain.ScannedItem
import com.example.hackathonproject.domain.SharedExpense
import com.example.hackathonproject.domain.remainingQuantity
import com.example.hackathonproject.domain.toggle
import com.example.hackathonproject.ui.util.formatMoney
import kotlin.math.abs

private fun Person.displayName(index: Int): String = name.ifBlank { "Человек ${index + 1}" }
private fun SharedExpense.displayName(index: Int): String = name.ifBlank { "Общий расход ${index + 1}" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterScreen(
    onBillSaved: () -> Unit,
    onScanReceipt: () -> Unit,
    viewModel: BillSplitterViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedPeople by viewModel.savedPeople.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showSavedSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }
    var itemPickerPersonId by remember { mutableStateOf<Int?>(null) }
    var showSharedPicker by remember { mutableStateOf(false) }

    if (showSavedSheet) {
        var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
        ModalBottomSheet(onDismissRequest = { showSavedSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Сохранённые люди",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (savedPeople.isEmpty()) {
                    Text(
                        text = "Пока никого. Сохранённые люди появятся здесь после первого счёта или со вкладки «Люди».",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        savedPeople.forEach { person ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedIds = selectedIds.toggle(person.id) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(person.id),
                                    onCheckedChange = { selectedIds = selectedIds.toggle(person.id) }
                                )
                                Text(
                                    text = person.name,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.addSavedPeople(savedPeople.filter { selectedIds.contains(it.id) })
                            showSavedSheet = false
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedIds.isEmpty()) "Добавить" else "Добавить (${selectedIds.size})")
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveBillDialog(
            title = saveTitle,
            onTitleChange = { saveTitle = it },
            onConfirm = {
                viewModel.saveBill(saveTitle) { onBillSaved() }
                showSaveDialog = false
                saveTitle = ""
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    val pickerPersonId = itemPickerPersonId
    if (pickerPersonId != null) {
        ScannedItemPickerSheet(
            scannedItems = state.scannedItems,
            people = state.people,
            sharedExpenses = state.sharedExpenses,
            onPick = { viewModel.addScannedItemToPerson(pickerPersonId, it.id) },
            onAddManual = {
                viewModel.addPersonItem(pickerPersonId)
                itemPickerPersonId = null
            },
            onDismiss = { itemPickerPersonId = null }
        )
    }

    if (showSharedPicker) {
        ScannedItemPickerSheet(
            scannedItems = state.scannedItems,
            people = state.people,
            sharedExpenses = state.sharedExpenses,
            onPick = { viewModel.addSharedExpenseFromScanned(it.id) },
            onAddManual = {
                viewModel.addSharedExpense()
                showSharedPicker = false
            },
            onDismiss = { showSharedPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Разделение счета",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedButton(
            onClick = onScanReceipt,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Сканировать чек")
        }

        if (state.scannedItems.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Позиции из чека",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Раскройте человека ниже и добавьте ему позиции.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.scannedItems.forEach { item ->
                        val remaining = item.remainingQuantity(state.people, state.sharedExpenses)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.name} · ${item.unitPrice}₸",
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (remaining > 0) "осталось $remaining из ${item.quantity}" else "распределено",
                                fontSize = 12.sp,
                                color = if (remaining > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }

        // Total bill ------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Общая сумма чека",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                OutlinedTextField(
                    value = state.totalBillAmount,
                    onValueChange = viewModel::setTotalBillAmount,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Сумма") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("₸") }
                )
            }
        }

        // Service charge --------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Процент обслуживания",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                OutlinedTextField(
                    value = state.serviceChargePercent,
                    onValueChange = viewModel::setServiceCharge,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Процент") },
                    placeholder = { Text("10") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") }
                )
            }
        }

        // Shared expenses -------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Общие расходы (чай, сок)",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val hasPool = state.scannedItems.any { it.remainingQuantity(state.people, state.sharedExpenses) > 0 }
                    if (hasPool) showSharedPicker = true else viewModel.addSharedExpense()
                },
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        }

        state.sharedExpenses.forEachIndexed { index, expense ->
            val isExpanded = state.expandedExpenses.contains(expense.id)
            val allSelected = state.people.isNotEmpty() && expense.participants.size == state.people.size

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleExpenseExpanded(expense.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть"
                                )
                            }
                            Text(
                                text = expense.displayName(index),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.removeExpense(expense.id) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (isExpanded) {
                        OutlinedTextField(
                            value = expense.name,
                            onValueChange = { viewModel.updateExpenseName(expense.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Название (чай, сок)") },
                            placeholder = { Text("Чай") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expense.amount,
                            onValueChange = { viewModel.updateExpenseAmount(expense.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Сумма") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text("₸") }
                        )
                        if (state.people.isNotEmpty()) {
                            Text(
                                text = "Кто участвует:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { viewModel.setAllParticipants(expense.id, it) }
                                )
                                Text(
                                    text = "Все",
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            state.people.forEachIndexed { personIndex, person ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = expense.participants.contains(person.id),
                                        onCheckedChange = {
                                            viewModel.toggleExpenseParticipant(expense.id, person.id, it)
                                        }
                                    )
                                    Text(
                                        text = person.displayName(personIndex),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // People ----------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Люди и их заказы",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { viewModel.addPerson() },
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        }

        OutlinedButton(
            onClick = { showSavedSheet = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Добавить из сохранённых")
        }

        state.people.forEachIndexed { index, person ->
            val isExpanded = state.expandedPeople.contains(person.id)
            val subtotal = person.items.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePersonExpanded(person.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть"
                                )
                            }
                            Text(
                                text = person.displayName(index),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (subtotal > 0) {
                            Text(
                                text = "${formatMoney(subtotal)}₸",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.removePerson(person.id) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (isExpanded) {
                        OutlinedTextField(
                            value = person.name,
                            onValueChange = { viewModel.updatePersonName(person.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Имя") },
                            placeholder = { Text("Введите имя") },
                            singleLine = true
                        )

                        person.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = { viewModel.updatePersonItemName(person.id, item.id, it) },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Блюдо") },
                                    placeholder = { Text("Название") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = item.amount,
                                    onValueChange = { viewModel.updatePersonItemAmount(person.id, item.id, it) },
                                    modifier = Modifier.width(120.dp),
                                    label = { Text("Сумма") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    suffix = { Text("₸") }
                                )
                                IconButton(onClick = { viewModel.removePersonItem(person.id, item.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить позицию",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val hasPool = state.scannedItems.any { it.remainingQuantity(state.people, state.sharedExpenses) > 0 }
                                if (hasPool) itemPickerPersonId = person.id else viewModel.addPersonItem(person.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить позицию")
                        }
                    }
                }
            }
        }

        // Remaining amount ------------------------------------------------
        if (state.totalBillAmount.isNotEmpty() && state.people.isNotEmpty()) {
            val totalBillValue = state.totalBillAmount.toDoubleOrNull() ?: 0.0
            val serviceCharge = state.serviceChargePercent.toDoubleOrNull() ?: 0.0
            val totalPeopleAmounts =
                BillCalculator.grandTotal(state.people, state.sharedExpenses, serviceCharge)
            val remainingAmount = totalBillValue - totalPeopleAmounts

            if (abs(remainingAmount) > 0.01) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Оставшаяся сумма",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${formatMoney(remainingAmount)}₸",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (remainingAmount < 0) {
                            Text(
                                text = "Внимание: итоговая сумма превышает общую сумму чека!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Totals ----------------------------------------------------------
        if (state.people.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Итоги",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Сохранить")
                        }
                        Button(
                            onClick = {
                                val shareText = formatShareText(
                                    state.totalBillAmount,
                                    state.serviceChargePercent,
                                    state.people,
                                    state.sharedExpenses
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Поделиться итогами"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Поделиться")
                        }
                    }

                    val serviceCharge = state.serviceChargePercent.toDoubleOrNull() ?: 0.0

                    state.people.forEachIndexed { index, person ->
                        val breakdown =
                            BillCalculator.breakdown(person, state.sharedExpenses, serviceCharge)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = person.displayName(index),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatMoney(breakdown.total)}₸",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (breakdown.personalAmount > 0) {
                                Text(
                                    text = "Заказ: ${formatMoney(breakdown.personalAmount)}₸",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (breakdown.sharedAmount > 0) {
                                Text(
                                    text = "Общие расходы: ${formatMoney(breakdown.sharedAmount)}₸",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (breakdown.serviceChargeAmount > 0) {
                                Text(
                                    text = "Обслуживание: ${formatMoney(breakdown.serviceChargeAmount)}₸",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index != state.people.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannedItemPickerSheet(
    scannedItems: List<ScannedItem>,
    people: List<Person>,
    sharedExpenses: List<SharedExpense>,
    onPick: (ScannedItem) -> Unit,
    onAddManual: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Добавить из чека", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            val available = scannedItems.filter { it.remainingQuantity(people, sharedExpenses) > 0 }
            if (available.isEmpty()) {
                Text(
                    text = "Все позиции из чека распределены.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    available.forEach { item ->
                        val remaining = item.remainingQuantity(people, sharedExpenses)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(item) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontSize = 16.sp)
                                Text(
                                    text = "осталось $remaining из ${item.quantity}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = "${item.unitPrice}₸", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddManual() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить вручную")
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Готово")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveBillDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сохранить счёт") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Название") },
                placeholder = { Text("Например: Ужин в кафе") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

/** Builds the plain-text summary shared via the system share sheet. */
fun formatShareText(
    totalBillAmount: String,
    serviceChargePercent: String,
    people: List<Person>,
    sharedExpenses: List<SharedExpense>
): String {
    val serviceCharge = serviceChargePercent.toDoubleOrNull() ?: 0.0
    val totalBillValue = totalBillAmount.toDoubleOrNull() ?: 0.0

    val sb = StringBuilder()
    sb.append("💰 Разделение счета\n")
    sb.append("═══════════════════\n\n")

    if (totalBillAmount.isNotEmpty()) {
        sb.append("📋 Общая сумма чека: ${formatMoney(totalBillValue)}₸\n")
    }
    if (serviceChargePercent.isNotEmpty() && serviceCharge > 0) {
        sb.append("💼 Процент обслуживания: ${String.format(java.util.Locale.US, "%.1f", serviceCharge)}%\n")
    }

    if (sharedExpenses.isNotEmpty()) {
        sb.append("\n🍵 Общие расходы:\n")
        sharedExpenses.forEachIndexed { index, expense ->
            val amount = expense.amount.toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                val participants = people.filter { expense.participants.contains(it.id) }
                val participantNames = if (participants.isNotEmpty()) {
                    participants.joinToString(", ") { it.displayName(people.indexOf(it)) }
                } else {
                    "Не выбраны"
                }
                sb.append("  • ${expense.displayName(index)}: ${formatMoney(amount)}₸ ($participantNames)\n")
            }
        }
    }

    sb.append("\n👥 Итоги по людям:\n")
    sb.append("─────────────────────\n")

    people.forEachIndexed { index, person ->
        val breakdown = BillCalculator.breakdown(person, sharedExpenses, serviceCharge)
        sb.append("\n${person.displayName(index)} -- ${formatMoney(breakdown.total)}₸\n")
        person.items.forEach { item ->
            val amount = item.amount.toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                sb.append("  ${item.name.ifBlank { "Позиция" }}: ${formatMoney(amount)}₸\n")
            }
        }
        if (breakdown.sharedAmount > 0) {
            sb.append("  Общие расходы: ${formatMoney(breakdown.sharedAmount)}₸\n")
        }
        if (breakdown.serviceChargeAmount > 0) {
            sb.append("  Обслуживание: ${formatMoney(breakdown.serviceChargeAmount)}₸\n")
        }
    }

    if (totalBillAmount.isNotEmpty() && people.isNotEmpty()) {
        val totalPeopleAmounts = BillCalculator.grandTotal(people, sharedExpenses, serviceCharge)
        val remainingAmount = totalBillValue - totalPeopleAmounts
        if (abs(remainingAmount) > 0.01) {
            sb.append("\n📊 Оставшаяся сумма: ${formatMoney(remainingAmount)}₸\n")
        }
    }

    return sb.toString()
}
