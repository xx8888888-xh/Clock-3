package com.clock3.pet.data.dao

import androidx.room.*
import com.clock3.pet.data.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarms: List<AlarmEntity>)

    @Transaction
    suspend fun importAlarms(alarms: List<AlarmEntity>) {
        deleteAllAlarms()
        insertAll(alarms)
    }
    @Query("SELECT * FROM alarms ORDER BY time ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY time ASC")
    suspend fun getAllAlarmsSync(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY time ASC")
    suspend fun getEnabledAlarms(): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET snoozeTime = :snoozeTime, snoozeCount = snoozeCount + 1 WHERE id = :id")
    suspend fun setSnoozeTimeAndIncrementCount(id: Long, snoozeTime: String)

    @Query("UPDATE alarms SET snoozeTime = NULL, snoozeCount = 0 WHERE id = :id")
    suspend fun clearSnooze(id: Long)

    @Query("DELETE FROM alarms")
    suspend fun deleteAllAlarms()
}
