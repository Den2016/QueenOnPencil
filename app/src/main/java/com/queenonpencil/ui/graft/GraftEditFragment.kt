package com.queenonpencil.ui.graft

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.queenonpencil.data.BreedingCalendar
import com.queenonpencil.databinding.FragmentGraftEditBinding
import com.queenonpencil.notification.AlarmScheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GraftEditFragment : Fragment() {

    private var _binding: FragmentGraftEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GraftEditViewModel by viewModels()
    private var graftId = 0L
    private var selectedDate = LocalDate.now()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGraftEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        graftId = arguments?.getLong("graftId") ?: 0L

        binding.spinnerType.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, BreedingCalendar.GRAFT_TYPES
        )

        binding.spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                refreshPreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val previewAdapter = EventPreviewAdapter()
        binding.rvPreview.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPreview.adapter = previewAdapter

        binding.btnDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d)
                updateDateDisplay()
                refreshPreview()
            }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
        }

        binding.btnSave.setOnClickListener {
            val tp = binding.spinnerType.selectedItemPosition
            val dt = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val shift = binding.etShift.text.toString().toIntOrNull() ?: 0
            val desc = binding.etDesc.text.toString()
            viewModel.save(tp, dt, shift, desc, graftId)
        }

        viewModel.grafting.observe(viewLifecycleOwner) { g ->
            selectedDate = try {
                LocalDate.parse(g.dt, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                LocalDate.now()
            }
            updateDateDisplay()
            binding.spinnerType.setSelection(g.tp)
            binding.etShift.setText(if (g.shift != 0) g.shift.toString() else "")
            binding.etDesc.setText(g.desc)
            refreshPreview()
        }

        viewModel.preview.observe(viewLifecycleOwner) { previewAdapter.submitList(it) }
        viewModel.saved.observe(viewLifecycleOwner) { if (it) findNavController().popBackStack() }

        viewModel.load(graftId)

        if (graftId == 0L) {
            // Если создаём НОВУЮ прививку (graftId == 0L), установи тип из настроек
            val defaultType = AlarmScheduler.getDefaultGraftType(requireContext())
            binding.spinnerType.setSelection(defaultType)

            updateDateDisplay()
            refreshPreview()
        }
    }

    private fun refreshPreview() {
        viewModel.updatePreview(
            selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            binding.spinnerType.selectedItemPosition
        )
    }

    private fun updateDateDisplay() {
        binding.btnDate.text = selectedDate.format(
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
