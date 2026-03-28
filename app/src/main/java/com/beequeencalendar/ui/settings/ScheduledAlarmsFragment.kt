package com.beequeencalendar.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.beequeencalendar.databinding.FragmentScheduledAlarmsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ScheduledAlarmsFragment : Fragment() {

    private var _binding: FragmentScheduledAlarmsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduledAlarmsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduledAlarmsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ScheduledAlarmsAdapter { eventId ->
            // Подтверждение отмены будильника
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("@string/cancel_alarm")
                .setMessage("@string/cancel_alarm_confirm")
                .setPositiveButton("@string/delete") { _, _ ->
                    viewModel.cancelAlarm(eventId)
                }
                .setNegativeButton("@android:string/cancel", null)
                .show()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.alarms.observe(viewLifecycleOwner) { alarms ->
            adapter.submitList(alarms)
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        viewModel.loadAlarms()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}