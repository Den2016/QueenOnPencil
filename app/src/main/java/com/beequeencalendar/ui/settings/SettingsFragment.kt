package com.beequeencalendar.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.beequeencalendar.R
import com.beequeencalendar.data.BreedingCalendar
import com.beequeencalendar.notification.AlarmScheduler
import com.beequeencalendar.util.DatabaseBackupHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController

class SettingsFragment : Fragment() {

    // ✅ Лаунчеры для SAF (экспорт и импорт)
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        uri?.let { performExport(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { performImport(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
//        timePicker.setIs24HourView(true)
//        timePicker.hour = AlarmScheduler.getHour(requireContext())
//        timePicker.minute = AlarmScheduler.getMinute(requireContext())
//
//
//        timePicker.setOnTimeChangedListener { _, hour, minute ->
//            AlarmScheduler.saveTime(requireContext(), hour, minute)
//
//            // ✅ ПРОВЕРКА перед пересозданием будильников
//            if (!AlarmScheduler.canScheduleExactAlarms(requireContext())) {
//                // ⚠️ Показываем диалог
//                showNoPermissionDialog()
//            } else {
//
//
//                CoroutineScope(Dispatchers.Main).launch {
//                    // ✅ Перед пересозданием проверяем права
//                    if (AlarmScheduler.canScheduleExactAlarms(requireContext())) {
//                        AlarmScheduler.rescheduleAll(requireContext())
//                    } else {
//                        // Показать предупреждение что будильники не сработают
//                        MaterialAlertDialogBuilder(requireContext())
//                            .setTitle("Нет разрешения")
//                            .setMessage("Разрешите установку точных будильников в настройках системы")
//                            .setPositiveButton("Настройки") { _, _ ->
//                                AlarmScheduler.requestExactAlarmPermission(requireContext())
//                            }
//                            .show()
//                    }
//                }
//            }
//        }

        // Добавь Spinner для выбора типа прививки по умолчанию
        val spinnerGraftType = view.findViewById<Spinner>(R.id.spinnerDefaultGraftType)
        val graftTypesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            BreedingCalendar.GRAFT_TYPES
        )
        spinnerGraftType.adapter = graftTypesAdapter

        val savedType = AlarmScheduler.getDefaultGraftType(requireContext())
        spinnerGraftType.setSelection(savedType)

        spinnerGraftType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                AlarmScheduler.saveDefaultGraftType(requireContext(), pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Кнопка просмотра будильников
        val btnScheduledAlarms = view.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnScheduledAlarms
        )
        btnScheduledAlarms.setOnClickListener {
            findNavController().navigate(
                com.beequeencalendar.R.id.action_settingsFragment_to_scheduledAlarmsFragment
            )
        }

// ✅ Кнопка пересоздания всех будильников
        val btnRescheduleAll = view.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnRescheduleAll
        )
        btnRescheduleAll.setOnClickListener {
            if (!AlarmScheduler.canScheduleExactAlarms(requireContext())) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⚠️ Нет разрешения")
                    .setMessage("Разрешите установку точных будильников в настройках системы")
                    .setPositiveButton("Настройки") { _, _ ->
                        AlarmScheduler.requestExactAlarmPermission(requireContext())
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            } else {
                // Проверка на Android 12+ с ограничением
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Пересоздать будильники?")
                        .setMessage("На Android 12+ действует ограничение до 15 ближайших будильников. Старые будут отменены.")
                        .setPositiveButton("Пересоздать") { _, _ ->
                            rescheduleAlarms()
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                } else {
                    // Android 7-11: без ограничений
                    rescheduleAlarms()
                }
            }
        }



// Кнопка управления расписаниями
        val btnSchedules = view.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnNotificationSchedules
        )
        btnSchedules.setOnClickListener {
            findNavController().navigate(
                com.beequeencalendar.R.id.action_settingsFragment_to_scheduleListFragment
            )
        }

        // ✅ Кнопка экспорта
        val btnExport = view.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnExportDatabase
        )
        btnExport.setOnClickListener {
            exportLauncher.launch("QueenOnPencil_${getCurrentDate()}.db")
        }

        // ✅ Кнопка импорта
        val btnImport = view.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnImportDatabase
        )
        btnImport.setOnClickListener {
            showImportWarningDialog()
        }


    }
    // Метод для пересоздания
    private fun rescheduleAlarms() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                AlarmScheduler.rescheduleAll(requireContext())
                Snackbar.make(
                    requireView(),
                    "✅ Будильники пересозданы",
                    Snackbar.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Ошибка")
                    .setMessage("Не удалось пересоздать будильники: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    // ✅ Выполнение экспорта
    private fun performExport(uri: android.net.Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = DatabaseBackupHelper.exportDatabase(requireContext(), uri)
            result.fold(
                onSuccess = {
                    Snackbar.make(
                        requireView(),
                        "✅ База данных экспортирована",
                        Snackbar.LENGTH_LONG
                    ).show()
                },
                onFailure = { error ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Ошибка экспорта")
                        .setMessage(error.message ?: "Неизвестная ошибка")
                        .setPositiveButton("OK", null)
                        .show()
                }
            )
        }
    }

    // ✅ Выполнение импорта
    private fun performImport(uri: android.net.Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = DatabaseBackupHelper.importDatabase(requireContext(), uri)
            result.fold(
                onSuccess = {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("✅ Импорт выполнен")
                        .setMessage("Приложение будет перезапущено для применения изменений.")
                        .setPositiveButton("Перезапустить") { _, _ ->
                            restartApp()
                        }
                        .setNegativeButton("Позже", null)
                        .show()
                },
                onFailure = { error ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Ошибка импорта")
                        .setMessage(error.message ?: "Неизвестная ошибка")
                        .setPositiveButton("OK", null)
                        .show()
                }
            )
        }
    }

    // ✅ Диалог предупреждения перед импортом
    private fun showImportWarningDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Импорт базы данных")
            .setMessage("Все текущие данные будут ЗАМЕНЕНЫ данными из файла.\n\nУбедитесь, что у вас есть актуальная резервная копия!")
            .setPositiveButton("Продолжить") { _, _ ->
                importLauncher.launch(arrayOf("application/x-sqlite3", "*/*"))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun getCurrentDate(): String {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    private fun restartApp() {
        val intent = requireActivity().packageManager
            .getLaunchIntentForPackage(requireContext().packageName)
            ?: return requireActivity().finish()  // Если null — просто завершаем

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        requireActivity().finish()
    }

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
