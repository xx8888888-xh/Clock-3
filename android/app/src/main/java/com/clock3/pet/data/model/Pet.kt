package com.clock3.pet.data.model

import com.clock3.pet.data.entity.PetEntity
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
    enum class PetMood(val emoji: String, val colorHex: String) {
        HAPPY("😊", "#FFD54F"),
        SAD("😢", "#90CAF9"),
        SLEEPY("😴", "#CE93D8"),
        EXCITED("🤩", "#FF8A65"),
        HUNGRY("🤤", "#A5D6A7"),
        BORED("😐", "#B0BEC5");

        companion object {
            fun random(): PetMood {
                return entries.filter { it != SLEEPY }.random()
            }
        }
    }

    fun getExpForNextLevel(): Int {
        return level * 100
    }

    fun addExp(amount: Int): Pair<Pet, List<String>> {
        var newExp = exp + amount
        var newLevel = level
        val messages = mutableListOf<String>()

        while (newExp >= newLevel * 100) {
            newExp -= newLevel * 100
            newLevel++
            messages.add("🎉 恭喜升级到 $newLevel 级!")
        }

        return Pair(this.copy(exp = newExp, level = newLevel), messages)
    }

    fun interact(): Pair<Pet, List<String>> {
        val newExp = 5 + (level * 2)
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
        return hour >= 22 || hour < 7
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

    companion object {
        fun fromEntity(entity: PetEntity): Pet {
            return Pet(
                id = entity.id,
                mood = PetMood.entries.find { it.name.equals(entity.mood, ignoreCase = true) } ?: PetMood.HAPPY,
                level = entity.level,
                exp = entity.exp,
                name = entity.name,
                lastInteraction = entity.lastInteraction?.let { LocalDateTime.parse(it) },
                totalInteractions = entity.totalInteractions
            )
        }
    }
}
