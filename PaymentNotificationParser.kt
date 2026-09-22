package com.example.cashflow.parser

import com.example.cashflow.data.TransactionEntity
import java.math.BigDecimal
import java.util.Locale

object PaymentNotificationParser {

    private val amountRegex = Regex(
        """(?:¥|￥)\s*(\d+(?:\.\d{1,2})?)|(\d+(?:\.\d{1,2})?)\s*元""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        packageName: String,
        title: String,
        text: String,
        postedAt: Long
    ): TransactionEntity? {
        val source = when (packageName) {
            "com.tencent.mm" -> "wechat"
            "com.eg.android.AlipayGphone" -> "alipay"
            else -> return null
        }

        val content = "$title $text".trim()
        val amountMatch = amountRegex.find(content) ?: return null

        val amountText = amountMatch.groups[1]?.value
            ?: amountMatch.groups[2]?.value
            ?: return null

        val amountFen = runCatching {
            BigDecimal(amountText)
                .multiply(BigDecimal(100))
                .longValueExact()
        }.getOrNull() ?: return null

        val type = when {
            containsAny(content, "退款", "退回") -> "refund"
            containsAny(content, "收款", "到账", "收入") -> "income"
            containsAny(content, "付款", "支付", "消费", "支出") -> "expense"
            else -> return null
        }

        val category = when {
            containsAny(content, "工资", "薪资") -> "工资"
            containsAny(content, "红包", "转账") -> "转账"
            containsAny(content, "退款", "退回") -> "退款"
            containsAny(content, "打车", "地铁", "公交", "交通") -> "交通"
            containsAny(content, "餐", "美团", "饿了么", "外卖") -> "餐饮"
            containsAny(content, "购物", "淘宝", "京东", "拼多多") -> "购物"
            else -> "其他"
        }

        val uniqueKey = listOf(
            source,
            type,
            amountFen,
            postedAt / 60_000,
            content.lowercase(Locale.ROOT)
        ).joinToString("|")

        return TransactionEntity(
            source = source,
            type = type,
            amountFen = amountFen,
            merchant = null,
            title = title,
            originalText = text,
            transactionTime = postedAt,
            uniqueKey = uniqueKey,
            category = category
        )
    }

    private fun containsAny(value: String, vararg keywords: String): Boolean {
        return keywords.any { value.contains(it, ignoreCase = true) }
    }
}