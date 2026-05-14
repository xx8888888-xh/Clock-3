package com.clock3.pet.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.clock3.pet.data.Clock3Database
import com.clock3.pet.data.model.Alarm
import com.clock3.pet.data.model.Countdown
import com.clock3.pet.data.model.Pet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Clock3Repository(context: Context) {
    private val database = Clock3Database.getDatabase(context)
    private val alarmDao = database.alarmDao()
    private val countdownDao = database.countdownDao()
    private val petDao = database.petDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("clock3_prefs", Context.MODE_PRIVATE)

    val alarms: Flow<List<Alarm>> = alarmDao.getAllAlarms().map { entities ->
        entities.map { Alarm.fromEntity(it) }
    }

    val countdowns: Flow<List<Countdown>> = countdownDao.getAllCountdowns().map { entities ->
        entities.map { Countdown.fromEntity(it) }
    }

    val pet: Flow<Pet?> = petDao.getPet().map { entity ->
        entity?.let { Pet.fromEntity(it) }
    }

    suspend fun getAllAlarmsSync(): List<Alarm> = withContext(Dispatchers.IO) {
        alarmDao.getAllAlarmsSync().map { Alarm.fromEntity(it) }
    }

    suspend fun getEnabledAlarms(): List<Alarm> = withContext(Dispatchers.IO) {
        alarmDao.getEnabledAlarms().map { Alarm.fromEntity(it) }
    }

    suspend fun addAlarm(alarm: Alarm): Long = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val entity = alarm.copy(createdAt = now, updatedAt = now).toEntity()
        alarmDao.insertAlarm(entity)
    }

    suspend fun updateAlarm(alarm: Alarm) = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val entity = alarm.copy(updatedAt = now).toEntity()
        alarmDao.updateAlarm(entity)
    }

    suspend fun deleteAlarm(alarmId: Long) = withContext(Dispatchers.IO) {
        alarmDao.deleteAlarmById(alarmId)
    }

    suspend fun toggleAlarm(alarmId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        alarmDao.setAlarmEnabled(alarmId, enabled)
    }

    suspend fun snoozeAlarm(alarmId: Long, minutes: Int) = withContext(Dispatchers.IO) {
        val snoozeTime = LocalDateTime.now().plusMinutes(minutes.toLong())
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        alarmDao.setSnoozeTime(alarmId, snoozeTime)
    }

    suspend fun clearAlarmSnooze(alarmId: Long) = withContext(Dispatchers.IO) {
        alarmDao.clearSnooze(alarmId)
    }

    suspend fun getPetSync(): Pet = withContext(Dispatchers.IO) {
        petDao.getPetSync()?.let { Pet.fromEntity(it) } ?: run {
            val newPet = Pet()
            petDao.insertOrUpdatePet(newPet.toEntity())
            newPet
        }
    }

    suspend fun savePet(pet: Pet) = withContext(Dispatchers.IO) {
        petDao.insertOrUpdatePet(pet.toEntity())
    }

    suspend fun updatePetMood(mood: Pet.PetMood) = withContext(Dispatchers.IO) {
        petDao.updateMood(mood.name.lowercase())
    }

    suspend fun addCountdown(countdown: Countdown): Long = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val entity = countdown.copy(createdAt = now).toEntity()
        countdownDao.insertCountdown(entity)
    }

    suspend fun updateCountdown(countdown: Countdown) = withContext(Dispatchers.IO) {
        countdownDao.updateCountdown(countdown.toEntity())
    }

    suspend fun deleteCountdown(countdownId: Long) = withContext(Dispatchers.IO) {
        countdownDao.deleteCountdownById(countdownId)
    }

    suspend fun pauseCountdown(countdownId: Long) = withContext(Dispatchers.IO) {
        countdownDao.setCountdownStatus(countdownId, "paused")
    }

    suspend fun resumeCountdown(countdownId: Long) = withContext(Dispatchers.IO) {
        countdownDao.setCountdownStatus(countdownId, "running")
    }

    fun getConfig(key: String, default: Any): Any {
        return when (default) {
            is Int -> prefs.getInt(key, default)
            is Long -> prefs.getLong(key, default)
            is Float -> prefs.getFloat(key, default)
            is Boolean -> prefs.getBoolean(key, default)
            is String -> prefs.getString(key, default) ?: default
            else -> default
        }
    }

    fun setConfig(key: String, value: Any) {
        prefs.edit().apply {
            when (value) {
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
            }
            apply()
        }
    }

    fun getAllConfig(): Map<String, *> = prefs.all

    fun exportData(): Map<String, Any> {
        return mapOf(
            "export_time" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }
}
