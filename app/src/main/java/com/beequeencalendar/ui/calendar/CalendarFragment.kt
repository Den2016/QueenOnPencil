package com.beequeencalendar.ui.calendar

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.beequeencalendar.R
import com.beequeencalendar.databinding.FragmentCalendarBinding

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = CalendarAdapter(
            onNoteClick = { eventId, currentNote -> showNoteDialog(eventId, currentNote) },
            onGraftClick = { graftId ->
                findNavController().navigate(
                    R.id.action_calendar_to_graftEdit,
                    bundleOf("graftId" to graftId)
                )
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.events.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
            binding.tvEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(
                R.id.action_calendar_to_graftEdit,
                bundleOf("graftId" to 0L)
            )
        }
    }

    private fun showNoteDialog(eventId: Long, currentNote: String) {
        val editText = EditText(requireContext()).apply {
            setText(currentNote)
            hint = getString(R.string.note_hint)
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.note_title)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.saveNote(eventId, editText.text.toString().trim())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
