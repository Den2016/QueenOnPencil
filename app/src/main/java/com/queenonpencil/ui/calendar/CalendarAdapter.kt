package com.queenonpencil.ui.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.queenonpencil.data.BreedingCalendar
import com.queenonpencil.data.dao.CalendarEvent
import com.queenonpencil.databinding.ItemCalendarEventBinding
import com.queenonpencil.databinding.ItemCalendarHeaderBinding
import com.queenonpencil.util.toDisplayDate

sealed class CalendarItem {
    data class Header(val date: String) : CalendarItem()
    data class EventItem(val event: CalendarEvent) : CalendarItem()
}

class CalendarAdapter(
    private val onNoteClick: (eventId: Long, currentNote: String) -> Unit,
    private val onGraftClick: (graftingId: Long) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<CalendarItem>()

    fun submitList(events: List<CalendarEvent>) {
        val grouped = events.groupBy { it.eventDt }
        val result = mutableListOf<CalendarItem>()
        for ((date, evts) in grouped) {
            result.add(CalendarItem.Header(date))
            evts.forEach { result.add(CalendarItem.EventItem(it)) }
        }
        items = result
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is CalendarItem.Header -> 0
        is CalendarItem.EventItem -> 1
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            HeaderVH(ItemCalendarHeaderBinding.inflate(inflater, parent, false))
        } else {
            EventVH(ItemCalendarEventBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is CalendarItem.Header -> (holder as HeaderVH).bind(item)
            is CalendarItem.EventItem -> (holder as EventVH).bind(item)
        }
    }

    inner class HeaderVH(private val b: ItemCalendarHeaderBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(item: CalendarItem.Header) {
            b.tvDate.text = item.date.toDisplayDate()
        }
    }

    inner class EventVH(private val b: ItemCalendarEventBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(item: CalendarItem.EventItem) {
            val ev = item.event
            b.tvEventDesc.text = ev.eventDesc
            b.tvGraftInfo.text = "Прививка ${ev.graftingDt.toDisplayDate()}" +
                    if (ev.graftingDesc.isNotBlank()) " — ${ev.graftingDesc}" else ""

            //val color = BreedingCalendar.GRAFT_COLORS.getOrElse(ev.graftingTp) { 0xFF9E9E9E.toInt() }
            val color = BreedingCalendar.getColorForGraftType(0)
            b.colorBg.setBackgroundColor(Color.argb(77, Color.red(color), Color.green(color), Color.blue(color)))

            if (ev.eventNote.isNotBlank()) {
                b.tvNote.visibility = View.VISIBLE
                b.tvNote.text = ev.eventNote
            } else {
                b.tvNote.visibility = View.GONE
            }

            b.root.setOnClickListener { onNoteClick(ev.eventId, ev.eventNote) }
            b.root.setOnLongClickListener {
                onGraftClick(ev.graftingId)
                true
            }
        }
    }
}
