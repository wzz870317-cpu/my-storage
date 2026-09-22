package com.example.cashflow

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashflow.data.AppDatabase
import com.example.cashflow.data.TransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.get(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            CashFlowViewModel.Factory(database)
        )[CashFlowViewModel::class.java]

        setContent {
            MaterialTheme {
                CashFlowScreen(
                    viewModel = viewModel,
                    onOpenNotificationSettings = {
                        startActivity(
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        )
                    }
                )
            }
        }
    }
}

class CashFlowViewModel(
    database: AppDatabase
) : ViewModel() {

    private val dao = database.transactionDao()

    private val monthRange: Pair<Long, Long>
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val start = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val end = calendar.timeInMillis

            return start to end
        }

    val transactions: StateFlow<List<TransactionEntity>> =
        dao.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val income: StateFlow<Long> = dao.observeIncome(
        monthRange.first,
        monthRange.second
    ).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0L
    )

    val expense: StateFlow<Long> = dao.observeExpense(
        monthRange.first,
        monthRange.second
    ).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0L
    )

    class Factory(
        private val database: AppDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CashFlowViewModel(database) as T
        }
    }
}

@androidx.compose.runtime.Composable
fun CashFlowScreen(
    viewModel: CashFlowViewModel,
    onOpenNotificationSettings: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val income by viewModel.income.collectAsState()
    val expense by viewModel.expense.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "收支统计",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                title = "本月收入",
                amountFen = income,
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "本月支出",
                amountFen = expense,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "结余：¥${formatMoney(income - expense)}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onOpenNotificationSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开启微信/支付宝通知读取权限")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "最近交易",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = transactions,
                key = { it.id }
            ) { transaction ->
                TransactionItem(transaction)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SummaryCard(
    title: String,
    amountFen: Long,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "¥${formatMoney(amountFen)}",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun TransactionItem(transaction: TransactionEntity) {
    val sign = when (transaction.type) {
        "income" -> "+"
        "refund" -> "+"
        else -> "-"
    }

    val sourceName = when (transaction.source) {
        "wechat" -> "微信"
        "alipay" -> "支付宝"
        else -> transaction.source
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = transaction.title.ifBlank { "未知交易" },
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$sourceName · ${transaction.category}"
            )

            Text(
                text = "$sign¥${formatMoney(transaction.amountFen)} · ${
                    formatDate(transaction.transactionTime)
                }"
            )
        }
    }
}

private fun formatMoney(amountFen: Long): String {
    return "%.2f".format(
        Locale.CHINA,
        amountFen / 100.0
    )
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "MM-dd HH:mm",
        Locale.CHINA
    ).format(Date(timestamp))
}