package com.hillal.acc.ui.transactions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.preferences.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationUtils {
    public static String buildWhatsAppMessage(Context context, String accountName, Transaction transaction, double balance, String type) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
        String date = dateFormat.format(new Date(transaction.getTransactionDate()));
        String typeMessage;
        if ("credit".equalsIgnoreCase(type)) {
            typeMessage = "قيدنا إلى حسابكم";
        } else if ("debit".equalsIgnoreCase(type)) {
            typeMessage = "قيد على حسابكم";
        } else {
            typeMessage = "تفاصيل القيد المحاسبي";
        }
        String balanceMessage;
        if (balance >= 0) {
            balanceMessage = String.format(Locale.ENGLISH, "الرصيد لكم: %.2f %s", balance, transaction.getCurrency());
        } else {
            balanceMessage = String.format(Locale.ENGLISH, "الرصيد عليكم: %.2f %s", Math.abs(balance), transaction.getCurrency());
        }
        String userName = new UserPreferences(context).getUserName();
        return String.format(Locale.ENGLISH,
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
        );
    }

    public static void sendWhatsAppMessage(Context context, String phoneNumber, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message);
            intent.setData(Uri.parse(url));
            Intent chooser = Intent.createChooser(intent, "اختر تطبيق واتساب");
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "حدث خطأ أثناء فتح واتساب", Toast.LENGTH_SHORT).show();
        }
    }

    public static void sendSmsMessage(Context context, String phoneNumber, String message) {
        try {
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + phoneNumber));
            smsIntent.putExtra("sms_body", message);
            context.startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(context, "حدث خطأ أثناء إرسال الرسالة النصية", Toast.LENGTH_SHORT).show();
        }
    }
} 