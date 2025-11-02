package com.example.hackathonproject

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathonproject.ui.theme.HackathonProjectTheme

data class Person(
    val id: Int,
    val name: String,
    val personalAmount: String
)

data class SharedExpense(
    val id: Int,
    val name: String,
    val amount: String,
    val participants: Set<Int>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterScreen(modifier: Modifier = Modifier) {
    var totalBillAmount by remember { mutableStateOf("") }
    var serviceChargePercent by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<Person>>(emptyList()) }
    var sharedExpenses by remember { mutableStateOf<List<SharedExpense>>(emptyList()) }
    var nextPersonId by remember { mutableStateOf(0) }
    var nextExpenseId by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
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
                    value = totalBillAmount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) {
                            totalBillAmount = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Сумма") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    suffix = { Text("₸") }
                )
            }
        }

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
                    value = serviceChargePercent,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) {
                            serviceChargePercent = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Процент") },
                    placeholder = { Text("10") },
                    singleLine = true,
                    suffix = { Text("%") }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Общие расходы (чай, сок)",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = {
                    sharedExpenses = sharedExpenses + SharedExpense(
                        id = nextExpenseId++,
                        name = "",
                        amount = "",
                        participants = emptySet()
                    )
                },
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        }

        sharedExpenses.forEachIndexed { index, expense ->
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
                        Text(
                            text = "Общий расход ${index + 1}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = {
                                sharedExpenses = sharedExpenses.filter { it.id != expense.id }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    OutlinedTextField(
                        value = expense.name,
                        onValueChange = { newName ->
                            sharedExpenses = sharedExpenses.map {
                                if (it.id == expense.id) it.copy(name = newName) else it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Название (чай, сок)") },
                        placeholder = { Text("Чай") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expense.amount,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) {
                                sharedExpenses = sharedExpenses.map {
                                    if (it.id == expense.id) it.copy(amount = newValue) else it
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Сумма") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        suffix = { Text("₸") }
                    )
                    if (people.isNotEmpty()) {
                        Text(
                            text = "Кто участвует:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        people.forEach { person ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = expense.participants.contains(person.id),
                                    onCheckedChange = { checked ->
                                        sharedExpenses = sharedExpenses.map {
                                            if (it.id == expense.id) {
                                                if (checked) {
                                                    it.copy(participants = it.participants + person.id)
                                                } else {
                                                    it.copy(participants = it.participants - person.id)
                                                }
                                            } else it
                                        }
                                    }
                                )
                                Text(
                                    text = person.name.ifEmpty { "Человек ${people.indexOf(person) + 1}" },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Люди и их заказы",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = {
                    people = people + Person(
                        id = nextPersonId++,
                        name = "",
                        personalAmount = ""
                    )
                },
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        }

        people.forEachIndexed { index, person ->
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
                        Text(
                            text = "Человек ${index + 1}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = {
                                people = people.filter { it.id != person.id }
                                sharedExpenses = sharedExpenses.map { expense ->
                                    expense.copy(participants = expense.participants - person.id)
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    OutlinedTextField(
                        value = person.name,
                        onValueChange = { newName ->
                            people = people.map {
                                if (it.id == person.id) it.copy(name = newName) else it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Имя") },
                        placeholder = { Text("Введите имя") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = person.personalAmount,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) {
                                people = people.map {
                                    if (it.id == person.id) it.copy(personalAmount = newValue) else it
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Сумма заказа") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        suffix = { Text("₸") }
                    )
                }
            }
        }

        if (totalBillAmount.isNotEmpty() && people.isNotEmpty()) {
            val totalBillValue = totalBillAmount.toDoubleOrNull() ?: 0.0
            val serviceCharge = serviceChargePercent.toDoubleOrNull() ?: 0.0
            
            val totalPeopleAmounts = people.sumOf { person ->
                val personalAmount = person.personalAmount.toDoubleOrNull() ?: 0.0
                
                var sharedExpensesTotal = 0.0
                sharedExpenses.forEach { expense ->
                    if (expense.participants.contains(person.id) && expense.participants.isNotEmpty()) {
                        val expenseAmount = expense.amount.toDoubleOrNull() ?: 0.0
                        sharedExpensesTotal += expenseAmount / expense.participants.size
                    }
                }
                
                val subtotal = personalAmount + sharedExpensesTotal
                val serviceChargeAmount = subtotal * serviceCharge / 100.0
                subtotal + serviceChargeAmount
            }
            
            val remainingAmount = totalBillValue - totalPeopleAmounts

            if (remainingAmount != 0.0) {
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
                            text = "${String.format("%.2f", remainingAmount)}₸",
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

        if (people.isNotEmpty()) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Итоги",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Button(
                            onClick = {
                                val shareText = formatShareText(
                                    totalBillAmount,
                                    serviceChargePercent,
                                    people,
                                    sharedExpenses
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Поделиться итогами"))
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Поделиться")
                        }
                    }

                    val serviceCharge = serviceChargePercent.toDoubleOrNull() ?: 0.0

                    people.forEach { person ->
                        val personalAmount = person.personalAmount.toDoubleOrNull() ?: 0.0
                        
                        var sharedExpensesTotal = 0.0
                        sharedExpenses.forEach { expense ->
                            if (expense.participants.contains(person.id) && expense.participants.isNotEmpty()) {
                                val expenseAmount = expense.amount.toDoubleOrNull() ?: 0.0
                                sharedExpensesTotal += expenseAmount / expense.participants.size
                            }
                        }

                        val subtotal = personalAmount + sharedExpensesTotal
                        val serviceChargeAmount = subtotal * serviceCharge / 100.0
                        val total = subtotal + serviceChargeAmount

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = person.name.ifEmpty { "Человек ${people.indexOf(person) + 1}" },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format("%.2f", total)}₸",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (personalAmount > 0) {
                                Text(
                                    text = "Заказ: ${String.format("%.2f", personalAmount)}₸",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (sharedExpensesTotal > 0) {
                                Text(
                                    text = "Общие расходы: ${String.format("%.2f", sharedExpensesTotal)}₸",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (serviceChargeAmount > 0) {
                                Text(
                                    text = "Обслуживание: ${String.format("%.2f", serviceChargeAmount)}₸",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (person != people.last()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

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
        sb.append("📋 Общая сумма чека: ${String.format("%.2f", totalBillValue)}₸\n")
    }
    
    if (serviceChargePercent.isNotEmpty() && serviceCharge > 0) {
        sb.append("💼 Процент обслуживания: ${String.format("%.1f", serviceCharge)}%\n")
    }
    
    if (sharedExpenses.isNotEmpty()) {
        sb.append("\n🍵 Общие расходы:\n")
        sharedExpenses.forEach { expense ->
            val amount = expense.amount.toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                val participants = people.filter { expense.participants.contains(it.id) }
                val participantNames = if (participants.isNotEmpty()) {
                    participants.joinToString(", ") { it.name.ifEmpty { "Человек ${people.indexOf(it) + 1}" } }
                } else {
                    "Не выбраны"
                }
                sb.append("  • ${expense.name.ifEmpty { "Общий расход" }}: ${String.format("%.2f", amount)}₸ (${participantNames})\n")
            }
        }
    }
    
    sb.append("\n👥 Итоги по людям:\n")
    sb.append("─────────────────────\n")
    
    people.forEach { person ->
        val personalAmount = person.personalAmount.toDoubleOrNull() ?: 0.0
        
        var sharedExpensesTotal = 0.0
        val personSharedExpenses = mutableListOf<String>()
        sharedExpenses.forEach { expense ->
            if (expense.participants.contains(person.id) && expense.participants.isNotEmpty()) {
                val expenseAmount = expense.amount.toDoubleOrNull() ?: 0.0
                val share = expenseAmount / expense.participants.size
                sharedExpensesTotal += share
                if (share > 0) {
                    personSharedExpenses.add("${expense.name.ifEmpty { "Общий расход" }}: ${String.format("%.2f", share)}₸")
                }
            }
        }
        
        val subtotal = personalAmount + sharedExpensesTotal
        val serviceChargeAmount = subtotal * serviceCharge / 100.0
        val total = subtotal + serviceChargeAmount
        
        val personName = person.name.ifEmpty { "Человек ${people.indexOf(person) + 1}" }
        sb.append("\n${personName} -- ${String.format("%.2f", total)}₸\n")
        
        if (personalAmount > 0) {
            sb.append("  Заказ: ${String.format("%.2f", personalAmount)}₸\n")
        }
        
        if (personSharedExpenses.isNotEmpty()) {
            personSharedExpenses.forEach { expense ->
                sb.append("  $expense\n")
            }
        }
        
        if (serviceChargeAmount > 0) {
            sb.append("  Обслуживание: ${String.format("%.2f", serviceChargeAmount)}₸\n")
        }
    }
    
    if (totalBillAmount.isNotEmpty() && people.isNotEmpty()) {
        val totalPeopleAmounts = people.sumOf { person ->
            val personalAmount = person.personalAmount.toDoubleOrNull() ?: 0.0
            
            var sharedExpensesTotal = 0.0
            sharedExpenses.forEach { expense ->
                if (expense.participants.contains(person.id) && expense.participants.isNotEmpty()) {
                    val expenseAmount = expense.amount.toDoubleOrNull() ?: 0.0
                    sharedExpensesTotal += expenseAmount / expense.participants.size
                }
            }
            
            val subtotal = personalAmount + sharedExpensesTotal
            val serviceChargeAmount = subtotal * serviceCharge / 100.0
            subtotal + serviceChargeAmount
        }
        
        val remainingAmount = totalBillValue - totalPeopleAmounts
        if (remainingAmount != 0.0) {
            sb.append("\n📊 Оставшаяся сумма: ${String.format("%.2f", remainingAmount)}₸\n")
        }
    }
    
    return sb.toString()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BillSplitterScreenPreview() {
    HackathonProjectTheme {
        BillSplitterScreen()
    }
}
