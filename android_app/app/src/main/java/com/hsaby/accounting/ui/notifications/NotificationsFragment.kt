package com.hsaby.accounting.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hsaby.accounting.databinding.FragmentNotificationsBinding
import com.hsaby.accounting.notification.NotificationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationsAdapter
    private lateinit var notificationManager: NotificationManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNotificationManager()
        setupRecyclerView()
        observeNotifications()
        setupListeners()
    }

    private fun setupNotificationManager() {
        notificationManager = NotificationManager(requireContext())
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                viewModel.markAsRead(notification.id)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NotificationsFragment.adapter
        }
    }

    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notifications.collectLatest { notifications ->
                adapter.submitList(notifications)
                binding.emptyView.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupListeners() {
        binding.markAllReadButton.setOnClickListener {
            viewModel.markAllAsRead()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 