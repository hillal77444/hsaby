package com.hsaby.accounting.ui.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.databinding.ItemAccountBinding
import com.hsaby.accounting.util.Constants

class AccountsAdapter : ListAdapter<AccountEntity, AccountsAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AccountViewHolder(
        private val binding: ItemAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: AccountEntity) {
            binding.tvAccountName.text = account.accountName
            binding.tvBalance.text = "${account.balance} ${account.currency}"
            
            // Set currency color
            val colorRes = when (account.currency) {
                Constants.CURRENCY_YEMENI -> android.R.color.holo_red_dark
                Constants.CURRENCY_SAUDI -> android.R.color.holo_green_dark
                Constants.CURRENCY_DOLLAR -> android.R.color.holo_blue_dark
                else -> android.R.color.black
            }
            binding.tvBalance.setTextColor(binding.root.context.getColor(colorRes))
            
            // Set phone number if available
            account.phoneNumber?.let {
                binding.tvPhoneNumber.text = it
                binding.tvPhoneNumber.visibility = ViewGroup.VISIBLE
            } ?: run {
                binding.tvPhoneNumber.visibility = ViewGroup.GONE
            }
            
            // Set notes if available
            account.notes?.let {
                binding.tvNotes.text = it
                binding.tvNotes.visibility = ViewGroup.VISIBLE
            } ?: run {
                binding.tvNotes.visibility = ViewGroup.GONE
            }
        }
    }

    private class AccountDiffCallback : DiffUtil.ItemCallback<AccountEntity>() {
        override fun areItemsTheSame(oldItem: AccountEntity, newItem: AccountEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AccountEntity, newItem: AccountEntity): Boolean {
            return oldItem == newItem
        }
    }
} 