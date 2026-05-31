package com.clock3.pet.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.clock3.pet.R
import com.clock3.pet.data.model.Pet
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.utils.ExpCalculator
import com.clock3.pet.utils.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

class PetService private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val repository = Clock3Repository(appContext)
    @Volatile
    private var pet: Pet? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var isInitialized = false
    private val initMutex = Mutex()
    private var initDeferred = CompletableDeferred<Unit>()

    private val statusCallbacks = CopyOnWriteArrayList<(Pet) -> Unit>()

    init {
        scope.launch {
            try {
                pet = repository.getPetSync()
                isInitialized = true
                initDeferred.complete(Unit)
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to initialize PetService", e)
                isInitialized = true
                initDeferred.complete(Unit)
            }
        }
    }

    suspend fun loadPet(): Pet {
        if (!isInitialized) {
            initDeferred.await()
        }
        if (pet == null) {
            initMutex.withLock {
                if (pet == null) {
                    pet = repository.getPetSync()
                }
            }
        }
        if (pet == null) {
            val defaultPet = Pet()
            this.pet = defaultPet
            return defaultPet
        }
        return pet ?: Pet()
    }

    suspend fun savePet() {
        pet?.let {
            repository.savePet(it)
        }
    }

    suspend fun interact(): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to appContext.getString(R.string.pet_not_loaded))
        val (updatedPet, levelUpMessages) = currentPet.interact()
        pet = updatedPet

        savePet()
        notifyStatusChange()

        return mapOf(
            "message" to updatedPet.getRandomMessage(),
            "mood" to updatedPet.mood.name,
            "emoji" to updatedPet.mood.emoji,
            "level" to updatedPet.level,
            "exp" to updatedPet.exp,
            "exp_needed" to updatedPet.getExpForNextLevel(),
            "level_up" to levelUpMessages
        )
    }

    suspend fun feedPet(): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to appContext.getString(R.string.pet_not_loaded))
        val happyPet = currentPet.copy(mood = Pet.PetMood.HAPPY)
        val (updatedPet, levelUpMessages) = happyPet.addExp(ExpCalculator.calculateFeedExp())
        pet = updatedPet

        savePet()
        notifyStatusChange()

        return mapOf(
            "message" to appContext.getString(R.string.pet_feed_msg),
            "level_up" to levelUpMessages
        )
    }

    suspend fun playWithPet(): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to appContext.getString(R.string.pet_not_loaded))
        val excitedPet = currentPet.copy(mood = Pet.PetMood.EXCITED)
        val (updatedPet, levelUpMessages) = excitedPet.addExp(ExpCalculator.calculatePlayExp())
        pet = updatedPet

        savePet()
        notifyStatusChange()

        return mapOf(
            "message" to appContext.getString(R.string.pet_play_msg),
            "level_up" to levelUpMessages
        )
    }

    suspend fun sleep(): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to appContext.getString(R.string.pet_not_loaded))
        pet = currentPet.copy(mood = Pet.PetMood.SLEEPY)
        savePet()
        notifyStatusChange()

        return mapOf("message" to appContext.getString(R.string.pet_sleep_msg))
    }

    suspend fun addExp(amount: Int): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to appContext.getString(R.string.pet_not_loaded))
        val safeAmount = amount.coerceAtLeast(0)
        val (updatedPet, levelUpMessages) = currentPet.addExp(safeAmount)
        val rows = repository.atomicUpdatePetExp(updatedPet.level, updatedPet.exp, currentPet.level, currentPet.exp)
        if (rows > 0) {
            pet = updatedPet
        } else {
            val freshPet = repository.getPetSync()
            val (retriedPet, retriedMessages) = freshPet.addExp(safeAmount)
            pet = retriedPet
            repository.savePet(retriedPet)
            return mapOf(
                "exp_added" to safeAmount,
                "level_up" to retriedMessages,
                "level" to retriedPet.level,
                "exp" to retriedPet.exp
            )
        }
        notifyStatusChange()

        return mapOf(
            "exp_added" to safeAmount,
            "level_up" to levelUpMessages,
            "level" to updatedPet.level,
            "exp" to updatedPet.exp
        )
    }

    fun isSleeping(): Boolean {
        return pet?.isSleeping() ?: false
    }

    fun getStatus(): Map<String, Any> {
        val currentPet = pet ?: return emptyMap()
        return mapOf(
            "name" to currentPet.name,
            "mood" to currentPet.mood.name,
            "emoji" to currentPet.mood.emoji,
            "level" to currentPet.level,
            "exp" to currentPet.exp,
            "exp_needed" to currentPet.getExpForNextLevel(),
            "is_sleeping" to currentPet.isSleeping(),
            "total_interactions" to currentPet.totalInteractions
        )
    }

    suspend fun updateName(name: String) {
        pet?.let {
            pet = it.copy(name = name)
            savePet()
            notifyStatusChange()
        }
    }

    fun onStatusChange(callback: (Pet) -> Unit) {
        statusCallbacks.add(callback)
    }

    fun removeStatusCallback(callback: (Pet) -> Unit) {
        statusCallbacks.remove(callback)
    }

    private fun notifyStatusChange() {
        pet?.let { currentPet ->
            mainHandler.post {
                statusCallbacks.forEach { callback ->
                    try {
                        callback(currentPet)
                    } catch (e: Exception) {
                        AppLog.e(TAG, "Status callback error", e)
                    }
                }
            }
        }
    }

    fun release() {
        scope.cancel()
        initDeferred.cancel()
        statusCallbacks.clear()
        pet = null
        isInitialized = false
    }

    fun reset() {
        scope.cancel()
        val newDeferred = CompletableDeferred<Unit>()
        initDeferred = newDeferred
        isInitialized = false
        pet = null
        val newScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        newScope.launch {
            try {
                pet = repository.getPetSync()
                isInitialized = true
                newDeferred.complete(Unit)
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to re-initialize PetService", e)
                isInitialized = true
                newDeferred.complete(Unit)
            }
        }
    }

    companion object {
        private const val TAG = "PetService"

        @Volatile
        private var INSTANCE: PetService? = null

        fun getInstance(context: Context): PetService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PetService(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun releaseInstance() {
            INSTANCE?.release()
            INSTANCE = null
        }
    }
}
