package com.hsaby.accounting.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.hsaby.accounting.databinding.ItemTransactionBinding
import com.hsaby.accounting.util.Constants
import java.text.SimpleDateFormat
import java.util.*

class TransactionsAdapter : ListAdapter<TransactionEntity, TransactionsAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionEntity) {
            binding.tvAmount.text = "${transaction.amount} ${transaction.currency}"
            binding.tvType.text = transaction.type
            binding.tvDescription.text = transaction.description
            binding.tvDate.text = dateFormat.format(Date(transaction.date))
            
            // Set type color
            val colorRes = when (transaction.type) {
                Constants.TRANSACTION_TYPE_TO -> android.R.color.holo_green_dark
                Constants.TRANSACTION_TYPE_FROM -> android.R.color.holo_red_dark
                else -> android.R.color.black
            }
            binding.tvType.setTextColor(binding.root.context.getColor(colorRes))
            
            // Set notes if available
            transaction.notes?.let {
                binding.tvNotes.text = it
                binding.tvNotes.visibility = ViewGroup.VISIBLE
            } ?: run {
                binding.tvNotes.visibility = ViewGroup.GONE
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem == newItem
        }
    }
} 