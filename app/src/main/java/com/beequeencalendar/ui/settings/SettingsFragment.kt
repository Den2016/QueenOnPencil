package com.beequeencalendar.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.beequeencalendar.R
import com.beequeencalendar.data.BreedingCalendar
import com.beequeencalendar.notification.AlarmScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
        timePicker.setIs24HourView(true)
        timePicker.hour = AlarmScheduler.getHour(requireContext())
        timePicker.minute = AlarmScheduler.getMinute(requireContext())

//        timePicker.setOnTimeChangedListener { _, hour, minute ->
//            AlarmScheduler.saveTime(requireContext(), hour, minute)
//            CoroutineScope(Dispatchers.Main).launch {
//                AlarmScheduler.rescheduleAll(requireContext())
//            }
//        }

        timePicker.setOnTimeChangedListener { _, hour, minute ->
            AlarmScheduler.saveTime(requireContext(), hour, minute)

            // ✅ ПРОВЕРКА перед пересозданием будильников
            if (!AlarmScheduler.canScheduleExactAlarms(requireContext())) {
                // ⚠️ Показываем диалог
                showNoPermissionDialog()
            } else {


                CoroutineScope(Dispatchers.Main).launch {
                    // ✅ Перед пересозданием проверяем права
                    if (AlarmScheduler.canScheduleExactAlarms(requireContext())) {
                        AlarmScheduler.rescheduleAll(requireContext())
                    } else {
                        // Показать предупреждение что будильники не сработают
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Нет разрешения")
                            .setMessage("Разрешите установку точных будильников в настройках системы")
                            .setPositiveButton("Настройки") { _, _ ->
                                AlarmScheduler.requestExactAlarmPermission(requireContext())
                            }
                            .show()
                    }
                }
            }
        }

        // Добавь Spinner для выбора типа прививки по умолчанию
        val spinnerGraftType = view.findViewById<Spinner>(R.id.spinnerDefaultGraftType)
        val graftTypesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            BreedingCalendar.GRAFT_TYPES
        )
        spinnerGraftType.adapter = graftTypesAdapter

// ✅ ДОБАВИТЬ кнопку в onViewCreated после spinnerGraftType:

        val btnScheduledAlarms = view.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnScheduledAlarms
        )
        btnScheduledAlarms.setOnClickListener {
            findNavController().navigate(
                com.beequeencalendar.R.id.action_settingsFragment_to_scheduledAlarmsFragment
            )
        }

        // Загрузи сохранённое значение
        val savedType = AlarmScheduler.getDefaultGraftType(requireContext())
        spinnerGraftType.setSelection(savedType)

        // Сохраняй при изменении
        spinnerGraftType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                AlarmScheduler.saveDefaultGraftType(requireContext(), pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


    }
    // ✅ ДОБАВИТЬ метод для показа диалога
    private fun showNoPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Нет разрешения")
            .setMessage("Разрешите \"Точные будильники\" в настройках системы, чтобы уведомления работали надёжно.")
            .setPositiveButton("Настройки") { _, _ ->
                AlarmScheduler.requestExactAlarmPermission(requireContext())
            }
            .setNegativeButton("Позже", null)
            .show()
    }
}
