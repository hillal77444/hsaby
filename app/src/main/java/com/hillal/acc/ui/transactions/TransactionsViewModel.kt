package com.hillal.acc.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hillal.acc.App
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.room.AccountTransactionCount
import com.hillal.acc.data.room.AccountBalanceByCurrency
import com.hillal.acc.data.repository.TransactionRepository
import java.util.stream.Collectors

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val transactions = MutableLiveData<MutableList<Transaction>?>()
    private var startDate: Long = 0
    private var endDate: Long = 0

    init {
        repository = (application as App).getTransactionRepository()
        // إزالة loadAllTransactions() - لا نحتاج لجلب جميع المعاملات
    }

    fun getTransactions(): LiveData<MutableList<Transaction>?> {
        return transactions
    }

    // إزالة loadAllTransactions() - غير محسن
    // fun loadAllTransactions() {
    //     repository.getAllTransactions()
    //         .observeForever(Observer { value: MutableList<Transaction?>? ->
    //             transactions.setValue(value?.filterNotNull()?.toMutableList())
    //         })
    // }

    // تحسين loadTransactionsByAccount لاستخدام استعلام محسن
    fun loadTransactionsByAccount(accountId: Long) {
        repository.getTransactionsByAccount(accountId)
            .observeForever(Observer { value: MutableList<Transaction?>? ->
                transactions.setValue(value?.filterNotNull()?.toMutableList())
            })
    }

    // تحسين loadTransactionsByDateRange - محسن بالفعل
    fun loadTransactionsByDateRange(startDate: Long, endDate: Long) {
        this.startDate = startDate
        this.endDate = endDate
        repository.getTransactionsByDateRange(startDate, endDate)
            .observeForever(Observer { value: MutableList<Transaction?>? ->
                transactions.setValue(value?.filterNotNull()?.toMutableList())
            })
    }

    // تحسين loadTransactionsByCurrency لاستخدام استعلام محسن
    fun loadTransactionsByCurrency(currency: String) {
        // استخدام استعلام محسن بدلاً من جلب جميع المعاملات
        repository.getTransactionsByCurrency(currency)
            .observeForever(Observer { value: MutableList<Transaction?>? ->
                transactions.setValue(value?.filterNotNull()?.toMutableList())
            })
    }

    // دالة محسنة للحصول على المعاملات حسب الصندوق
    fun loadTransactionsByCashbox(cashboxId: Long) {
        repository.getTransactionsByCashbox(cashboxId)
            .observeForever(Observer { value: MutableList<Transaction?>? ->
                transactions.setValue(value?.filterNotNull()?.toMutableList())
            })
    }

    fun deleteTransaction(transaction: Transaction?) {
        repository.delete(transaction)
        // إعادة تحميل المعاملات حسب النطاق الحالي
        if (startDate > 0 && endDate > 0) {
            loadTransactionsByDateRange(startDate, endDate)
        }
    }

    fun updateTransaction(transaction: Transaction?) {
        repository.update(transaction)
        // إعادة تحميل المعاملات حسب النطاق الحالي
        if (startDate > 0 && endDate > 0) {
            loadTransactionsByDateRange(startDate, endDate)
        }
    }

    fun insertTransaction(transaction: Transaction?) {
        repository.insert(transaction)
        // إعادة تحميل المعاملات حسب النطاق الحالي
        if (startDate > 0 && endDate > 0) {
            loadTransactionsByDateRange(startDate, endDate)
        }
    }

    fun getTransactionById(id: Long): LiveData<Transaction?>? {
        return repository.getTransactionById(id)
    }

    // تحسين filterTransactionsByCurrency لاستخدام استعلام محسن
    fun filterTransactionsByCurrency(currency: String) {
        // استخدام استعلام محسن بدلاً من الفلترة في الذاكرة
        loadTransactionsByCurrency(currency)
    }

    fun searchTransactionsByDescription(query: String?): LiveData<MutableList<Transaction?>?>? {
        return repository.searchTransactionsByDescription(query)
    }

    // إضافة دوال محسنة
    fun getAccountsTransactionCount(): LiveData<List<AccountTransactionCount>> {
        return repository.getAccountsTransactionCount()
    }

    fun getAllAccountsBalancesByCurrency(): LiveData<List<AccountBalanceByCurrency>> {
        return repository.getAllAccountsBalancesByCurrency()
    }

    // تحسين حساب الأرصدة
    val accountBalancesMapOptimized: LiveData<Map<Long, Map<String, Double>>>
        get() {
            val balancesLiveData = MutableLiveData<Map<Long, Map<String, Double>>>()
            getAllAccountsBalancesByCurrency().observeForever { balancesList ->
                val balancesMap = mutableMapOf<Long, MutableMap<String, Double>>()
                balancesList?.forEach { balance ->
                    if (!balancesMap.containsKey(balance.accountId)) {
                        balancesMap[balance.accountId] = mutableMapOf()
                    }
                    balancesMap[balance.accountId]!![balance.currency] = balance.balance
                }
                balancesLiveData.postValue(balancesMap)
            }
            return balancesLiveData
        }

    // دالة التوافق مع الملفات القديمة
    val accountBalancesMap: LiveData<MutableMap<Long?, MutableMap<String?, Double?>?>?>
        get() {
            val balancesLiveData = MutableLiveData<MutableMap<Long?, MutableMap<String?, Double?>?>?>()
            getAllAccountsBalancesByCurrency().observeForever { balancesList ->
                val balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?> = HashMap()
                balancesList?.forEach { balance ->
                    if (!balancesMap.containsKey(balance.accountId)) {
                        balancesMap[balance.accountId] = HashMap()
                    }
                    balancesMap[balance.accountId]!![balance.currency] = balance.balance
                }
                balancesLiveData.postValue(balancesMap)
            }
            return balancesLiveData
        }

    fun searchTransactionsByDescription(query: String?): LiveData<MutableList<Transaction?>?>? {
        return repository.searchTransactionsByDescription(query)
    }
}