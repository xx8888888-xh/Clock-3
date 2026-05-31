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

        val hour = parts[0].toIntOrNull()
        val minute = parts[1].toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) return null
        val targetTime = LocalTime.of(hour, minute)
        var target = LocalDateTime.of(now.toLocalDate(), targetTime)

        if (target.isBefore(now) || target.isEqual(now)) {
            target = target.plusDays(1)
        }

        val maxAttempts = 365

        when (repeatType) {
            RepeatType.ONCE -> {
                return if (target.isAfter(now)) target else null
            }
            RepeatType.DAILY -> {
                return target
            }
            RepeatType.WORKDAYS -> {
                var attempts = 0
                while (attempts < maxAttempts) {
                    if (target.dayOfWeek in WEEKDAYS && target.isAfter(now)) {
                        return target
                    }
                    target = target.plusDays(1)
                    attempts++
                }
                return null
            }
            RepeatType.WEEKEND -> {
                var attempts = 0
                while (attempts < maxAttempts) {
                    if (target.dayOfWeek in WEEKEND_DAYS && target.isAfter(now)) {
                        return target
                    }
                    target = target.plusDays(1)
                    attempts++
                }
                return null
            }
            RepeatType.CUSTOM -> {
                if (repeatDays.isEmpty()) return null
                var attempts = 0
                while (attempts < maxAttempts) {
                    if (repeatDays.contains(target.dayOfWeek.value) && target.isAfter(now)) {
                        return target
                    }
                    target = target.plusDays(1)
                    attempts++
                }
                return null
            }
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

    fun toExportMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "label" to label,
            "time" to time,
            "content" to content,
            "repeatType" to repeatType.value,
            "repeatDays" to repeatDays,
            "enabled" to enabled,
            "snoozeCount" to snoozeCount,
            "snoozeTime" to (snoozeTime ?: ""),
            "createdAt" to (createdAt ?: ""),
            "updatedAt" to (updatedAt ?: "")
        )
    }

    companion object {
        private val WEEKDAYS = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        private val WEEKEND_DAYS = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

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

        fun fromExportMap(map: Map<*, *>): Alarm {
            return Alarm(
                id = 0,
                label = (map["label"] as? String) ?: "闹钟",
                time = (map["time"] as? String) ?: "08:00",
                content = (map["content"] as? String) ?: "",
                repeatType = (map["repeatType"] as? String)?.let { RepeatType.fromValue(it) } ?: RepeatType.ONCE,
                repeatDays = (map["repeatDays"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
                enabled = (map["enabled"] as? Boolean) ?: true,
                snoozeCount = (map["snoozeCount"] as? Number)?.toInt() ?: 0,
                snoozeTime = (map["snoozeTime"] as? String)?.let { if (it.isNotEmpty()) it else null },
                createdAt = (map["createdAt"] as? String)?.let { if (it.isNotEmpty()) it else null },
                updatedAt = (map["updatedAt"] as? String)?.let { if (it.isNotEmpty()) it else null }
            )
        }
    }
}
