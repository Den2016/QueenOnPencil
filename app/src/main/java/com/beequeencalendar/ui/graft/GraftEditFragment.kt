package com.beequeencalendar.ui.graft

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.beequeencalendar.R
import com.beequeencalendar.data.AppDatabase
import com.beequeencalendar.data.BreedingCalendar
import com.beequeencalendar.data.entity.NotificationSchedule
import com.beequeencalendar.databinding.FragmentGraftEditBinding
import com.beequeencalendar.notification.AlarmScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GraftEditFragment : Fragment() {

    private var _binding: FragmentGraftEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GraftEditViewModel by viewModels()
    private var graftId = 0L
    private var selectedDate = LocalDate.now()

    // ✅ Кэшируем список расписаний и дефолтный ID
    private var scheduleList: List<NotificationSchedule> = emptyList()
    private var defaultScheduleId: Long = 1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraftEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        graftId = arguments?.getLong("graftId") ?: 0L
        var currentTp = 0

        // ✅ 1. Загружаем дефолтный ID расписания (suspend вызов в корутине)
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            defaultScheduleId = db.notificationScheduleDao().getDefault()?.id ?: 1L
        }

        // ✅ 2. Spinner расписаний — наблюдаем за LiveData напрямую (НЕ в launch!)
        val scheduleDao = AppDatabase.getInstance(requireContext()).notificationScheduleDao()
        scheduleDao.getAllActive().observe(viewLifecycleOwner) { schedules ->
            scheduleList = schedules  // ✅ Сохраняем список для доступа по ID
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                schedules.map { it.name }
            )
            binding.spinnerSchedule.adapter = adapter

            // ✅ 3. Устанавливаем выбранное расписание из grafting
            viewModel.grafting.observe(viewLifecycleOwner) { g ->
                val currentScheduleId = g.scheduleId.takeIf { it > 0 } ?: defaultScheduleId
                val scheduleIndex = schedules.indexOfFirst { it.id == currentScheduleId }
                if (scheduleIndex >= 0 && scheduleIndex < adapter.count) {
                    binding.spinnerSchedule.setSelection(scheduleIndex)
                }
            }
        }

        // Инициализация спиннера типа прививки
        updateSpinnerAdapter(tp = 0)
        binding.spinnerType.setSelection(0)

        // Переключение пола (матка/трутень)
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentTp = if (checkedId == R.id.radioButton2) 1 else 0
            updateSpinnerAdapter(tp = currentTp)
            binding.spinnerType.setSelection(0)
            refreshPreview()
        }

        binding.spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                refreshPreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Превью событий
        val previewAdapter = EventPreviewAdapter()
        binding.rvPreview.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPreview.adapter = previewAdapter

        // Выбор даты
        binding.btnDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    selectedDate = LocalDate.of(y, m + 1, d)
                    updateDateDisplay()
                    refreshPreview()
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        // ✅ 4. Кнопка сохранения (ОДИН раз!)
        binding.btnSave.setOnClickListener {
            val shift = binding.spinnerType.selectedItemPosition
            val dt = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val tp = when (binding.radioGroup.checkedRadioButtonId) {
                R.id.radioButton -> 0
                R.id.radioButton2 -> 1
                else -> 0
            }
            val desc = binding.etDesc.text.toString()

            // ✅ Получаем ID выбранного расписания из спиннера
            val selectedPos = binding.spinnerSchedule.selectedItemPosition
            val scheduleId = if (selectedPos >= 0 && selectedPos < scheduleList.size) {
                scheduleList[selectedPos].id
            } else {
                defaultScheduleId
            }

            viewModel.save(tp, dt, shift, desc, graftId, scheduleId)
        }

        // Загрузка данных прививки
        viewModel.grafting.observe(viewLifecycleOwner) { g ->
            selectedDate = try {
                LocalDate.parse(g.dt, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                LocalDate.now()
            }
            updateDateDisplay()

            val typeToSet = if (graftId == 0L) {
                AlarmScheduler.getDefaultGraftType(requireContext())
            } else {
                g.shift
            }

            if (graftId != 0L) {
                currentTp = g.tp
                binding.radioGroup.check(
                    if (currentTp == 1) R.id.radioButton2 else R.id.radioButton
                )
                updateSpinnerAdapter(tp = currentTp)
            }

            binding.spinnerType.setSelection(typeToSet)
            binding.etDesc.setText(g.desc)
            refreshPreview()
        }

        viewModel.preview.observe(viewLifecycleOwner) {
            previewAdapter.submitList(it)
        }

        // Обработка результата сохранения
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is GraftEditViewModel.SaveResult.Success -> {
                    findNavController().popBackStack()
                }
                is GraftEditViewModel.SaveResult.Warning -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Внимание")
                        .setMessage(result.message)
                        .setPositiveButton("Сохранить без будильников") { _, _ ->
                            findNavController().popBackStack()
                        }
                        .setNegativeButton("Настройки") { _, _ ->
                            AlarmScheduler.requestExactAlarmPermission(requireContext())
                        }
                        .setOnDismissListener { viewModel.resetSaveResult() }
                        .show()
                }
                is GraftEditViewModel.SaveResult.Error -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Ошибка")
                        .setMessage("Не удалось сохранить прививку. Попробуйте ещё раз.")
                        .setPositiveButton("OK") { _, _ ->
                            viewModel.resetSaveResult()
                        }
                        .show()
                }
                null -> {}
            }
        }

        viewModel.load(graftId)
    }

    private fun refreshPreview() {
        val sex = when (binding.radioGroup.checkedRadioButtonId) {
            R.id.radioButton -> 0
            R.id.radioButton2 -> 1
            else -> 0
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

    private fun updateSpinnerAdapter(tp: Int) {
        val types = if (tp == 1) {
            BreedingCalendar.DRON_TYPES
        } else {
            BreedingCalendar.GRAFT_TYPES
        }
        binding.spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            types
        )
    }
}