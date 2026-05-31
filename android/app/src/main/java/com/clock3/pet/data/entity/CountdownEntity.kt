package com.clock3.pet.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "countdowns",
    indices = [Index(value = ["status"])]
)
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val targetTime: String,
    val status: String = "running",
    val remainingSeconds: Int = 0,
    val createdAt: String? = null
)
