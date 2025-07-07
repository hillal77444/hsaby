package com.hillal.acc.ui.reports

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hillal.acc.R
import java.util.Locale

class AccountsSummaryAdapter(private var data: MutableList<AccountSummary>?) :
    RecyclerView.Adapter<AccountsSummaryAdapter.ViewHolder?>() {
    class AccountSummary(
        val accountName: String?,
        val credit: Double,
        val debit: Double,
        val balance: Double
    )

    fun setData(data: MutableList<AccountSummary>?) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_account_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data!!.get(position)
        holder.tvAccountName.setText(item.accountName)
        holder.tvCredit.setText(String.format(Locale.US, "%,.0f", item.credit))
        holder.tvDebit.setText(String.format(Locale.US, "%,.0f", item.debit))
        holder.tvBalance.setText(String.format(Locale.US, "%,.0f", item.balance))
        if (item.balance > 0) {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_upward)
            holder.ivArrow.setColorFilter(Color.parseColor("#4CAF50"))
            holder.tvBalance.setTextColor(Color.parseColor("#4CAF50"))
        } else if (item.balance < 0) {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_downward)
            holder.ivArrow.setColorFilter(Color.parseColor("#F44336"))
            holder.tvBalance.setTextColor(Color.parseColor("#F44336"))
        } else {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_upward)
            holder.ivArrow.setColorFilter(Color.GRAY)
            holder.tvBalance.setTextColor(Color.GRAY)
        }
        holder.tvAccountName.setGravity(Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        holder.tvCredit.setGravity(Gravity.CENTER)
        holder.tvDebit.setGravity(Gravity.CENTER)
        holder.tvBalance.setGravity(Gravity.CENTER)
    }

    override fun getItemCount(): Int {
        return if (data != null) data!!.size else 0
    }

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvAccountName: TextView
        var tvCredit: TextView
        var tvDebit: TextView
        var tvBalance: TextView
        var ivArrow: ImageView

        init {
            tvAccountName = itemView.findViewById<TextView>(R.id.userNameTextView)
            tvCredit = itemView.findViewById<TextView>(R.id.creditsTextView)
            tvDebit = itemView.findViewById<TextView>(R.id.debitsTextView)
            tvBalance = itemView.findViewById<TextView>(R.id.balanceTextView)
            ivArrow = itemView.findViewById<ImageView>(R.id.ivArrow)
        }
    }
}