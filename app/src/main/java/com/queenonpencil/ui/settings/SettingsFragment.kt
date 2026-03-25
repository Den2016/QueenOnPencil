package com.queenonpencil.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import com.queenonpencil.R
import com.queenonpencil.data.BreedingCalendar
import com.queenonpencil.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
        timePicker.setIs24HourView(true)
        timePicker.hour = AlarmScheduler.getHour(requireContext())
        timePicker.minute = AlarmScheduler.getMinute(requireContext())

        timePicker.setOnTimeChangedListener { _, hour, minute ->
            AlarmScheduler.saveTime(requireContext(), hour, minute)
            CoroutineScope(Dispatchers.Main).launch {
                AlarmScheduler.rescheduleAll(requireContext())
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
}
