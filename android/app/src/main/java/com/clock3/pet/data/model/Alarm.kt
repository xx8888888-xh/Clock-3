package com.clock3.pet.data.model

import com.clock3.pet.data.entity.AlarmEntity
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Alarm(
    val id: Long = 0,
    val label: String,
    val time: String,
    val content: String = "",
    val repeatType: RepeatType = RepeatType.ONCE,
    val repeatDays: List<Int> = emptyList(),
    val enabled: Boolean = true,
    val snoozeCount: Int = 0,
    val snoozeTime: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    enum class RepeatType(val value: String) {
        ONCE("once"),
        DAILY("daily"),
        WORKDAYS("workdays"),
        WEEKEND("weekend"),
        CUSTOM("custom");

        companion object {
            fun fromValue(value: String): RepeatType {
                return entries.find { it.value == value } ?: ONCE
            }
        }
    }

    fun getNextTriggerTime(): LocalDateTime? {
        val now = LocalDateTime.now()
        val parts = time.split(":")
        if (parts.size != 2) return null

        val targetTime = LocalTime.of(parts[0].toIntOrNull() ?: 8, parts[1].toIntOrNull() ?: 0)
        var target = LocalDateTime.of(now.toLocalDate(), targetTime)

        if (repeatType == RepeatType.ONCE) {
            return if (target.isAfter(now)) target else null
        }

        if (target.isBefore(now) || target.isEqual(now)) {
            target = target.plusDays(1)
        }

        val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val weekendDays = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        when (repeatType) {
            RepeatType.DAILY -> {
                while (target.isBefore(now) || target.isEqual(now)) {
                    target = target.plusDays(1)
                }
                return target
            }
            RepeatType.WORKDAYS -> {
                while (true) {
                    if (target.dayOfWeek in weekdays && (target.isAfter(now) || target.isEqual(now))) {
                        return target
                    }
                    target = target.plusDays(1)
                }
            }
            RepeatType.WEEKEND -> {
                while (true) {
                    if (target.dayOfWeek in weekendDays && (target.isAfter(now) || target.isEqual(now))) {
                        return target
                    }
                    target = target.plusDays(1)
                }
            }
            RepeatType.CUSTOM -> {
                while (true) {
                    if (repeatDays.contains(target.dayOfWeek.value) && (target.isAfter(now) || target.isEqual(now))) {
                        return target
                    }
                    target = target.plusDays(1)
                }
            }
            else -> return target
        }
    }

    fun shouldTriggerToday(): Boolean {
        val today = LocalDateTime.now().dayOfWeek.value
        return when (repeatType) {
            RepeatType.ONCE, RepeatType.DAILY -> true
            RepeatType.WORKDAYS -> today in 1..5
            RepeatType.WEEKEND -> today in 6..7
            RepeatType.CUSTOM -> repeatDays.contains(today)
        }
    }

    fun toEntity(): AlarmEntity {
        return AlarmEntity(
            id = id,
            label = label,
            time = time,
            content = content,
            repeatType = repeatType.value,
            repeatDays = if (repeatDays.isNotEmpty()) repeatDays.joinToString(",") else null,
            enabled = enabled,
            snoozeCount = snoozeCount,
            snoozeTime = snoozeTime,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromEntity(entity: AlarmEntity): Alarm {
            return Alarm(
                id = entity.id,
                label = entity.label,
                time = entity.time,
                content = entity.content,
                repeatType = RepeatType.fromValue(entity.repeatType),
                repeatDays = entity.repeatDays?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
                enabled = entity.enabled,
                snoozeCount = entity.snoozeCount,
                snoozeTime = entity.snoozeTime,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
