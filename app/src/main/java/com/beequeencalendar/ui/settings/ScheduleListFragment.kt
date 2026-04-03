package com.beequeencalendar.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.beequeencalendar.R
import com.beequeencalendar.data.entity.NotificationSchedule
import com.beequeencalendar.databinding.FragmentScheduleListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ScheduleListFragment : Fragment() {
    private var _binding: FragmentScheduleListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScheduleListViewModel by viewModels()
    private lateinit var adapter: ScheduleListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ScheduleListAdapter(
            onClick = { schedule ->
                findNavController().navigate(
                    R.id.action_scheduleList_to_scheduleEdit,
                    Bundle().apply { putLong("scheduleId", schedule.id) }
                )
            },
            onSetDefault = { schedule ->
                viewModel.setDefault(schedule.id)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.schedules.observe(viewLifecycleOwner) { schedules ->
            adapter.submitList(schedules)
            binding.tvEmpty.visibility = if (schedules.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_scheduleList_to_scheduleEdit)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}