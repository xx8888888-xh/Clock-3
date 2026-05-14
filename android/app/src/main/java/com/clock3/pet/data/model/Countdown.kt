package com.clock3.pet.data.model

import com.clock3.pet.data.entity.CountdownEntity
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Countdown(
    val id: Long = 0,
    val label: String,
    val targetTime: LocalDateTime,
    val status: CountdownStatus = CountdownStatus.RUNNING,
    val remainingSeconds: Int = 0,
    val createdAt: String? = null
) {
    enum class CountdownStatus {
        RUNNING, PAUSED, COMPLETED
    }

    fun updateRemaining(): Int {
        if (status == CountdownStatus.RUNNING) {
            remainingSeconds = Duration.between(LocalDateTime.now(), targetTime).seconds.toInt()
            if (remainingSeconds <= 0) {
                remainingSeconds = 0
            }
        }
        return remainingSeconds
    }

    fun getFormattedTime(): String {
        val totalSeconds = remainingSeconds.coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun pause() {
        remainingSeconds = updateRemaining()
    }

    fun toEntity(): CountdownEntity {
        return CountdownEntity(
            id = id,
            label = label,
            targetTime = targetTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = status.name.lowercase(),
            remainingSeconds = remainingSeconds,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromEntity(entity: CountdownEntity): Countdown {
            return Countdown(
                id = entity.id,
                label = entity.label,
                targetTime = LocalDateTime.parse(entity.targetTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status = Countdown.CountdownStatus.valueOf(entity.status.uppercase()),
                remainingSeconds = entity.remainingSeconds,
                createdAt = entity.createdAt
            )
        }

        fun create(label: String, seconds: Int): Countdown {
            return Countdown(
                label = label,
                targetTime = LocalDateTime.now().plusSeconds(seconds.toLong()),
                remainingSeconds = seconds
            )
        }
    }
}
