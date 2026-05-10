package com.clock3.pet.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_data")
data class PetEntity(
    @PrimaryKey
    val id: Int = 1,
    val mood: String = "happy",
    val level: Int = 1,
    val exp: Int = 0,
    val name: String = "小宠物",
    val lastInteraction: String? = null,
    val totalInteractions: Int = 0
)
