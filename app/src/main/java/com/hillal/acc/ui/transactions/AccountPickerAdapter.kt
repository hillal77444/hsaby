package com.hillal.acc.ui.transactions

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.hillal.acc.App
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.ui.transactions.AccountPickerAdapter.AccountViewHolder
import java.util.Locale
import kotlin.math.abs

class AccountPickerAdapter(
    context: Context,
    accounts: MutableList<Account>?,
    accountTransactions: MutableMap<Long?, MutableList<Transaction?>?>?,
    listener: OnAccountClickListener?
) : RecyclerView.Adapter<AccountViewHolder?>() {
    interface OnAccountClickListener {
        fun onAccountClick(account: Account?)
    }

    private var accounts: MutableList<Account>?
    private val accountTransactions: MutableMap<Long?, MutableList<Transaction?>?>?
    private val listener: OnAccountClickListener?
    private val context: Context?
    private val transactionRepository: TransactionRepository?
    private var lifecycleOwner: LifecycleOwner? = null
    private var balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?> =
        HashMap<Long?, MutableMap<String?, Double?>?>()

    init {
        this.context = context
        this.accounts = accounts
        this.accountTransactions = accountTransactions
        this.listener = listener
        this.transactionRepository =
            TransactionRepository((context.getApplicationContext() as App).getDatabase())
        if (context is LifecycleOwner) {
            this.lifecycleOwner = context as LifecycleOwner
        } else {
            throw IllegalArgumentException("Context must be a LifecycleOwner")
        }
    }

    fun updateList(filtered: MutableList<Account>?) {
        this.accounts = filtered
        notifyDataSetChanged()
    }

    fun setBalancesMap(balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?>) {
        this.balancesMap = balancesMap
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_account_picker, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accounts!!.get(position)
        holder.accountNameTextView.setText(if (account.getName() != null) account.getName() else "بدون اسم")
        holder.accountNameTextView.setTextColor(-0xddddde)
        val icon = if (account.getName() != null && !account.getName().isEmpty()) account.getName()
            .substring(0, 1) else "?"
        holder.accountIconTextView.setText(icon)
        val notes = account.getNotes()
        if (notes != null && !notes.trim { it <= ' ' }.isEmpty()) {
            holder.accountNotesTextView.setText(notes)
            holder.accountNotesTextView.setVisibility(View.VISIBLE)
        } else {
            holder.accountNotesTextView.setText("")
            holder.accountNotesTextView.setVisibility(View.GONE)
        }
        holder.balancesContainer.removeAllViews()
        val currencyBalances = balancesMap.get(account.getId())
        if (currencyBalances != null && !currencyBalances.isEmpty()) {
            for (currency in currencyBalances.keys) {
                val balance: Double = currencyBalances.get(currency)!!
                val label: String?
                val bgColor: Int
                val textColor: Int
                if (balance > 0) {
                    label = "له " + String.format(Locale.US, "%.2f", balance) + " " + currency
                    bgColor = -0x170a17
                    textColor = -0xc771c4
                } else if (balance < 0) {
                    label =
                        "عليه " + String.format(Locale.US, "%.2f", abs(balance)) + " " + currency
                    bgColor = -0x1412
                    textColor = -0x2cd0d1
                } else {
                    label = "رصيد صفر " + currency
                    bgColor = -0x13100f
                    textColor = -0x9f8275
                }
                val tv = TextView(context)
                tv.setText(label)
                tv.setTextSize(14f)
                tv.setTextColor(textColor)
                tv.setPadding(18, 6, 18, 6)
                tv.setBackgroundResource(R.drawable.bg_balance_chip)
                tv.setBackgroundTintList(ColorStateList.valueOf(bgColor))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMarginEnd(12)
                tv.setLayoutParams(params)
                holder.balancesContainer.addView(tv)
            }
        }
        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            if (listener != null) listener.onAccountClick(account)
        })
    }

    override fun getItemCount(): Int {
        return if (accounts != null) accounts!!.size else 0
    }

    internal class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var accountNameTextView: TextView
        var balancesContainer: LinearLayout
        var accountIconTextView: TextView
        var accountNotesTextView: TextView

        init {
            accountNameTextView = itemView.findViewById<TextView>(R.id.accountNameTextView)
            balancesContainer = itemView.findViewById<LinearLayout>(R.id.balancesContainer)
            accountIconTextView = itemView.findViewById<TextView>(R.id.accountIconTextView)
            accountNotesTextView = itemView.findViewById<TextView>(R.id.accountNotesTextView)
        }
    }
}