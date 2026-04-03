package com.beequeencalendar.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beequeencalendar.data.entity.NotificationSchedule
import com.beequeencalendar.databinding.ItemScheduleBinding

class ScheduleListAdapter(
    private val onClick: (NotificationSchedule) -> Unit,
    private val onSetDefault: (NotificationSchedule) -> Unit
) : ListAdapter<NotificationSchedule, ScheduleListAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationSchedule>() {
            override fun areItemsTheSame(old: NotificationSchedule, new: NotificationSchedule) =
                old.id == new.id
            override fun areContentsTheSame(old: NotificationSchedule, new: NotificationSchedule) =
                old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position), onClick, onSetDefault)

    class VH(private val b: ItemScheduleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(
            schedule: NotificationSchedule,
            onClick: (NotificationSchedule) -> Unit,
            onSetDefault: (NotificationSchedule) -> Unit
        ) {
            b.tvName.text = schedule.name
            b.tvDefault.visibility = if (schedule.isDefault) View.VISIBLE else View.GONE

            b.root.setOnClickListener { onClick(schedule) }

            b.btnSetDefault.apply {
                visibility = if (schedule.isDefault) View.GONE else View.VISIBLE
                setOnClickListener { onSetDefault(schedule) }
            }
        }
    }
}