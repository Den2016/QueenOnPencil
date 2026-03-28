package com.beequeencalendar.ui.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beequeencalendar.data.dao.CalendarEvent
import com.beequeencalendar.util.toDisplayDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.beequeencalendar.R
import com.beequeencalendar.databinding.ItemCalendarDayBinding

// Цвета для карточек дней (можно вынести в colors.xml)
private val COLOR_TODAY = Color.parseColor("#4CAF50")      // зелёный
private val COLOR_TOMORROW = Color.parseColor("#FF9800")    // оранжевый
private val COLOR_DAY_AFTER = Color.parseColor("#29B6F6")   // голубой
private val COLOR_DEFAULT = Color.WHITE                     // белый

// Лейблы
private const val LABEL_TODAY = "Сегодня"
private const val LABEL_TOMORROW = "Завтра"
private const val LABEL_DAY_AFTER = "Послезавтра"


// Убираем sealed class - теперь единица списка = день со списком событий
data class CalendarDay(
    val date: String,
    val events: List<CalendarEvent>
)

// Определяем смещение дня относительно сегодня
private fun getDayOffset(dateStr: String): Int {
    return try {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        (date.toEpochDay() - today.toEpochDay()).toInt()
    } catch (e: Exception) {
        Int.MAX_VALUE
    }
}

// Возвращаем цвет фона и лейбл для карточки дня
private fun getDayCardStyle(context: Context, dateStr: String): Pair<Int, String?> {
    return when (getDayOffset(dateStr)) {
        0 -> ContextCompat.getColor(context, R.color.calendar_today) to "Сегодня"
        1 -> ContextCompat.getColor(context, R.color.calendar_tomorrow) to "Завтра"
        2 -> ContextCompat.getColor(context, R.color.calendar_day_after) to "Послезавтра"
        else -> ContextCompat.getColor(context, R.color.calendar_default) to null
    }
}

class CalendarAdapter(
    private val onNoteClick: (eventId: Long, currentNote: String) -> Unit,
    private val onGraftClick: (graftingId: Long) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<CalendarDay>()

    fun submitList(events: List<CalendarEvent>) {
        val grouped = events.groupBy { it.eventDt }

        // Формируем список дней
        val days = grouped.map { (date, dayEvents) ->
            CalendarDay(date = date, events = dayEvents.sortedBy { it.eventDt })
        }.sortedBy { it.date } // Сортируем по дате

        items = days
        notifyDataSetChanged()
    }


    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DayVH(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val day = items[position] as CalendarDay
        (holder as DayVH).bind(day, onNoteClick, onGraftClick)
    }

    inner class DayVH(private val b: ItemCalendarDayBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(
            day: CalendarDay,
            onNoteClick: (eventId: Long, currentNote: String) -> Unit,
            onGraftClick: (graftingId: Long) -> Unit
        ) {
            val context = b.root.context

            // 1. Устанавливаем дату
            b.tvDate.text = day.date.toDisplayDate()

            // 2. Применяем цвет фона и лейбл из ресурсов
            val (bgColor, label) = getDayCardStyle(context, day.date)
            b.dayCard.setCardBackgroundColor(bgColor)

            // 3. Показываем/скрываем лейбл
            if (label != null) {
                b.tvDayLabel.visibility = View.VISIBLE
                b.tvDayLabel.text = label
                // Цвет фона лейбла из ресурса
                b.tvDayLabel.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.day_label_bg)
                )
            } else {
                b.tvDayLabel.visibility = View.GONE
            }

            // 4. Очищаем контейнер событий
            b.eventsContainer.removeAllViews()

            // 5. Рендерим события ВНУТРИ карточки
            if (day.events.isEmpty()) {
                b.tvNoEvents.visibility = View.VISIBLE
            } else {
                b.tvNoEvents.visibility = View.GONE

                day.events.forEachIndexed { index, ev ->
                    // Инфлейтим event-view ВНУТРЬ контейнера
                    val eventView = LayoutInflater.from(context)
                        .inflate(R.layout.item_calendar_event, b.eventsContainer, false)

                    // Заполняем данными
                    eventView.findViewById<TextView>(R.id.tvEventDesc).text = ev.eventDesc
                    eventView.findViewById<TextView>(R.id.tvGraftInfo).text =
                        "Прививка ${ev.graftingDt.toDisplayDate()}" +
                                if (ev.graftingDesc.isNotBlank()) " — ${ev.graftingDesc}" else ""

                    // Заметка
                    val noteView = eventView.findViewById<TextView>(R.id.tvNote)
                    if (ev.eventNote.isNotBlank()) {
                        noteView.visibility = View.VISIBLE
                        noteView.text = ev.eventNote
                    } else {
                        noteView.visibility = View.GONE
                    }

                    // ❌ УБИРАЕМ цветной фон по типу прививки (теперь фон у карточки общий)
                    eventView.findViewById<View>(R.id.colorBg)?.visibility = View.GONE

                    // Клики
                    eventView.setOnClickListener { onNoteClick(ev.eventId, ev.eventNote) }
                    eventView.setOnLongClickListener {
                        onGraftClick(ev.graftingId)
                        true
                    }

                    b.eventsContainer.addView(eventView)

                    // Добавляем разделитель между событиями (кроме последнего)
                    if (index < day.events.lastIndex) {
                        val divider = View(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                1
                            ).apply {
                                marginStart = 16
                                marginEnd = 16
                            }
                            setBackgroundColor(ContextCompat.getColor(context, R.color.primary_light))
                        }
                        b.eventsContainer.addView(divider)
                    }
                }
            }
        }
    }}
