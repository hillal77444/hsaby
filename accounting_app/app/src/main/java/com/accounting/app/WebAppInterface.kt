package com.accounting.app

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import com.google.gson.Gson
import java.security.MessageDigest

class WebAppInterface(private val context: MainActivity, private val dbHelper: DatabaseHelper) {
    private val TAG = "WebAppInterface"
    private val gson = Gson()

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun onPageLoaded() {
        // يمكن إضافة أي كود مطلوب عند تحميل الصفحة
    }

    @JavascriptInterface
    fun isOnline(): Boolean {
        return context.isOnline()
    }

    @JavascriptInterface
    fun refreshContent() {
        context.refreshContent()
    }

    @JavascriptInterface
    fun login(username: String, phone: String, password: String): String {
        return try {
            val user = dbHelper.getUser(username, phone)
            if (user != null && user.passwordHash == hashPassword(password)) {
                (context as MainActivity).saveLoginState(username, phone)
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم تسجيل الدخول بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "بيانات الدخول غير صحيحة"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء تسجيل الدخول"
            ))
        }
    }

    @JavascriptInterface
    fun register(username: String, phone: String, password: String): String {
        return try {
            if (dbHelper.getUser(username, phone) != null) {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "المستخدم موجود بالفعل"
                ))
            } else {
                val success = dbHelper.addUser(username, phone, hashPassword(password))
                if (success) {
                    (context as MainActivity).saveLoginState(username, phone)
                    gson.toJson(mapOf(
                        "success" to true,
                        "message" to "تم إنشاء الحساب بنجاح"
                    ))
                } else {
                    gson.toJson(mapOf(
                        "success" to false,
                        "message" to "فشل في إنشاء الحساب"
                    ))
                }
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء إنشاء الحساب"
            ))
        }
    }

    @JavascriptInterface
    fun logout(): String {
        return try {
            (context as MainActivity).clearLoginState()
            gson.toJson(mapOf(
                "success" to true,
                "message" to "تم تسجيل الخروج بنجاح"
            ))
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء تسجيل الخروج"
            ))
        }
    }

    @JavascriptInterface
    fun getAccounts(): String {
        return try {
            val accounts = dbHelper.getAllAccounts()
            gson.toJson(mapOf(
                "success" to true,
                "accounts" to accounts
            ))
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء جلب الحسابات"
            ))
        }
    }

    @JavascriptInterface
    fun addAccount(accountJson: String): String {
        return try {
            val account = gson.fromJson(accountJson, Account::class.java)
            val success = dbHelper.addAccount(account)
            if (success) {
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم إضافة الحساب بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "فشل في إضافة الحساب"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء إضافة الحساب"
            ))
        }
    }

    @JavascriptInterface
    fun updateAccount(accountJson: String): String {
        return try {
            val account = gson.fromJson(accountJson, Account::class.java)
            val success = dbHelper.updateAccount(account)
            if (success) {
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم تحديث الحساب بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "فشل في تحديث الحساب"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء تحديث الحساب"
            ))
        }
    }

    @JavascriptInterface
    fun deleteAccount(accountId: Int): String {
        return try {
            val success = dbHelper.deleteAccount(accountId)
            if (success) {
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم حذف الحساب بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "فشل في حذف الحساب"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء حذف الحساب"
            ))
        }
    }

    @JavascriptInterface
    fun getTransactions(accountId: Int): String {
        return try {
            val transactions = dbHelper.getTransactionsForAccount(accountId)
            gson.toJson(mapOf(
                "success" to true,
                "transactions" to transactions
            ))
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء جلب المعاملات"
            ))
        }
    }

    @JavascriptInterface
    fun addTransaction(transactionJson: String): String {
        return try {
            val transaction = gson.fromJson(transactionJson, Transaction::class.java)
            val success = dbHelper.addTransaction(transaction)
            if (success) {
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم إضافة المعاملة بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "فشل في إضافة المعاملة"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء إضافة المعاملة"
            ))
        }
    }

    @JavascriptInterface
    fun updateTransaction(transactionJson: String): String {
        return try {
            val transaction = gson.fromJson(transactionJson, Transaction::class.java)
            val success = dbHelper.updateTransaction(transaction)
            if (success) {
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم تحديث المعاملة بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "فشل في تحديث المعاملة"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء تحديث المعاملة"
            ))
        }
    }

    @JavascriptInterface
    fun deleteTransaction(transactionId: Int): String {
        return try {
            val success = dbHelper.deleteTransaction(transactionId)
            if (success) {
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم حذف المعاملة بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "فشل في حذف المعاملة"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء حذف المعاملة"
            ))
        }
    }

    @JavascriptInterface
    fun getSettings(): String {
        return try {
            val settings = dbHelper.getAllSettings()
            gson.toJson(mapOf(
                "success" to true,
                "settings" to settings
            ))
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء جلب الإعدادات"
            ))
        }
    }

    @JavascriptInterface
    fun updateSettings(settingsJson: String): String {
        return try {
            val settings = gson.fromJson(settingsJson, Map::class.java)
            val success = dbHelper.updateSettings(settings)
            if (success) {
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم تحديث الإعدادات بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "فشل في تحديث الإعدادات"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء تحديث الإعدادات"
            ))
        }
    }

    @JavascriptInterface
    fun syncData(): String {
        return try {
            val success = dbHelper.syncData()
            if (success) {
                gson.toJson(mapOf(
                    "success" to true,
                    "message" to "تم مزامنة البيانات بنجاح"
                ))
            } else {
                gson.toJson(mapOf(
                    "success" to false,
                    "message" to "فشل في مزامنة البيانات"
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "success" to false,
                "message" to "حدث خطأ أثناء مزامنة البيانات"
            ))
        }
    }

    private fun hashPassword(password: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
} 