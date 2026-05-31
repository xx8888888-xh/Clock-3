package com.clock3.pet.data.model

import com.clock3.pet.data.entity.PetEntity
import com.clock3.pet.utils.ExpCalculator
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

data class Pet(
    val id: Int = 1,
    val mood: PetMood = PetMood.HAPPY,
    val level: Int = 1,
    val exp: Int = 0,
    val name: String = "小宠物",
    val lastInteraction: LocalDateTime? = null,
    val totalInteractions: Int = 0
) {
    enum class PetMood(val emoji: String) {
        HAPPY("😊"),
        SAD("😢"),
        SLEEPY("😴"),
        EXCITED("🤩"),
        HUNGRY("🤤"),
        BORED("😐");

        companion object {
            private val RANDOMABLE_MOODS = entries.filter { it != SLEEPY }

            fun random(): PetMood {
                return RANDOMABLE_MOODS.random()
            }
        }
    }

    fun getExpForNextLevel(): Int {
        return level * ExpCalculator.EXP_PER_LEVEL
    }

    fun addExp(amount: Int): Pair<Pet, List<String>> {
        val safeAmount = amount.coerceAtLeast(0)
        var newExp = exp + safeAmount
        var newLevel = level
        val messages = mutableListOf<String>()

        while (newExp >= newLevel * ExpCalculator.EXP_PER_LEVEL) {
            newExp -= newLevel * ExpCalculator.EXP_PER_LEVEL
            newLevel++
            messages.add("🎉 恭喜升级到 $newLevel 级!")
        }

        return Pair(this.copy(exp = newExp, level = newLevel), messages)
    }

    fun interact(): Pair<Pet, List<String>> {
        val newExp = ExpCalculator.calculateInteractExp(level)
        val (updatedPet, levelUpMessages) = addExp(newExp)
        val newMood = PetMood.random()
        return Pair(
            updatedPet.copy(
                mood = newMood,
                lastInteraction = LocalDateTime.now(),
                totalInteractions = totalInteractions + 1
            ),
            levelUpMessages
        )
    }

    fun getRandomMessage(): String {
        val messages = when (mood) {
            PetMood.HAPPY -> listOf("今天心情真好!", "好开心呀~", "看到你真高兴!")
            PetMood.SAD -> listOf("有点难过...", "不理我了?", "想你了...")
            PetMood.SLEEPY -> listOf("好困啊...", "想睡觉了", "zzZ...")
            PetMood.EXCITED -> listOf("太棒了!", "耶耶耶!", "好兴奋!")
            PetMood.HUNGRY -> listOf("肚子饿了...", "想吃东西", "喂我吃饭吧~")
            PetMood.BORED -> listOf("好无聊啊", "陪我玩嘛", "没人理我...")
        }
        return messages.random()
    }

    fun isSleeping(): Boolean {
        val hour = LocalTime.now().hour
        return hour >= SLEEP_START_HOUR || hour < SLEEP_END_HOUR
    }

    fun toEntity(): PetEntity {
        return PetEntity(
            id = id,
            mood = mood.name.lowercase(),
            level = level,
            exp = exp,
            name = name,
            lastInteraction = lastInteraction?.toString(),
            totalInteractions = totalInteractions
        )
    }

    fun toExportMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "mood" to mood.name,
            "level" to level,
            "exp" to exp,
            "name" to name,
            "lastInteraction" to (lastInteraction?.toString() ?: ""),
            "totalInteractions" to totalInteractions
        )
    }

    companion object {
        private const val SLEEP_START_HOUR = 22
        private const val SLEEP_END_HOUR = 7

        fun fromEntity(entity: PetEntity): Pet {
            return Pet(
                id = entity.id,
                mood = PetMood.entries.find { it.name.equals(entity.mood, ignoreCase = true) } ?: PetMood.HAPPY,
                level = entity.level.coerceAtLeast(1),
                exp = entity.exp.coerceAtLeast(0),
                name = entity.name,
                lastInteraction = entity.lastInteraction?.let { LocalDateTime.parse(it) },
                totalInteractions = entity.totalInteractions.coerceAtLeast(0)
            )
        }

        fun fromExportMap(map: Map<*, *>): Pet {
            return Pet(
                id = 1,
                mood = (map["mood"] as? String)?.let {
                    try {
                        PetMood.valueOf(it.uppercase())
                    } catch (e: Exception) {
                        PetMood.HAPPY
                    }
                } ?: PetMood.HAPPY,
                level = ((map["level"] as? Number)?.toInt() ?: 1).coerceAtLeast(1),
                exp = ((map["exp"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
                name = (map["name"] as? String) ?: "小宠物",
                lastInteraction = (map["lastInteraction"] as? String)?.let {
                    try {
                        if (it.isNotEmpty()) LocalDateTime.parse(it) else null
                    } catch (e: Exception) {
                        null
                    }
                },
                totalInteractions = ((map["totalInteractions"] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
            )
        }
    }
}
