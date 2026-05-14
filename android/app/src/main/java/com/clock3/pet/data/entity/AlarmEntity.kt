package com.clock3.pet.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val time: String,
    val content: String = "",
    val repeatType: String = "once",
    val repeatDays: String? = null,
    val enabled: Boolean = true,
    val snoozeCount: Int = 0,
    val snoozeTime: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
