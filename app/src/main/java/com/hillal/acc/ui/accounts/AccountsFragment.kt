package com.hillal.acc.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher

import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.switchmaterial.SwitchMaterial
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.viewmodel.AccountViewModel

import java.util.*

class AccountsFragment : Fragment() {
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var accountsRecyclerView: RecyclerView
    private lateinit var accountsAdapter: AccountsAdapter
    private lateinit var searchEditText: TextInputEditText
    private lateinit var filterButton: MaterialButton
    private lateinit var sortButton: MaterialButton
    private lateinit var totalAccountsText: TextView
    private lateinit var activeAccountsText: TextView
    private val accountBalances = mutableMapOf<Long, Double>()
    private var isAscendingSort = true // true = من الأصغر إلى الأكبر، false = من الأكبر إلى الأصغر
    private var currentSortType = "balance" // balance, name, date

    override fun onCreateView(
        @NonNull inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_accounts, container, false)

        // Initialize views
        accountsRecyclerView = root.findViewById(R.id.accounts_list)
        searchEditText = root.findViewById(R.id.search_edit_text)
        filterButton = root.findViewById(R.id.filterButton)
        sortButton = root.findViewById(R.id.sortButton)
        totalAccountsText = root.findViewById(R.id.totalAccountsText)
        activeAccountsText = root.findViewById(R.id.activeAccountsText)
        val addAccountButton = root.findViewById<FloatingActionButton>(R.id.fab_add_account)

        // Initialize ViewModel
        accountViewModel = ViewModelProvider(this)[AccountViewModel::class.java]

        // Setup RecyclerView
        accountsRecyclerView.layoutManager = LinearLayoutManager(context)
        accountsAdapter = AccountsAdapter(mutableListOf(), accountViewModel, viewLifecycleOwner)
        accountsRecyclerView.adapter = accountsAdapter

        // قائمة الحسابات الحالية
        val currentAccounts = mutableListOf<Account>()

        // Observe accounts data
        accountViewModel.allAccounts.observe(viewLifecycleOwner) { accounts ->
            currentAccounts.clear()
            currentAccounts.addAll(accounts)
            
            // تحديث الإحصائيات
            totalAccountsText.text = accounts.size.toString()
            val activeCount = accounts.count { it.isWhatsappEnabled }
            activeAccountsText.text = activeCount.toString()
            
            // تحديث الأرصدة
            accounts.forEach { account ->
                accountViewModel.getAccountBalanceYemeni(account.id).observe(viewLifecycleOwner) { balance ->
                    accountBalances[account.id] = balance ?: 0.0
                }
            }
            accountsAdapter.updateAccounts(accounts)
        }

