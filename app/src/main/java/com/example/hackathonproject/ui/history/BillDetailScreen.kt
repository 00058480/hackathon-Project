package com.example.hackathonproject.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hackathonproject.AppViewModelProvider
import com.example.hackathonproject.data.local.BillPersonEntity
import com.example.hackathonproject.data.local.BillSharedExpenseEntity
import com.example.hackathonproject.ui.util.formatDateTime
import com.example.hackathonproject.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    billId: Long,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val billFlow = remember(billId) { viewModel.bill(billId) }
    val details by billFlow.collectAsStateWithLifecycle(initialValue = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    val current = details

    if (showDeleteDialog && current != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить счёт?") },
            text = { Text("«${current.bill.title}» будет удалён без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(current.bill)
                    showDeleteDialog = false
                    onBack()
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.bill?.title ?: "Счёт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (current != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                createdAt = current.bill.createdAt,
                totalAmount = current.bill.totalAmount,
                serviceChargePercent = current.bill.serviceChargePercent,
                grandTotal = current.bill.grandTotal
            )

            if (current.sharedExpenses.isNotEmpty()) {
                Text(text = "Общие расходы", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                current.sharedExpenses.forEach { SharedExpenseRow(it) }
            }

            Text(text = "Итоги по людям", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            current.people.forEach { PersonResultCard(it) }
        }
    }
}

@Composable
private fun SummaryCard(
    createdAt: Long,
    totalAmount: Double,
    serviceChargePercent: Double,
    grandTotal: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatDateTime(createdAt),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            SummaryLine("Сумма чека", "${formatMoney(totalAmount)}₸")
            SummaryLine("Обслуживание", "${formatMoney(serviceChargePercent)}%")
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Итого",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${formatMoney(grandTotal)}₸",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(text = value, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun SharedExpenseRow(expense: BillSharedExpenseEntity) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "на ${expense.participantCount} чел.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = "${formatMoney(expense.amount)}₸", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PersonResultCard(person: BillPersonEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = person.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "${formatMoney(person.total)}₸",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            if (person.personalAmount > 0) {
                DetailLine("Заказ", person.personalAmount)
            }
            if (person.sharedAmount > 0) {
                DetailLine("Общие расходы", person.sharedAmount)
            }
            if (person.serviceChargeAmount > 0) {
                DetailLine("Обслуживание", person.serviceChargeAmount)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: Double) {
    Text(
        text = "$label: ${formatMoney(value)}₸",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onTertiaryContainer
    )
}
