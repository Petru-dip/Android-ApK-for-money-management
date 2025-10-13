package com.example.expensetracker

import java.util.Locale
import kotlin.math.abs

data class TransactionEntity(
    val transactionId: String,
    val amount: Double,
    val currency: String,
    val description: String?,
    val merchant: String?,
    val bookingDate: String,
    val createdAt: String,
)

object NotificationParser {

    private enum class Source { REVOLUT, GOOGLE_PAY, BT_PAY, OTHER }

    private fun sourceFromPkg(pkg: String): Source = when {
        pkg.contains("revolut", true) -> Source.REVOLUT
        pkg.contains("walletnfcrel", true) -> Source.GOOGLE_PAY
        pkg.contains("btpay", true) -> Source.BT_PAY
        else -> Source.OTHER
    }

    private val amountRegex = Regex("""([-+]?\d{1,4}(?:[.,]\d{1,2})?)""")
    private val currencyRegex = Regex("""(?i)\b(RON|LEI|EUR|USD|GBP)\b""")
    private val merchantAfterAt = Regex("""(?i)\b(?:la|at|către|to)\s+([A-Z0-9][A-Za-z0-9 '\-&._]+)""")

    private val revSpent1 = Regex("""(?i)\b(you\s+spent|plată|payment)\b.*?${amountRegex.pattern}\s*(RON|LEI|EUR|USD|GBP)?""")
    private val revCardChg = Regex("""(?i)\b(card\s+(?:charge|charged)|ai\s+plătit|s-a\s+debita?t)\b.*?${amountRegex.pattern}\s*(RON|LEI|EUR|USD|GBP)?""")
    private val gwPay      = Regex("""(?i)\b(payment\s+(?:made|completed)|purchase)\b.*?${amountRegex.pattern}\s*(RON|LEI|EUR|USD|GBP)?""")
    private val btPay      = Regex("""(?i)\b(ai\s+plătit|plată\s+cu\s+cardul)\b.*?${amountRegex.pattern}\s*(RON|LEI|EUR|USD|GBP)?""")
    private val genericAmount = Regex("""(?i)${amountRegex.pattern}\s*(RON|LEI|EUR|USD|GBP)?""")

    fun parse(appPkg: String, title: String = "", body: String): TransactionEntity? {
        val src = sourceFromPkg(appPkg)
        val text = (title + " • " + body).trim()

        val (amt, ccy) = when (src) {
            Source.REVOLUT   -> firstAmountAndCcy(text, revSpent1, revCardChg, genericAmount)
            Source.GOOGLE_PAY-> firstAmountAndCcy(text, gwPay, genericAmount)
            Source.BT_PAY    -> firstAmountAndCcy(text, btPay, genericAmount)
            Source.OTHER     -> firstAmountAndCcy(text, genericAmount)
        } ?: return null

        val merchant = extractMerchant(text)

        val nowDate = java.time.LocalDate.now().toString()
        val nowIso  = java.time.OffsetDateTime.now().toString()

        val sign = if (Regex("(?i)refund|returnat|ramburs").containsMatchIn(text)) +1 else -1
        val idSeed = "${appPkg}:${nowIso}:${abs(text.hashCode())}"

        return TransactionEntity(
            transactionId = "notif_$idSeed",
            amount = sign * amt,
            currency = ccy,
            description = text.take(240),
            merchant = merchant,
            bookingDate = nowDate,
            createdAt = nowIso,
        )
    }

    private fun firstAmountAndCcy(text: String, vararg patterns: Regex): Pair<Double, String>? {
        patterns.forEach { rx ->
            val m = rx.find(text) ?: return@forEach
            val a = amountRegex.find(m.value)?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
            if (a != null) {
                val c = currencyRegex.find(m.value)?.groupValues?.getOrNull(1)?.uppercase(Locale.ROOT) ?: "RON"
                return a to c
            }
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        val m1 = merchantAfterAt.find(text)?.groupValues?.getOrNull(1)?.trim()
        if (!m1.isNullOrBlank()) return m1
        val m2 = Regex("""(?i)(?:RON|LEI|EUR|USD|GBP)\s+[^\w]?([A-Z][A-Za-z0-9 '&._-]{2,})""").find(text)
            ?.groupValues?.getOrNull(1)?.trim()
        return m2
    }
}