        // Setup search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    accountViewModel.searchAccounts(query).observe(viewLifecycleOwner) { accounts ->
                        currentAccounts.clear()
                        currentAccounts.addAll(accounts)
                        accountsAdapter.updateAccounts(accounts)
                    }
                } else {
                    accountViewModel.allAccounts.observe(viewLifecycleOwner) { accounts ->
                        currentAccounts.clear()
                        currentAccounts.addAll(accounts)
                        accountsAdapter.updateAccounts(accounts)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup filter button
        filterButton.setOnClickListener {
            Toast.makeText(context, "سيتم إضافة خيارات التصفية قريباً", Toast.LENGTH_SHORT).show()
        }

        // Setup sort button
        sortButton.setOnClickListener { v ->
            // منع انتشار الحدث
            v.isEnabled = false
            
            val sorted = mutableListOf<Account>().apply { addAll(currentAccounts) }
            
            // تبديل نوع الترتيب
            currentSortType = when (currentSortType) {
                "balance" -> "name"
                "name" -> "number"
                "number" -> "date"
                "date" -> "balance"
                else -> "balance"
            }
            
            sortButton.text = when (currentSortType) {
                "balance" -> "ترتيب (الرصيد)"
                "name" -> "ترتيب (الاسم)"
                "number" -> "ترتيب (الرقم)"
                "date" -> "ترتيب (التاريخ)"
                else -> "ترتيب (الرصيد)"
            }
            
            // تطبيق الترتيب حسب النوع
            when (currentSortType) {
                "balance" -> {
                    if (isAscendingSort) {
                        // ترتيب من الأصغر إلى الأكبر (رصيد)
                        sorted.sortBy { accountBalances[it.id] ?: 0.0 }
                        Toast.makeText(context, "تم الترتيب حسب الرصيد (من الأصغر إلى الأكبر)", Toast.LENGTH_SHORT).show()
                    } else {
                        // ترتيب من الأكبر إلى الأصغر (رصيد)
                        sorted.sortByDescending { accountBalances[it.id] ?: 0.0 }
                        Toast.makeText(context, "تم الترتيب حسب الرصيد (من الأكبر إلى الأصغر)", Toast.LENGTH_SHORT).show()
                    }
                }
                    
                "name" -> {
                    if (isAscendingSort) {
                        // ترتيب من أ إلى ي (اسم)
                        sorted.sortBy { it.name }
                        Toast.makeText(context, "تم الترتيب حسب الاسم (أ → ي)", Toast.LENGTH_SHORT).show()
                    } else {
                        // ترتيب من ي إلى أ (اسم)
                        sorted.sortByDescending { it.name }
                        Toast.makeText(context, "تم الترتيب حسب الاسم (ي → أ)", Toast.LENGTH_SHORT).show()
                    }
                }
                    
                "number" -> {
                    if (isAscendingSort) {
                        // ترتيب من الأصغر إلى الأكبر (server_id)
                        sorted.sortBy { it.serverId }
                        Toast.makeText(context, "تم الترتيب حسب رقم الحساب (من الأصغر إلى الأكبر)", Toast.LENGTH_SHORT).show()
                    } else {
                        // ترتيب من الأكبر إلى الأصغر (server_id)
                        sorted.sortByDescending { it.serverId }
                        Toast.makeText(context, "تم الترتيب حسب رقم الحساب (من الأكبر إلى الأصغر)", Toast.LENGTH_SHORT).show()
                    }
                }
                    
                "date" -> {
                    if (isAscendingSort) {
                        // ترتيب من الأقدم إلى الأحدث (تاريخ)
                        sorted.sortBy { it.createdAt }
                        Toast.makeText(context, "تم الترتيب حسب التاريخ (الأقدم أولاً)", Toast.LENGTH_SHORT).show()
                    } else {
                        // ترتيب من الأحدث إلى الأقدم (تاريخ)
                        sorted.sortByDescending { it.createdAt }
                        Toast.makeText(context, "تم الترتيب حسب التاريخ (الأحدث أولاً)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            isAscendingSort = !isAscendingSort // تبديل اتجاه الترتيب
            accountsAdapter.updateAccounts(sorted)
            
            // إعادة تفعيل الزر بعد فترة قصيرة
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                sortButton.isEnabled = true
            }, 500)
        }

        // Setup add account button
        addAccountButton.setOnClickListener {
            Navigation.findNavController(root).navigate(R.id.addAccountFragment)
        }

        val originalMargin = (addAccountButton.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(addAccountButton) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = originalMargin + bottom
            v.layoutParams = params
            insets
        }

        return root
    }

    private class AccountsAdapter(
        private var accounts: MutableList<Account>,
        private val accountViewModel: AccountViewModel,
        private val lifecycleOwner: LifecycleOwner
    ) : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {

        fun updateAccounts(newAccounts: List<Account>) {
            accounts.clear()
            accounts.addAll(newAccounts)
            notifyDataSetChanged()
        }

        @NonNull
        override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_account, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {
            val account = accounts[position]
            holder.accountName.text = account.name
            holder.phone.text = account.phoneNumber
            
            // عرض رقم الحساب مع تنسيق
            val serverId = account.serverId
            holder.accountNumber.text = if (serverId > 0) {
                "رقم الحساب: $serverId"
            } else {
                "رقم: غير محدد"
            }

            // راقب الرصيد اليمني فقط
            accountViewModel.getAccountBalanceYemeni(account.id).observe(lifecycleOwner) { balance ->
                val value = balance ?: 0.0
                val balanceText = if (value < 0) {
                    String.format(Locale.US, "عليه %,d يمني", kotlin.math.abs(value.toLong()))
                } else {
                    String.format(Locale.US, "له %,d يمني", value.toLong())
                }
                
                holder.balance.text = balanceText
                holder.balance.setTextColor(
                    holder.itemView.context.getColor(
                        if (value < 0) R.color.debit_red else R.color.credit_green
                    )
                )
            }

            // Setup WhatsApp switch
            // منع الاستدعاء المزدوج
            holder.whatsappSwitch.setOnCheckedChangeListener(null)
            holder.whatsappSwitch.isChecked = account.isWhatsappEnabled
            
            // تعيين اللون الأولي
            val switchColor = if (account.isWhatsappEnabled) {
                holder.itemView.context.getColor(R.color.credit_green)
            } else {
                holder.itemView.context.getColor(R.color.gray)
            }
            
            holder.whatsappSwitch.thumbTintList = android.content.res.ColorStateList.valueOf(switchColor)
            holder.whatsappSwitch.trackTintList = android.content.res.ColorStateList.valueOf(switchColor)
            
            holder.whatsappSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                // منع التحديث إذا كانت القيمة نفسها
                if (account.isWhatsappEnabled == isChecked) {
                    return@setOnCheckedChangeListener
                }
                
                // منع انتشار الحدث
                buttonView.isEnabled = false
                
                // تحديث حالة واتساب الحساب
                account.isWhatsappEnabled = isChecked
                account.updatedAt = System.currentTimeMillis()
                accountViewModel.updateAccount(account)
                
                // تغيير لون الزر
                val newColor = if (isChecked) {
                    holder.itemView.context.getColor(R.color.credit_green)
                } else {
                    holder.itemView.context.getColor(R.color.gray)
                }
                
                holder.whatsappSwitch.thumbTintList = android.content.res.ColorStateList.valueOf(newColor)
                holder.whatsappSwitch.trackTintList = android.content.res.ColorStateList.valueOf(newColor)
                
                val message = if (isChecked) "تم تفعيل واتساب للحساب" else "تم إيقاف واتساب للحساب"
                Toast.makeText(buttonView.context, message, Toast.LENGTH_SHORT).show()
                
                // إعادة تفعيل الزر بعد فترة قصيرة
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    holder.whatsappSwitch.isEnabled = true
                }, 300)
            }

            // Setup edit button
            holder.editButton.setOnClickListener { v ->
                // منع انتشار الحدث
                v.isEnabled = false
                
                val args = Bundle().apply {
                    putLong("accountId", account.id)
                }
                Navigation.findNavController(v).navigate(R.id.editAccountFragment, args)
                
                // إعادة تفعيل الزر بعد فترة قصيرة
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    holder.editButton.isEnabled = true
                }, 300)
            }

            // Setup item click
            holder.itemView.setOnClickListener { v ->
                // منع انتشار الحدث
                v.isEnabled = false
                
                val args = Bundle().apply {
                    putLong("accountId", account.id)
                }
                Navigation.findNavController(v).navigate(R.id.accountDetailsFragment, args)
                
                // إعادة تفعيل العنصر بعد فترة قصيرة
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    holder.itemView.isEnabled = true
                }, 300)
            }
        }

        override fun getItemCount(): Int = accounts.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val accountName: TextView = itemView.findViewById(R.id.account_name)
            val phone: TextView = itemView.findViewById(R.id.phone)
            val accountNumber: TextView = itemView.findViewById(R.id.account_number)
            val balance: TextView = itemView.findViewById(R.id.balance)
            val whatsappSwitch: SwitchMaterial = itemView.findViewById(R.id.whatsapp_switch)
            val editButton: MaterialButton = itemView.findViewById(R.id.edit_button)
        }
    }
} 