package com.hsaby.accounting.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.hsaby.accounting.R
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.hsaby.accounting.databinding.FragmentReportsBinding
import com.hsaby.accounting.util.Constants
import com.hsaby.accounting.util.PreferencesManager
import com.hsaby.accounting.util.ReportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class ReportsFragment : Fragment() {
    
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var reportGenerator: ReportGenerator
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        reportGenerator = ReportGenerator(requireContext())
        
        setupViews()
        observeData()
    }
    
    private fun setupViews() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
        
        binding.btnExportPdf.setOnClickListener {
            lifecycleScope.launch {
                val accounts = getAccounts()
                val transactions = getTransactions()
                val file = reportGenerator.generatePdfReport(accounts, transactions)
                reportGenerator.shareReport(file)
            }
        }
        
        binding.btnExportExcel.setOnClickListener {
            lifecycleScope.launch {
                val accounts = getAccounts()
                val transactions = getTransactions()
                val file = reportGenerator.generateExcelReport(accounts, transactions)
                reportGenerator.shareReport(file)
            }
        }
    }
    
    private fun observeData() {
        lifecycleScope.launch {
            val userId = preferencesManager.getUserId()
            
            // جمع بيانات الحسابات
            val accounts = getAccounts()
            
            // جمع بيانات المعاملات
            val transactions = getTransactions()
            
            // تحديث واجهة المستخدم
            updateUI(accounts, transactions)
        }
    }
    
    private suspend fun getAccounts(): List<AccountEntity> {
        // TODO: استبدال هذا بجلب البيانات من قاعدة البيانات
        return emptyList()
    }
    
    private suspend fun getTransactions(): List<TransactionEntity> {
        // TODO: استبدال هذا بجلب البيانات من قاعدة البيانات
        return emptyList()
    }
    
    private fun updateUI(accounts: List<AccountEntity>, transactions: List<TransactionEntity>) {
        // تحديث إجمالي الأرصدة
        val yemeniTotal = accounts.filter { it.currency == Constants.CURRENCY_YEMENI }
            .sumOf { it.balance }
        val saudiTotal = accounts.filter { it.currency == Constants.CURRENCY_SAUDI }
            .sumOf { it.balance }
        val dollarTotal = accounts.filter { it.currency == Constants.CURRENCY_DOLLAR }
            .sumOf { it.balance }
        
        binding.tvYemeniTotal.text = yemeniTotal.toString()
        binding.tvSaudiTotal.text = saudiTotal.toString()
        binding.tvDollarTotal.text = dollarTotal.toString()
        
        // تحديث إجمالي المعاملات
        val toTotal = transactions.filter { it.type == Constants.TRANSACTION_TYPE_TO }
            .sumOf { it.amount }
        val fromTotal = transactions.filter { it.type == Constants.TRANSACTION_TYPE_FROM }
            .sumOf { it.amount }
        
        binding.tvToTotal.text = toTotal.toString()
        binding.tvFromTotal.text = fromTotal.toString()
        
        // تحديث الرسم البياني الدائري
        val pieEntries = listOf(
            PieEntry(yemeniTotal.toFloat(), "ريال يمني"),
            PieEntry(saudiTotal.toFloat(), "ريال سعودي"),
            PieEntry(dollarTotal.toFloat(), "دولار")
        )
        
        val pieDataSet = PieDataSet(pieEntries, "توزيع العملات")
        pieDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        
        binding.pieChart.data = PieData(pieDataSet)
        binding.pieChart.invalidate()
        
        // تحديث الرسم البياني الخطي
        val lineEntries = transactions
            .groupBy { it.date }
            .map { (date, transactions) ->
                Entry(
                    date.toFloat(),
                    transactions.sumOf { it.amount }.toFloat()
                )
            }
            .sortedBy { it.x }
        
        val lineDataSet = LineDataSet(lineEntries, "المعاملات")
        lineDataSet.color = requireContext().getColor(R.color.colorPrimary)
        lineDataSet.setCircleColor(requireContext().getColor(R.color.colorPrimary))
        
        binding.lineChart.data = LineData(lineDataSet)
        binding.lineChart.invalidate()
    }
    
    private fun refreshData() {
        lifecycleScope.launch {
            try {
                // TODO: تحديث البيانات من الخادم
                binding.swipeRefresh.isRefreshing = false
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                // TODO: عرض رسالة خطأ
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 