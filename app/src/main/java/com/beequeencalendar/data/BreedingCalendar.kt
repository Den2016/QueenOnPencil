package com.beequeencalendar.data

import com.beequeencalendar.data.entity.Event
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BreedingCalendar {

    // Сдвиги дней от даты кладки яйца (день 0)
    private val EVENTS = listOf(
        3 to "Однодневная личинка",
        5 to "Контроль приёма",
        8 to "Запечатка маточника",
        13 to "Отбор (бигуди)",
        14 to "Выход матки (два дня)",
        21 to "Начало облёта",
        27 to "Контроль засева"
    )

    // Возраст на момент прививки → сдвиг назад к дате кладки яйца
    // tp=0: яйцо (0 дн от кладки)
    // tp=1: однодневная личинка (4 дн от кладки: 3 дня яйцо + 1 день личинка)
    // tp=2: двухдневная личинка (5 дн от кладки)
    // tp=3: маточник (11 дн от кладки — уже запечатан)
    val GRAFT_TYPES = arrayOf(
        "Яйцо 1 день",
        "Яйцо 2 дня",
        "Яйцо 3 дня",
        "Личинка 1 день",
        "Личинка 2 дня",
        "Маточник (запечатан)"
    )

    // Цвета для каждого типа прививки (tp 0–6)
    val GRAFT_COLORS = intArrayOf(
        0xFF66BB6A.toInt(), // Яйцо 1 день — светло-зелёный
        0xFF29B6F6.toInt(), // Яйцо 2 дня — голубой
        0xFF42A5F5.toInt(), // Яйцо 3 дня — синий
        0xFFFF9800.toInt(), // Личинка 1 день — оранжевый
        0xFFFF7043.toInt(), // Личинка 2 дня — красно-оранжевый
        0xFFAB47BC.toInt()  // Маточник — фиолетовый
    )

    private val AGE_OFFSETS = intArrayOf(0, 1, 2, 3, 4, 11)

    private val FMT = DateTimeFormatter.ISO_LOCAL_DATE

    fun generateEvents(graftingId: Long, graftingDate: String, tp: Int): List<Event> {
        val eggDate = calcEggDate(graftingDate, tp)
        return EVENTS
            .filter { (day, _) -> day > ageOffset(tp) }
            .map { (dayOffset, description) ->
                Event(
                    graftingId = graftingId,
                    dt = eggDate.plusDays(dayOffset.toLong()).format(FMT),
                    desc = description
                )
            }
    }

    fun previewEvents(graftingDate: String, tp: Int): List<Pair<String, String>> {
        val eggDate = calcEggDate(graftingDate, tp)
        val result = mutableListOf<Pair<String, String>>()
        if (tp > 0) {
            result.add(eggDate.format(FMT) to "Дата кладки яйца")
        }
        EVENTS
            .filter { (day, _) -> day > ageOffset(tp) }
            .forEach { (dayOffset, description) ->
                result.add(eggDate.plusDays(dayOffset.toLong()).format(FMT) to description)
            }
        return result
    }

    fun eggDateString(graftingDate: String, tp: Int): String? {
        if (tp == 0) return null
        return calcEggDate(graftingDate, tp).format(FMT)
    }

    private fun calcEggDate(graftingDate: String, tp: Int): LocalDate {
        val graftDate = LocalDate.parse(graftingDate, FMT)
        return graftDate.minusDays(ageOffset(tp).toLong())
    }

    private fun ageOffset(tp: Int): Int =
        AGE_OFFSETS.getOrElse(tp) { 0 }

    // TODO: рефакторинг цветовой схемы календаря
    fun getColorForGraftType(tp: Int): Int =
        GRAFT_COLORS.getOrElse(tp) { 0xFFAB47BC.toInt() } // дефолт — фиолетовый
}
