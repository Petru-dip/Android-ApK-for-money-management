package com.example.expensetracker

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.expensetracker.Settings.isAutoSaveOn
import java.lang.Double
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import kotlin.Exception
import kotlin.String

class PaymentNotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.getPackageName() ?: return
        val lowers = pkg.lowercase()
        if (!(lowers.contains("revolut") || lowers.contains("walletnfcrel") || lowers.contains("btpay"))) return

        val e = sbn.getNotification().extras
        val titleCs = e.getCharSequence(Notification.EXTRA_TITLE)
        val textCs = e.getCharSequence(Notification.EXTRA_TEXT)
        val title = if (titleCs != null) titleCs.toString() else ""
        val text = if (textCs != null) textCs.toString() else ""
        val combined = "$title $text"
        val combinedLower = combined.lowercase(Locale.ROOT)

        if (isPromotionalNoise(combinedLower)) return
        if (isRequestOnly(combinedLower)) return
        if (isNonPaymentInfo(combinedLower)) return

        var amount = extractAmount(title, text)
        var currency = extractCurrency(title, text)
        var merchant = extractMerchant(title, text)
        var category = guessCategoryFromMerchant(merchant)

        // semn venit/cheltuială
        val isIncome = isIncomeNotification(combinedLower, amount)

        // Dacă nu avem sumă validă, nu salvăm nimic (evităm fallback 1.01)
        if (Double.isNaN(amount) || amount <= 0) {
            return
        }

        if (currency == null || currency.isEmpty()) currency = "RON"
        if (merchant == null) merchant = ""
        if (category == null) category = ""

        val autoOn = isAutoSaveOn(this)

        if (autoOn) {
            if (isIncome) {
                val inc = Income()
                inc.amount = kotlin.math.abs(amount)
                inc.description = if (merchant.isEmpty()) "Auto" else merchant
                inc.date = System.currentTimeMillis()
                inc.category = category
                inc.categoryType = pickIncomeType(combinedLower)
                inc.uid = UUID.randomUUID().toString()
                Thread {
                    AppDatabase.getInstance(applicationContext).incomeDao().insert(inc)
                    showSavedNotification(
                        "Venit salvat",
                        String.format(Locale.ROOT, "%.2f %s – %s", inc.amount, currency, inc.description)
                    )
                }.start()

            } else {
                val ex = Expense()
                ex.amount = kotlin.math.abs(amount)
                ex.description = if (merchant.isEmpty()) "Auto" else merchant
                ex.date = System.currentTimeMillis()
                ex.category = if (category.isEmpty()) guessCategoryFromText("$title $text") ?: "AUTO" else category
                ex.categoryType = "PERSONAL"
                ex.uid = UUID.randomUUID().toString()
                Thread {
                    AppDatabase.getInstance(applicationContext).expenseDao().insert(ex)
                    showSavedNotification(
                        "Cheltuială salvată",
                        String.format(Locale.ROOT, "%.2f %s – %s", ex.amount, currency, ex.description)
                    )
                }.start()
            }
        } else {
            val target = if (isIncome) AddIncomeActivity::class.java else AddExpenseActivity::class.java
            val i = Intent(this, target)
            if (!isIncome) i.setAction(ACTION_EXPENSE_FROM_NOTIFICATION)
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            i.putExtra(EXTRA_AMOUNT, amount)
            i.putExtra(EXTRA_CURRENCY, currency)
            i.putExtra(EXTRA_MERCHANT, merchant)
            i.putExtra(EXTRA_CATEGORY, category)
            i.putExtra(EXTRA_SOURCE, "revolut")
            startActivity(i)
        }
    }

    // ---------- Helperi de parsare -----------
    private fun extractAmount(vararg texts: String?): kotlin.Double {
        val numberRegex = Regex("(-?\\d+[\\.,]\\d{1,2}|-?\\d+)")
        val currencyRegex = Regex("(RON|LEI|EUR|USD|€|\\$)", RegexOption.IGNORE_CASE)
        // încearcă să găsești un număr lipit de monedă
        for (t in texts) {
            if (t == null) continue
            val cleaned = t.replace("\u00A0", " ")
            val parts = cleaned.split(" ")
            for (i in parts.indices) {
                val part = parts[i]
                if (currencyRegex.containsMatchIn(part)) {
                    // caută număr în stânga sau dreapta
                    if (i > 0 && numberRegex.containsMatchIn(parts[i - 1])) {
                        return parts[i - 1].replace(",", ".").filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
                            ?: kotlin.Double.NaN
                    }
                    if (i + 1 < parts.size && numberRegex.containsMatchIn(parts[i + 1])) {
                        return parts[i + 1].replace(",", ".").filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
                            ?: kotlin.Double.NaN
                    }
                }
            }
        }
        // fallback: primul număr găsit
        for (t in texts) {
            if (t == null) continue
            val m = numberRegex.find(t.replace("\u00A0", " "))
            if (m != null) {
                return try {
                    m.groupValues[1].replace(',', '.').toDouble()
                } catch (ignored: Exception) {
                    kotlin.Double.NaN
                }
            }
        }
        return kotlin.Double.NaN
    }

    private fun extractCurrency(vararg texts: String?): String? {
        for (t in texts) {
            if (t == null) continue
            val s = t.uppercase()
            if (s.contains("RON") || s.contains(" LEI") || s.contains("LEI ")) return "RON"
            if (s.contains("EUR") || s.contains("€")) return "EUR"
            if (s.contains("USD") || s.contains("$")) return "USD"
        }
        return null
    }

    private fun extractMerchant(vararg texts: String?): String? {
        for (t in texts) {
            if (t == null) continue
            val cleaned =
                t.replace("(-?\\d+[\\.,]\\d{1,2}|-?\\d+)\\s*(RON|LEI|EUR|USD|€|\\$)?".toRegex(), "")
                    .trim { it <= ' ' }
            if (!cleaned.isEmpty()) return cleaned
        }
        return null
    }

    private fun guessCategoryFromMerchant(merchant: String?): String? {
        if (merchant == null) return null
        val m = merchant.lowercase()
        if (m.contains("mega") || m.contains("carrefour") || m.contains("kaufland") || m.contains("lidl")) return "Supermarket"
        if (m.contains("uber") || m.contains("bolt")) return "Transport"
        if (m.contains("mc") || m.contains("kfc") || m.contains("pizza")) return "Mancare"
        return null
    }

    private fun guessCategoryFromText(text: String): String? {
        val t = text.lowercase()
        if (Regex("(?i)\\b(mâncare|mancare|supermarket|restaurant|pizza|mc|kfc|lidl|kaufland|carrefour)\\b").containsMatchIn(t)) return "Mâncare"
        if (Regex("(?i)\\b(farmacie|spital|sanatate|sănătate)\\b").containsMatchIn(t)) return "Sănătate"
        if (Regex("(?i)\\b(uber|bolt|transport|taxi|bilet)\\b").containsMatchIn(t)) return "Transport"
        if (Regex("(?i)\\b(chirie|utilități|gaz|curent|apa|casă|casa)\\b").containsMatchIn(t)) return "Casă"
        return null
    }

    private fun pickIncomeType(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("salariu") || t.contains("salary") -> "Salariu"
            t.contains("bonus") -> "Bonus"
            t.contains("familie") || t.contains("family") -> "Familie"
            t.contains("primit") || t.contains("received") || t.contains("transfer") -> "Primit"
            else -> "Altele"
        }
    }

    private fun isIncomeNotification(text: String, amount: kotlin.Double): Boolean {
        val incomeRegex = Regex("(?i)\\b(received|primit|salary|salariu|bonus|refund|ramburs|incasare|încasare|creditat|sent you|ti-a trimis|tiau trimis|ti-a transferat|transferat|incoming|depunere|deposit)\\b")
        val expenseRegex = Regex("(?i)\\b(plata|payment|achitat|debitat|purchase|retragere|withdraw|cheltuială|cheltuiala|card|visa|mastercard|mc|kaufland|lidl|carrefour|mega|bolt|uber)\\b")
        if (incomeRegex.containsMatchIn(text)) return true
        if (expenseRegex.containsMatchIn(text)) return false
        // dacă avem o categorie ghicită de supermarket, tratăm ca expense
        if (guessCategoryFromText(text) != null) return false
        if (guessCategoryFromMerchant(text) != null) return false
        // fallback: dacă există semn plus/minus în text
        if (text.contains("-")) return false
        if (text.contains("+")) return true
        // default conservator: cheltuială
        return false
    }

    private fun isPromotionalNoise(text: String): Boolean {
        val promoWords = listOf("invita", "invite", "prieten", "friend", "referral", "recomand", "promo", "campanie", "câștigi", "castigi", "win", "voucher")
        val hasInviteFlow = (text.contains("invita") || text.contains("invite") || text.contains("refer")) &&
                (text.contains("prieten") || text.contains("friend"))
        val hasPromo = promoWords.count { text.contains(it) } >= 2
        return hasInviteFlow || hasPromo
    }

    private fun isRequestOnly(text: String): Boolean {
        val req = Regex("(?i)\\b(request|requested|cerere|cerut|solicitat|has requested|asks for|asking for)\\b")
        return req.containsMatchIn(text)
    }

    private fun isNonPaymentInfo(text: String): Boolean {
        val keywords = listOf(
            "revpoints", "points", "puncte", "reward", "recompens",
            "welcome", "bine ai venit",
            "declined", "decline", "respins", "refuzat",
            "failed", "anulat", "cancelled"
        )
        return keywords.any { text.contains(it) }
    }

    private fun showSavedNotification(title: String?, text: String?) {
        val chId = "auto_save"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    chId,
                    "Auto-save",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val nb = NotificationCompat.Builder(this, chId)
            .setSmallIcon(R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
        nm.notify(System.currentTimeMillis().toInt(), nb.build())
    }

    companion object {
        const val ACTION_EXPENSE_FROM_NOTIFICATION: String =
            "com.example.expensetracker.ACTION_EXPENSE_FROM_NOTIFICATION"
        const val EXTRA_AMOUNT: String = "extra_amount"
        const val EXTRA_CURRENCY: String = "extra_currency"
        const val EXTRA_MERCHANT: String = "extra_merchant"
        const val EXTRA_CATEGORY: String = "extra_category"
        const val EXTRA_SOURCE: String = "extra_source"
    }
}
