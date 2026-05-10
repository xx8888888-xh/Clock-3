package com.clock3.pet.service

import android.content.Context
import com.clock3.pet.data.model.Pet
import com.clock3.pet.data.repository.Clock3Repository
import kotlinx.coroutines.*
import java.time.LocalDateTime

class PetService(context: Context) {
    private val repository = Clock3Repository(context)
    private var pet: Pet? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var statusCallbacks = mutableListOf<(Pet) -> Unit>()

    init {
        scope.launch {
            pet = repository.getPetSync()
        }
    }

    suspend fun loadPet(): Pet {
        if (pet == null) {
            pet = repository.getPetSync()
        }
        return pet!!
    }

    suspend fun savePet() {
        pet?.let {
            repository.savePet(it)
        }
    }

    suspend fun interact(): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to "宠物未加载")
        val levelUpMessages = currentPet.interact()
        currentPet.lastInteraction = LocalDateTime.now()
        currentPet.totalInteractions++
        pet = currentPet.copy(mood = Pet.PetMood.random())

        savePet()
        notifyStatusChange()

        return mapOf(
            "message" to currentPet.getRandomMessage(),
            "mood" to currentPet.mood.name,
            "emoji" to currentPet.mood.emoji,
            "level" to currentPet.level,
            "exp" to currentPet.exp,
            "exp_needed" to currentPet.getExpForNextLevel(),
            "level_up" to levelUpMessages
        )
    }

    suspend fun feedPet(): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to "宠物未加载")
        currentPet.copy(mood = Pet.PetMood.HAPPY)
        val levelUpMessages = currentPet.addExp(10)
        pet = currentPet

        savePet()
        notifyStatusChange()

        return mapOf(
            "message" to "好吃! 谢谢喂我~",
            "level_up" to levelUpMessages
        )
    }

    suspend fun playWithPet(): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to "宠物未加载")
        pet = currentPet.copy(mood = Pet.PetMood.EXCITED)
        val levelUpMessages = currentPet.addExp(15)
        pet = currentPet

        savePet()
        notifyStatusChange()

        return mapOf(
            "message" to "太好玩了! 再来再来!",
            "level_up" to levelUpMessages
        )
    }

    suspend fun sleep(): Map<String, Any> {
        val currentPet = pet ?: return mapOf("message" to "宠物未加载")
        pet = currentPet.copy(mood = Pet.PetMood.SLEEPY)
        savePet()
        notifyStatusChange()

        return mapOf("message" to "困了... zzZ...")
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

    private fun notifyStatusChange() {
        pet?.let { currentPet ->
            scope.launch {
                statusCallbacks.forEach { callback ->
                    try {
                        callback(currentPet)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PetService? = null

        fun getInstance(context: Context): PetService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PetService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
