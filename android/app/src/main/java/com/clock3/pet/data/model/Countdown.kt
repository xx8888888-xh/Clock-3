package com.clock3.pet.data.model

import com.clock3.pet.data.entity.CountdownEntity
import com.clock3.pet.utils.AppLog
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Countdown(
    val id: Long = 0,
    val label: String,
    val targetTime: LocalDateTime,
    val status: CountdownStatus = CountdownStatus.RUNNING,
    var remainingSeconds: Int = 0,
    val createdAt: String? = null
) {
    enum class CountdownStatus {
        RUNNING, PAUSED, COMPLETED
    }

    fun updateRemaining() {
        if (status == CountdownStatus.RUNNING) {
            remainingSeconds = Duration.between(LocalDateTime.now(), targetTime).seconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        }
    }

    fun getFormattedTime(): String {
        updateRemaining()
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

    fun pause(): Countdown {
        if (status != CountdownStatus.RUNNING) return this
        val remaining = Duration.between(LocalDateTime.now(), targetTime).seconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        return this.copy(
            status = CountdownStatus.PAUSED,
            remainingSeconds = remaining
        )
    }

    fun resume(): Countdown {
        if (status != CountdownStatus.PAUSED) return this
        return this.copy(
            targetTime = LocalDateTime.now().plusSeconds(remainingSeconds.toLong()),
            status = CountdownStatus.RUNNING
        )
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

    fun toExportMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "label" to label,
            "targetTime" to targetTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "status" to status.name,
            "remainingSeconds" to remainingSeconds,
            "createdAt" to (createdAt ?: "")
        )
    }

    companion object {
        fun fromEntity(entity: CountdownEntity): Countdown {
            val status = try {
                Countdown.CountdownStatus.valueOf(entity.status.uppercase())
            } catch (e: Exception) {
                AppLog.w("Countdown", "Invalid status from DB: ${entity.status}")
                Countdown.CountdownStatus.COMPLETED
            }

            return Countdown(
                id = entity.id,
                label = entity.label,
                targetTime = LocalDateTime.parse(entity.targetTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status = status,
                remainingSeconds = entity.remainingSeconds,
                createdAt = entity.createdAt
            )
        }

        fun fromExportMap(map: Map<*, *>): Countdown {
            val targetTimeStr = (map["targetTime"] as? String) ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val targetTime = try {
                LocalDateTime.parse(targetTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) {
                LocalDateTime.now()
            }
            
            val rawStatus = (map["status"] as? String)?.let {
                try {
                    Countdown.CountdownStatus.valueOf(it.uppercase())
                } catch (e: Exception) {
                    Countdown.CountdownStatus.RUNNING
                }
            } ?: Countdown.CountdownStatus.RUNNING
            val finalStatus = if (rawStatus == CountdownStatus.RUNNING && targetTime.isBefore(LocalDateTime.now())) {
                CountdownStatus.COMPLETED
            } else {
                rawStatus
            }

            return Countdown(
                id = 0,
                label = (map["label"] as? String) ?: "倒计时",
                targetTime = targetTime,
                status = finalStatus,
                remainingSeconds = (map["remainingSeconds"] as? Number)?.toInt() ?: 0,
                createdAt = (map["createdAt"] as? String)?.let { if (it.isNotEmpty()) it else null }
            )
        }

        fun create(label: String, seconds: Int): Countdown {
            val safeSeconds = seconds.coerceAtLeast(0)
            return Countdown(
                label = label,
                targetTime = LocalDateTime.now().plusSeconds(safeSeconds.toLong()),
                remainingSeconds = safeSeconds
            )
        }
    }
}
