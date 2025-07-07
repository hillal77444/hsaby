package com.hillal.acc.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hillal.acc.R
import com.hillal.acc.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class ReportAdapter : RecyclerView.Adapter<ReportAdapter.ViewHolder?>() {
    private var transactions: MutableList<Transaction> = ArrayList<Transaction>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions.get(position)
        holder.bind(transaction)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    fun setTransactions(transactions: MutableList<Transaction>) {
        this.transactions = transactions
        notifyDataSetChanged()
    }

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView
        private val amountTextView: TextView
        private val descriptionTextView: TextView

        init {
            dateTextView = itemView.findViewById<TextView>(R.id.transactionDate)
            amountTextView = itemView.findViewById<TextView>(R.id.transactionAmount)
            descriptionTextView = itemView.findViewById<TextView>(R.id.transactionDescription)
        }

        fun bind(transaction: Transaction) {
            dateTextView.setText(DATE_FORMAT.format(Date(transaction.getTransactionDate())))
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

            if ((type == "عليه" || type.equals("debit", ignoreCase = true)) && amount != 0.0) {
                amountTextView.setText(amountStr + " " + transaction.getCurrency())
                amountTextView.setTextColor(
                    itemView.getContext().getResources().getColor(R.color.debit_color)
                )
                itemView.setBackgroundColor(
                    itemView.getContext().getResources().getColor(R.color.red_100)
                )
            } else if ((type == "له" || type.equals(
                    "credit",
                    ignoreCase = true
                )) && amount != 0.0
            ) {
                amountTextView.setText(amountStr + " " + transaction.getCurrency())
                amountTextView.setTextColor(
                    itemView.getContext().getResources().getColor(R.color.credit_color)
                )
                itemView.setBackgroundColor(
                    itemView.getContext().getResources().getColor(R.color.green_100)
                )
            } else {
                amountTextView.setText("")
                amountTextView.setTextColor(
                    itemView.getContext().getResources().getColor(R.color.text_primary)
                )
                itemView.setBackgroundColor(
                    itemView.getContext().getResources().getColor(R.color.white)
                )
            }
        }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }
}