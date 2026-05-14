package com.clock3.pet.data.dao

import androidx.room.*
import com.clock3.pet.data.entity.CountdownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdowns ORDER BY targetTime ASC")
    fun getAllCountdowns(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdowns ORDER BY targetTime ASC")
    suspend fun getAllCountdownsSync(): List<CountdownEntity>

    @Query("SELECT * FROM countdowns WHERE id = :id")
    suspend fun getCountdownById(id: Long): CountdownEntity?

    @Query("SELECT * FROM countdowns WHERE status = :status ORDER BY targetTime ASC")
    suspend fun getCountdownsByStatus(status: String): List<CountdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountdown(countdown: CountdownEntity): Long

    @Update
    suspend fun updateCountdown(countdown: CountdownEntity)

    @Delete
    suspend fun deleteCountdown(countdown: CountdownEntity)

    @Query("DELETE FROM countdowns WHERE id = :id")
    suspend fun deleteCountdownById(id: Long)

    @Query("UPDATE countdowns SET status = :status WHERE id = :id")
    suspend fun setCountdownStatus(id: Long, status: String)

    @Query("UPDATE countdowns SET remainingSeconds = :seconds WHERE id = :id")
    suspend fun setRemainingSeconds(id: Long, seconds: Int)
}
