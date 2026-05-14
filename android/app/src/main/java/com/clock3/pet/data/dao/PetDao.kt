package com.clock3.pet.data.dao

import androidx.room.*
import com.clock3.pet.data.entity.PetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_data WHERE id = 1")
    fun getPet(): Flow<PetEntity?>

    @Query("SELECT * FROM pet_data WHERE id = 1")
    suspend fun getPetSync(): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePet(pet: PetEntity)

    @Query("UPDATE pet_data SET mood = :mood WHERE id = 1")
    suspend fun updateMood(mood: String)

    @Query("UPDATE pet_data SET level = :level, exp = :exp WHERE id = 1")
    suspend fun updateLevelAndExp(level: Int, exp: Int)

    @Query("UPDATE pet_data SET totalInteractions = totalInteractions + 1, lastInteraction = :timestamp WHERE id = 1")
    suspend fun incrementInteractions(timestamp: String)

    @Query("UPDATE pet_data SET name = :name WHERE id = 1")
    suspend fun updateName(name: String)
}
