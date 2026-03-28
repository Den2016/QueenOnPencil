package com.beequeencalendar.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beequeencalendar.databinding.ItemScheduledAlarmBinding
import com.beequeencalendar.notification.AlarmScheduler
import com.beequeencalendar.util.toDisplayDate
import java.text.SimpleDateFormat
import java.util.Locale

class ScheduledAlarmsAdapter(
    private val onCancelClick: (Long) -> Unit
) : ListAdapter<AlarmScheduler.ScheduledAlarmInfo, ScheduledAlarmsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AlarmScheduler.ScheduledAlarmInfo>() {
            override fun areItemsTheSame(
                oldItem: AlarmScheduler.ScheduledAlarmInfo,
                newItem: AlarmScheduler.ScheduledAlarmInfo
            ) = oldItem.eventId == newItem.eventId

            override fun areContentsTheSame(
                oldItem: AlarmScheduler.ScheduledAlarmInfo,
                newItem: AlarmScheduler.ScheduledAlarmInfo
            ) = oldItem == newItem
        }

        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemScheduledAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position), onCancelClick)

    class VH(private val b: ItemScheduledAlarmBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(alarm: AlarmScheduler.ScheduledAlarmInfo, onCancelClick: (Long) -> Unit) {
            b.tvEventDesc.text = alarm.eventDesc
            b.tvEventDate.text = alarm.eventDate.toDisplayDate()
            b.tvAlarmTime.text = TIME_FORMAT.format(alarm.alarmTime)

            // Индикатор типа будильника
            b.tvAlarmType.text = if (alarm.isExactAlarm) "🔔 Точный" else "⏰ Обычный"
            b.tvAlarmType.setTextColor(
                if (alarm.isExactAlarm)
                    b.root.context.getColor(android.R.color.holo_green_dark)
                else
                    b.root.context.getColor(android.R.color.holo_orange_dark)
            )

            b.btnDelete.setOnClickListener { onCancelClick(alarm.eventId) }
        }
    }
}