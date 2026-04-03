package com.beequeencalendar.ui.settings

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beequeencalendar.R
import com.beequeencalendar.data.entity.NotificationRule
import com.beequeencalendar.notification.AlarmScheduler.parseAdvanceDays
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.Spinner
import android.widget.ArrayAdapter
import com.beequeencalendar.data.BreedingCalendar

class RuleEditAdapter(
    private val onRuleChanged: (NotificationRule, Int) -> Unit,
    private val onRuleDeleted: (NotificationRule) -> Unit
) : ListAdapter<NotificationRule, RuleEditAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationRule>() {
            override fun areItemsTheSame(old: NotificationRule, new: NotificationRule) =
                old.eventType == new.eventType
            override fun areContentsTheSame(old: NotificationRule, new: NotificationRule) =
                old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rule_edit, parent, false)
        // ✅ Передаём колбэки в конструктор VH
        return VH(view, onRuleChanged, onRuleDeleted)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position), position)

    class VH(
        itemView: View,
        private val onRuleChanged: (NotificationRule, Int) -> Unit,  // ✅ Принимаем в конструктор
        private val onRuleDeleted: (NotificationRule) -> Unit         // ✅ Принимаем в конструктор
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvEventType: TextView = itemView.findViewById(R.id.tvEventType)
        private val switchEnabled: SwitchMaterial = itemView.findViewById(R.id.switchEnabled)
        private val tvRuleSummary: TextView = itemView.findViewById(R.id.tvRuleSummary)
        private val btnDeleteRule: View = itemView.findViewById(R.id.btnDeleteRule)

        fun bind(rule: NotificationRule, position: Int) {
            tvEventType.text = rule.eventType
            switchEnabled.isChecked = rule.isEnabled

            // ✅ Теперь колбэки доступны напрямую
            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onRuleChanged(rule.copy(isEnabled = isChecked), position)
            }

            btnDeleteRule.setOnClickListener {
                onRuleDeleted(rule)
            }
            tvEventType.setOnClickListener {
                showEditDialog(itemView.context, rule) { updatedRule ->
                    Log.d("ListenerUPDATED",updatedRule.toString())
                    onRuleChanged(updatedRule, position)
                }
            }

            // ✅ Формирование сводки
            val advanceList = parseAdvanceDays(rule.advanceDays).filter { it > 0 }
            val advanceText = if (advanceList.isEmpty()) "Нет" else advanceList.joinToString(", ") { "-${it}д" }
            val repeatText = if (rule.repeatCount > 0) "🔄${rule.repeatCount}x${rule.repeatIntervalMin}м" else "🔄 Нет"
            tvRuleSummary.text = "${String.format("%02d:%02d", rule.timeHour, rule.timeMinute)} | $advanceText $repeatText".trim()

            Log.d("DEBUG.1", advanceList.toString())
            Log.d("DEBUG.2", advanceText)
            Log.d("DEBUG.3", repeatText)
            Log.d("DEBUG", tvRuleSummary.text as String)
        }
        private fun showEditDialog(
            context: Context,
            rule: NotificationRule,
            onSave: (NotificationRule) -> Unit
        ) {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_rule_edit, null)

            val spinnerEventType: Spinner = view.findViewById(R.id.spinnerEventType)
            val etHour: EditText = view.findViewById(R.id.etHour)
            val etMinute: EditText = view.findViewById(R.id.etMinute)
            val chkAdvance1: CheckBox = view.findViewById(R.id.chkAdvance1)
            val chkAdvance2: CheckBox = view.findViewById(R.id.chkAdvance2)
            val chkAdvance3: CheckBox = view.findViewById(R.id.chkAdvance3)
            val etRepeatCount: EditText = view.findViewById(R.id.etRepeatCount)
            val etRepeatInterval: EditText = view.findViewById(R.id.etRepeatInterval)

            // ✅ Заполняем поля текущими значениями
            etHour.setText(rule.timeHour.toString())
            etMinute.setText(rule.timeMinute.toString())
            etRepeatCount.setText(rule.repeatCount.toString())
            etRepeatInterval.setText(rule.repeatIntervalMin.toString())

            val advanceDays = parseAdvanceDays(rule.advanceDays).filter { it > 0 }
            chkAdvance1.isChecked = 1 in advanceDays
            chkAdvance2.isChecked = 2 in advanceDays
            chkAdvance3.isChecked = 3 in advanceDays

            // ✅ ИНИЦИАЛИЗАЦИЯ СПИННЕРА (раньше было пропущено)
            val allTypes = BreedingCalendar.getAllEventTypes()
            spinnerEventType.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, allTypes)
            val currentTypeIndex = allTypes.indexOf(rule.eventType)
            if (currentTypeIndex >= 0) spinnerEventType.setSelection(currentTypeIndex)

            // Опционально: если тип события менять нельзя, раскомментируйте строку ниже
            // spinnerEventType.isEnabled = false

            MaterialAlertDialogBuilder(context)
                .setTitle("Настройка: ${rule.eventType}")
                .setView(view)
                .setPositiveButton("Сохранить") { _, _ ->
                    val selectedDays = mutableListOf(0)
                    if (chkAdvance1.isChecked) selectedDays.add(1)
                    if (chkAdvance2.isChecked) selectedDays.add(2)
                    if (chkAdvance3.isChecked) selectedDays.add(3)
                    val advanceDaysJson = selectedDays.sorted().joinToString(",", "[", "]")
                    Log.d("DEBUG_DIALOG", "etHour.text: '${etHour.text}'")
                    Log.d("DEBUG_DIALOG", "chkAdvance1.isChecked: ${chkAdvance1.isChecked}")
                    Log.d("DEBUG_DIALOG", "chkAdvance2.isChecked: ${chkAdvance2.isChecked}")
                    Log.d("DEBUG_DIALOG", "chkAdvance3.isChecked: ${chkAdvance3.isChecked}")
                    Log.d("DEBUG_DIALOG", "advanceDaysJson: $advanceDaysJson")

                    // ✅ Сохраняем ВСЕ поля, включая выбранный тип из спиннера
                    val updated = rule.copy(
                        eventType = spinnerEventType.selectedItem.toString(),
                        timeHour = etHour.text.toString().toIntOrNull() ?: 8,
                        timeMinute = etMinute.text.toString().toIntOrNull() ?: 0,
                        advanceDays = advanceDaysJson,
                        repeatCount = etRepeatCount.text.toString().toIntOrNull() ?: 0,
                        repeatIntervalMin = etRepeatInterval.text.toString().toIntOrNull() ?: 5
                    )

                    Log.d("DEBUG_DIALOG", "updated rule: $updated")
                    onSave(updated)
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}