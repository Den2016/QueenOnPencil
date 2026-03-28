package com.beequeencalendar.ui.graft

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.beequeencalendar.R
import com.beequeencalendar.data.BreedingCalendar
import com.beequeencalendar.databinding.FragmentGraftEditBinding
import com.beequeencalendar.notification.AlarmScheduler
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

        // По умолчанию матка (tp = 0)
        var currentTp = 0

        // Инициализация спиннера для матки
        updateSpinnerAdapter(tp = 0)
        binding.spinnerType.setSelection(0)

        // Обновляем превью при переключении пола
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentTp = if (checkedId == R.id.radioButton2) 1 else 0  // трутень = 1, матка = 0
            updateSpinnerAdapter(tp = currentTp)
            binding.spinnerType.setSelection(0)  // сброс на первый элемент
            refreshPreview()
        }

//        binding.spinnerType.adapter = ArrayAdapter(
//            requireContext(), android.R.layout.simple_spinner_dropdown_item, BreedingCalendar.GRAFT_TYPES
//        )

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
            val shift = binding.spinnerType.selectedItemPosition
            val dt = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val tp = when (binding.radioGroup.checkedRadioButtonId) {
                R.id.radioButton -> 0      // матка
                R.id.radioButton2 -> 1     // трутень
                else -> 0                  // по умолчанию матка
            }
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
            // 👇 ДОБАВЬ ПРОВЕРКУ: если новая прививка и shift=0, бери из настроек
            val typeToSet = if (graftId == 0L ) {
                AlarmScheduler.getDefaultGraftType(requireContext())
            } else {
                g.shift
            }
            if(graftId != 0L){
                currentTp = g.tp

                // Устанавливаем RadioGroup
                binding.radioGroup.check(
                    if (currentTp == 1) R.id.radioButton2 else R.id.radioButton
                )

                // Обновляем адаптер спиннера
                updateSpinnerAdapter(tp = currentTp)
            }


            binding.spinnerType.setSelection(typeToSet)
            binding.etDesc.setText(g.desc)
            refreshPreview()
        }

        viewModel.preview.observe(viewLifecycleOwner) { previewAdapter.submitList(it) }
        viewModel.saved.observe(viewLifecycleOwner) { if (it) findNavController().popBackStack() }

        viewModel.load(graftId)

    }

    private fun refreshPreview() {
        // Определяем пол: 0 = матка, 1 = трутень
        val sex = when (binding.radioGroup.checkedRadioButtonId) {
            R.id.radioButton -> 0      // матка
            R.id.radioButton2 -> 1     // трутень
            else -> 0                  // по умолчанию матка
        }
        viewModel.updatePreview(
            selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            binding.spinnerType.selectedItemPosition,
            sex
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

    // Функция обновления адаптера спиннера
    private fun updateSpinnerAdapter(tp: Int) {
        val types = if (tp == 1) {
            BreedingCalendar.DRON_TYPES  // трутень
        } else {
            BreedingCalendar.GRAFT_TYPES  // матка
        }

        binding.spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            types
        )
    }

}
