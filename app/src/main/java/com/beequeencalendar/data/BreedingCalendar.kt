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

    private val DRON_EVENTS = listOf(
        0 to "Яйцо трутневое",
        15 to "Можно планировать вывод маток",
        37 to "Отбор трута для ИО",
        48 to "Окончание срока годности трута для ИО"
    )

    // Возраст на момент прививки → сдвиг назад к дате кладки яйца
    // shift=0: яйцо (0 дн от кладки)
    // shift=1: однодневная личинка (4 дн от кладки: 3 дня яйцо + 1 день личинка)
    // shift=2: двухдневная личинка (5 дн от кладки)
    // shift=3: маточник (11 дн от кладки — уже запечатан)
    val GRAFT_TYPES = arrayOf(
        "Яйцо 1 день",
        "Яйцо 2 дня",
        "Яйцо 3 дня",
        "Личинка 1 день",
        "Личинка 2 дня",
        "Маточник (запечатан)"
    )

    val DRON_TYPES = arrayOf(
        "Яйцо 1 день",
        "Личинка 1 день",
        "Печатка трутня"
    )

    private val AGE_OFFSETS = intArrayOf(0, 1, 2, 3, 4, 8)
    private val DRONE_OFFSETS = intArrayOf(0, 3, 9)  // ✅ для трутней

    private val FMT = DateTimeFormatter.ISO_LOCAL_DATE


    fun generateEvents(graftingId: Long, graftingDate: String, shift: Int, tp: Int): List<Event> {
        val eggDate = calcEggDate(graftingDate, shift, tp)
        if(tp==1){
            return DRON_EVENTS
                .filter { (day, _) -> day > dronOffset(shift) }
                .map { (dayOffset, description) ->
                    Event(
                        graftingId = graftingId,
                        dt = eggDate.plusDays(dayOffset.toLong()).format(FMT),
                        desc = description
                    )
                }
        }
        return EVENTS
            .filter { (day, _) -> day > ageOffset(shift) }
            .map { (dayOffset, description) ->
                Event(
                    graftingId = graftingId,
                    dt = eggDate.plusDays(dayOffset.toLong()).format(FMT),
                    desc = description
                )
            }
    }

    fun previewEvents(graftingDate: String, shift: Int, tp: Int): List<Pair<String, String>> {
        val eggDate = calcEggDate(graftingDate, shift, tp)
        val result = mutableListOf<Pair<String, String>>()
        if (shift > 0) {
            result.add(eggDate.format(FMT) to "Дата кладки яйца")
        }
        if(tp == 0) {
            EVENTS
                .filter { (day, _) -> day > ageOffset(shift) }
                .forEach { (dayOffset, description) ->
                    result.add(eggDate.plusDays(dayOffset.toLong()).format(FMT) to description)
                }
        }
        if(tp == 1){
            DRON_EVENTS
                .filter { (day, _) -> day > dronOffset(shift) }
                .forEach { (dayOffset, description) ->
                    result.add(eggDate.plusDays(dayOffset.toLong()).format(FMT) to description)
                }

        }
        return result
    }

//    fun eggDateString(graftingDate: String, shift: Int): String? {
//        if (shift == 0) return null
//        return calcEggDate(graftingDate, shift).format(FMT)
//    }

    private fun calcEggDate(graftingDate: String, shift: Int, tp: Int): LocalDate {
        val graftDate = LocalDate.parse(graftingDate, FMT)
        if(tp == 0) {
            return graftDate.minusDays(ageOffset(shift).toLong())
        }else{
            return graftDate.minusDays(dronOffset(shift).toLong())
        }
    }

    private fun ageOffset(shift: Int): Int =
        AGE_OFFSETS.getOrElse(shift) { 0 }

    private fun dronOffset(shift: Int): Int =
        DRONE_OFFSETS.getOrElse(shift) { 0 }

}
