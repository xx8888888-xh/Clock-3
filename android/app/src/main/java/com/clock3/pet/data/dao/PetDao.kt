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

    @Query("UPDATE pet_data SET level = :newLevel, exp = :newExp WHERE id = 1 AND level = :expectedLevel AND exp = :expectedExp")
    suspend fun atomicUpdateExp(newLevel: Int, newExp: Int, expectedLevel: Int, expectedExp: Int): Int
}
