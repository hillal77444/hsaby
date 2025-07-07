package com.hillal.acc.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hillal.acc.R
import com.hillal.acc.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class TransactionsAdapter :
    ListAdapter<Transaction, TransactionsAdapter.TransactionViewHolder?>(object :
        DiffUtil.ItemCallback<Transaction?>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.getId() == newItem.getId()
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    internal class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView
        private val amountTextView: TextView
        private val descriptionTextView: TextView

        init {
            dateTextView = itemView.findViewById<TextView>(R.id.transactionDate)
            amountTextView = itemView.findViewById<TextView>(R.id.transactionAmount)
            descriptionTextView = itemView.findViewById<TextView>(R.id.transactionDescription)
        }

        fun bind(transaction: Transaction) {
            dateTextView.setText(dateFormat.format(Date(transaction.getTransactionDate())))
            descriptionTextView.setText(transaction.getDescription())

            val amount = transaction.getAmount()
            val type =
                if (transaction.getType() != null) transaction.getType().trim { it <= ' ' } else ""

            val amountStr: String?
            if (abs(amount - Math.round(amount)) < 0.00001) {
                amountStr = String.format("%.0f", amount)
            } else {
                amountStr = String.format("%.2f", amount)
            }

            amountTextView.setText(amountStr + " " + transaction.getCurrency())

            if ((type == "عليه" || type.equals("debit", ignoreCase = true)) && amount != 0.0) {
                itemView.setActivated(true)
                itemView.setSelected(false)
            } else if ((type == "له" || type.equals(
                    "credit",
                    ignoreCase = true
                )) && amount != 0.0
            ) {
                itemView.setActivated(false)
                itemView.setSelected(true)
            } else {
                itemView.setActivated(false)
                itemView.setSelected(false)
            }
        }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }
}