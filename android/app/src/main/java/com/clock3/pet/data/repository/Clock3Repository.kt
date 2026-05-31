package com.clock3.pet.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import com.clock3.pet.data.Clock3Database
import com.clock3.pet.data.model.Alarm
import com.clock3.pet.data.model.Countdown
import com.clock3.pet.data.model.Pet
import com.clock3.pet.utils.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Clock3Repository(context: Context) {
    private val appContext = context.applicationContext
    private val database = Clock3Database.getDatabase(appContext)
    private val alarmDao = database.alarmDao()
    private val countdownDao = database.countdownDao()
    private val petDao = database.petDao()
    private val prefs: SharedPreferences = appContext.getSharedPreferences("clock3_prefs", Context.MODE_PRIVATE)

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

    suspend fun getAllCountdownsSync(): List<Countdown> = withContext(Dispatchers.IO) {
        countdownDao.getAllCountdownsSync().map { Countdown.fromEntity(it) }
    }

    suspend fun getCountdownById(id: Long): Countdown? = withContext(Dispatchers.IO) {
        countdownDao.getCountdownById(id)?.let { Countdown.fromEntity(it) }
    }

    suspend fun getCountdownsByStatus(status: String): List<Countdown> = withContext(Dispatchers.IO) {
        countdownDao.getCountdownsByStatus(status).map { Countdown.fromEntity(it) }
    }

    suspend fun getEnabledAlarms(): List<Alarm> = withContext(Dispatchers.IO) {
        alarmDao.getEnabledAlarms().map { Alarm.fromEntity(it) }
    }

    suspend fun getAlarmById(id: Long): Alarm? = withContext(Dispatchers.IO) {
        alarmDao.getAlarmById(id)?.let { Alarm.fromEntity(it) }
    }

    suspend fun addAlarm(alarm: Alarm): Long = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().format(ISO_FORMATTER)
        val entity = alarm.copy(createdAt = now, updatedAt = now).toEntity()
        alarmDao.insertAlarm(entity)
    }

    suspend fun updateAlarm(alarm: Alarm) = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().format(ISO_FORMATTER)
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
            .format(ISO_FORMATTER)
        alarmDao.setSnoozeTimeAndIncrementCount(alarmId, snoozeTime)
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

    suspend fun atomicUpdatePetExp(newLevel: Int, newExp: Int, expectedLevel: Int, expectedExp: Int): Int = withContext(Dispatchers.IO) {
        petDao.atomicUpdateExp(newLevel, newExp, expectedLevel, expectedExp)
    }

    suspend fun updatePetMood(mood: Pet.PetMood) = withContext(Dispatchers.IO) {
        petDao.updateMood(mood.name.lowercase())
    }

    suspend fun addCountdown(countdown: Countdown): Long = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().format(ISO_FORMATTER)
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
            else -> {
                AppLog.w(TAG, "Unsupported config type: ${default.javaClass.simpleName} for key: $key")
                default
            }
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
                else -> {
                    AppLog.w(TAG, "Unsupported config type: ${value.javaClass.simpleName} for key: $key")
                }
            }
            apply()
        }
    }

    fun getAllConfig(): Map<String, *> = prefs.all

    suspend fun exportData(): Map<String, Any?> = withContext(Dispatchers.IO) {
        val pet = petDao.getPetSync()?.let { Pet.fromEntity(it) } ?: Pet()
        val alarms = alarmDao.getAllAlarmsSync().map { Alarm.fromEntity(it) }
        val countdowns = countdownDao.getAllCountdownsSync().map { Countdown.fromEntity(it) }
        
        return@withContext mapOf(
            "export_time" to LocalDateTime.now().format(ISO_FORMATTER),
            "version" to "1.0",
            "prefs" to prefs.all,
            "pet" to pet.toExportMap(),
            "alarms" to alarms.map { it.toExportMap() },
            "countdowns" to countdowns.map { it.toExportMap() }
        )
    }

    suspend fun importData(data: Map<String, Any?>): Boolean = withContext(Dispatchers.IO) {
        try {
            val version = data["version"] as? String
            if (version != "1.0") {
                return@withContext false
            }

            database.withTransaction {
                (data["pet"] as? Map<*, *>)?.let { petMap ->
                    val pet = Pet.fromExportMap(petMap)
                    petDao.insertOrUpdatePet(pet.toEntity())
                }

                (data["alarms"] as? List<*>)?.let { alarmList ->
                    val alarms = alarmList.mapNotNull { alarmMap ->
                        (alarmMap as? Map<*, *>)?.let {
                            Alarm.fromExportMap(it).toEntity()
                        }
                    }
                    alarmDao.importAlarms(alarms)
                }

                (data["countdowns"] as? List<*>)?.let { countdownList ->
                    val countdowns = countdownList.mapNotNull { countdownMap ->
                        (countdownMap as? Map<*, *>)?.let {
                            Countdown.fromExportMap(it).toEntity()
                        }
                    }
                    countdownDao.importCountdowns(countdowns)
                }
            }

            (data["prefs"] as? Map<*, *>)?.let { prefsMap ->
                prefs.edit().clear().apply {
                    prefsMap.forEach { (key, value) ->
                        when (value) {
                            is Int -> putInt(key.toString(), value)
                            is String -> putString(key.toString(), value)
                            is Boolean -> putBoolean(key.toString(), value)
                            is Long -> putLong(key.toString(), value)
                            is Float -> putFloat(key.toString(), value)
                            is Double -> {
                                if (value == value.toLong().toDouble()) {
                                    putLong(key.toString(), value.toLong())
                                } else {
                                    putFloat(key.toString(), value.toFloat())
                                }
                            }
                            else -> {
                                AppLog.w(TAG, "Unsupported prefs type on import: ${value?.javaClass?.simpleName} for key: $key")
                            }
                        }
                    }
                }.apply()
            }

            return@withContext true
        } catch (e: Exception) {
            AppLog.e(TAG, "Import data failed", e)
            return@withContext false
        }
    }

    fun getTodayPomodoroCount(): Int {
        val today = LocalDateTime.now().format(DATE_FORMATTER)
        return prefs.getInt("pomodoro_count_$today", 0)
    }

    fun getTodayExp(): Int {
        val today = LocalDateTime.now().format(DATE_FORMATTER)
        return prefs.getInt("pomodoro_exp_$today", 0)
    }

    fun savePomodoroRecord(completedPomodoros: Int, totalExp: Int) {
        val today = LocalDateTime.now().format(DATE_FORMATTER)
        prefs.edit()
            .putInt("pomodoro_count_$today", completedPomodoros)
            .putInt("pomodoro_exp_$today", totalExp)
            .apply()
    }

    companion object {
        private const val TAG = "Clock3Repository"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }
}
