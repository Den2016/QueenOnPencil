package com.beequeencalendar.ui.settings


import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.beequeencalendar.R
import com.beequeencalendar.data.BreedingCalendar
import com.beequeencalendar.data.entity.NotificationRule
import com.beequeencalendar.databinding.FragmentScheduleEditBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner


class ScheduleEditFragment : Fragment() {
    private var _binding: FragmentScheduleEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScheduleListViewModel by viewModels()
    private var scheduleId: Long = 0L
    private var currentRules = mutableListOf<NotificationRule>()
    private lateinit var rulesAdapter: RuleEditAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduleEditBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scheduleId = arguments?.getLong("scheduleId") ?: 0L
        rulesAdapter = RuleEditAdapter(
// ✅ СТАЛО:
            onRuleChanged = { rule, pos ->
                Log.d("DEBUG_FRAGMENT", "onRuleChanged pos=$pos, rule=$rule")
                currentRules[pos] = rule  // ✅ Обновляем источник данных!
                rulesAdapter.submitList(currentRules.toList())  // ✅ И уведомляем адаптер
            },
            onRuleDeleted = { rule ->
                currentRules.remove(rule)
                rulesAdapter.submitList(currentRules.toList())
            }
        )

        binding.rvRules.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRules.adapter = rulesAdapter

        // ✅ TimePicker для времени по умолчанию
        binding.timePickerDefault.setIs24HourView(true)
        binding.timePickerDefault.setOnTimeChangedListener { _, hour, minute ->
            // Просто запоминаем, применим при сохранении
        }

        binding.btnAddRule.setOnClickListener { showAddRuleDialog() }
        binding.btnDelete.visibility = if (scheduleId > 0) View.VISIBLE else View.GONE
        binding.btnDelete.setOnClickListener { confirmDelete() }

        loadSchedule()
    }
    // ✅ ИСПРАВЛЕНО в ScheduleEditFragment.kt:
    private var isDefaultSchedule = false  // Добавить поле

    private fun loadSchedule() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (schedule, rules) = viewModel.getScheduleWithRules(scheduleId)
            binding.etScheduleName.setText(schedule?.name ?: "")
            binding.timePickerDefault.hour = schedule?.defaultHour ?: 8
            binding.timePickerDefault.minute = schedule?.defaultMinute ?: 0
            isDefaultSchedule = schedule?.isDefault ?: false  // Сохраняем значение
            currentRules = rules.toMutableList()
            rulesAdapter.submitList(currentRules.toList())
        }
    }

    private fun showAddRuleDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rule_edit, null)
        val spinner: Spinner = view.findViewById(R.id.spinnerEventType)
        val etHour: EditText = view.findViewById(R.id.etHour)
        val etMinute: EditText = view.findViewById(R.id.etMinute)
        val chk1: CheckBox = view.findViewById(R.id.chkAdvance1)
        val chk2: CheckBox = view.findViewById(R.id.chkAdvance2)
        val chk3: CheckBox = view.findViewById(R.id.chkAdvance3)
        val etRepeatCount: EditText = view.findViewById(R.id.etRepeatCount)
        val etRepeatInterval: EditText = view.findViewById(R.id.etRepeatInterval)

        val types = BreedingCalendar.getAllEventTypes()
        spinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)

        // ✅ Инициализация временем по умолчанию
        etHour.setText(binding.timePickerDefault.hour.toString())
        etMinute.setText(binding.timePickerDefault.minute.toString())
        etRepeatCount.setText("0")
        etRepeatInterval.setText("5")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Новое правило")
            .setView(view)
            .setPositiveButton("Добавить") { _, _ ->
                val eventType = spinner.selectedItem.toString()
                if (currentRules.any { it.eventType == eventType }) {
                    MaterialAlertDialogBuilder(requireContext()).setTitle("Ошибка").setMessage("Правило для этого события уже есть").setPositiveButton("OK", null).show()
                    return@setPositiveButton
                }
                val advanceDays = mutableListOf(0)
                if (chk1.isChecked) advanceDays.add(1)
                if (chk2.isChecked) advanceDays.add(2)
                if (chk3.isChecked) advanceDays.add(3)

                val newRule = NotificationRule(
                    scheduleId = if (scheduleId > 0) scheduleId else 0L,
                    eventType = eventType,
                    isEnabled = true,
                    timeHour = etHour.text.toString().toIntOrNull() ?: 8,
                    timeMinute = etMinute.text.toString().toIntOrNull() ?: 0,
                    advanceDays = advanceDays.sorted().joinToString(",", "[", "]"),
                    repeatCount = etRepeatCount.text.toString().toIntOrNull() ?: 0,
                    repeatIntervalMin = etRepeatInterval.text.toString().toIntOrNull() ?: 5
                )
                currentRules.add(newRule)
                rulesAdapter.submitList(currentRules.toList())
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveSchedule() {
        val name = binding.etScheduleName.text.toString().trim()
        if (name.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Ошибка").setMessage("Введите название").setPositiveButton("OK", null).show()
            return
        }
        // ✅ Запускаем корутину и ЖДЁМ завершения сохранения
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("DEBUG_SAVE", "currentRules перед сохранением:")
            currentRules.forEachIndexed { i, r ->
                Log.d("DEBUG_SAVE", "  [$i] ${r.eventType} @ ${r.timeHour}:${r.timeMinute} ${r.advanceDays}")
            }
            viewModel.saveSchedule(
                scheduleId,
                name,
                isDefaultSchedule,
                binding.timePickerDefault.hour,
                binding.timePickerDefault.minute,
                currentRules
            )
            // ✅ Только после успешной записи в БД уходим с экрана
            findNavController().popBackStack()
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить?").setMessage(R.string.delete_schedule_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteSchedule(com.beequeencalendar.data.entity.NotificationSchedule(id = scheduleId, name = "", isDefault = false))
                findNavController().popBackStack()
            }.setNegativeButton("Отмена", null).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_schedule_edit, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_save -> { saveSchedule(); true }
        android.R.id.home -> { findNavController().popBackStack(); true }
        else -> super.onOptionsItemSelected(item)
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}