package com.hillal.acc.ui.transactions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.preferences.UserPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object NotificationUtils {
    @JvmStatic
    fun buildWhatsAppMessage(
        context: Context,
        accountName: String,
        transaction: Transaction,
        balance: Double,
        type: String?
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val date = dateFormat.format(Date(transaction.getTransactionDate()))
        val typeMessage: String?
        if ("credit".equals(type, ignoreCase = true)) {
            typeMessage = "قيدنا إلى حسابكم"
        } else if ("debit".equals(type, ignoreCase = true)) {
            typeMessage = "قيد على حسابكم"
        } else {
            typeMessage = "تفاصيل القيد المحاسبي"
        }
        val balanceMessage: String?
        if (balance >= 0) {
            balanceMessage = String.format(
                Locale.ENGLISH,
                "الرصيد لكم: %.2f %s",
                balance,
                transaction.getCurrency()
            )
        } else {
            balanceMessage = String.format(
                Locale.ENGLISH,
                "الرصيد عليكم: %.2f %s",
                abs(balance),
                transaction.getCurrency()
            )
        }
        val userName = UserPreferences(context).getUserName()
        return String.format(
            Locale.ENGLISH,
            "السيد / *%s*\n" +
                    "──────────────\n" +
                    "%s\n" +
                    "──────────────\n" +
                    "التاريخ: %s\n" +
                    "المبلغ: %.2f %s\n" +
                    "البيان: %s\n" +
                    "%s\n" +
                    "──────────────\n" +
                    "تم الإرسال بواسطة:\n*%s*",
            accountName,
            typeMessage,
            date,
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getDescription(),
            balanceMessage,
            userName
        )
    }

    @JvmStatic
    fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url =
                "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message)
            intent.setData(Uri.parse(url))
            val chooser = Intent.createChooser(intent, "اختر تطبيق واتساب")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء فتح واتساب", Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun sendSmsMessage(context: Context, phoneNumber: String?, message: String?) {
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.setData(Uri.parse("smsto:" + phoneNumber))
            smsIntent.putExtra("sms_body", message)
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء إرسال الرسالة النصية", Toast.LENGTH_SHORT).show()
        }
    }
}