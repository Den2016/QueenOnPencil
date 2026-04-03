package com.beequeencalendar.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.beequeencalendar.databinding.FragmentScheduledAlarmsBinding
import com.beequeencalendar.notification.AlarmScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

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

// ✅ Кнопка пересоздания будильников
        binding.btnRescheduleAll.setOnClickListener {
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
                // Проверка версии Android
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Пересоздать будильники?")
                        .setMessage("⚠️ Android 12+ ограничивает количество будильников до 15 на приложение.\n\nБудут установлены только 15 ближайших уведомлений.")
                        .setPositiveButton("Пересоздать") { _, _ ->
                            performReschedule()
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                } else {
                    // Android 7-11
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Пересоздать будильники?")
                        .setMessage("Все будущие события будут перепланированы.")
                        .setPositiveButton("Пересоздать") { _, _ ->
                            performReschedule()
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            }
        }


        viewModel.loadAlarms()
    }
    // Метод пересоздания
    private fun performReschedule() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AlarmScheduler.rescheduleAll(requireContext())
                viewModel.loadAlarms() // Обновить список
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
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}