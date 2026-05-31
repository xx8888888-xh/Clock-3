package com.clock3.pet.data

import android.content.Context
import android.content.SharedPreferences
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.model.ItemType
import com.clock3.pet.model.ShopItem
import com.clock3.pet.utils.ExpCalculator

class ShopRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("shop_prefs", Context.MODE_PRIVATE)
    private val clock3Repository = Clock3Repository(appContext)

    companion object {
        const val PREFS_CURRENT_THEME = "current_theme"
        const val THEME_DEFAULT = "default"
        const val THEME_CHERRY_BLOSSOM = "theme_cherry_blossom"
        const val THEME_OCEAN = "theme_ocean"
        const val THEME_FOREST = "theme_forest"
        const val THEME_NIGHT = "theme_night"
        private const val MAX_CAS_RETRIES = 3
    }

    val defaultItems by lazy {
        listOf(
            ShopItem(
                id = "theme_cherry_blossom",
                name = "樱花主题",
                description = "粉色樱花背景",
                cost = 50,
                type = ItemType.THEME
            ),
            ShopItem(
                id = "theme_ocean",
                name = "海洋主题",
                description = "蓝色海洋风格",
                cost = 50,
                type = ItemType.THEME
            ),
            ShopItem(
                id = "theme_forest",
                name = "森林主题",
                description = "绿色森林风格",
                cost = 80,
                type = ItemType.THEME
            ),
            ShopItem(
                id = "break_10min",
                name = "10分钟休息券",
                description = "使用后可获得10分钟额外休息时间",
                cost = 30,
                type = ItemType.BREAK_TIME
            ),
            ShopItem(
                id = "break_30min",
                name = "30分钟休息券",
                description = "使用后可获得30分钟额外休息时间",
                cost = 80,
                type = ItemType.BREAK_TIME
            ),
            ShopItem(
                id = "reward_custom",
                name = "自定义奖励",
                description = "解锁自定义奖励设置",
                cost = 100,
                type = ItemType.REWARD
            ),
            ShopItem(
                id = "theme_night",
                name = "夜间主题",
                description = "深色夜间模式",
                cost = 60,
                type = ItemType.THEME
            ),
            ShopItem(
                id = "break_60min",
                name = "60分钟休息券",
                description = "使用后可获得60分钟额外休息时间",
                cost = 150,
                type = ItemType.BREAK_TIME
            )
        )
    }

    fun getUnlockedItems(): List<String> {
        return prefs.getStringSet("unlocked_items", emptySet())?.toList() ?: emptyList()
    }

    fun unlockItem(itemId: String): Boolean {
        val unlocked = getUnlockedItems().toMutableSet()
        unlocked.add(itemId)
        prefs.edit().putStringSet("unlocked_items", unlocked).apply()
        return true
    }

    fun isItemUnlocked(itemId: String): Boolean {
        return getUnlockedItems().contains(itemId)
    }

    suspend fun getTotalExp(): Int {
        return try {
            val pet = clock3Repository.getPetSync()
            calculateTotalExp(pet.level, pet.exp)
        } catch (e: Exception) {
            com.clock3.pet.utils.AppLog.e("ShopRepository", "getTotalExp failed", e)
            0
        }
    }

    suspend fun spendExp(amount: Int): Boolean {
        if (amount < 0) return false
        var retries = 0
        while (retries < MAX_CAS_RETRIES) {
            retries++
            try {
                val pet = clock3Repository.getPetSync()
                val totalExp = calculateTotalExp(pet.level, pet.exp)
                if (totalExp < amount) return false

                var remainingExp = (totalExp - amount).coerceAtLeast(0)
                var newLevel = 1
                while (remainingExp >= newLevel * ExpCalculator.EXP_PER_LEVEL) {
                    remainingExp -= newLevel * ExpCalculator.EXP_PER_LEVEL
                    newLevel++
                }
                val rows = clock3Repository.atomicUpdatePetExp(newLevel, remainingExp, pet.level, pet.exp)
                if (rows > 0) return true
            } catch (e: Exception) {
                com.clock3.pet.utils.AppLog.e("ShopRepository", "spendExp failed", e)
                return false
            }
        }
        return false
    }

    private fun calculateTotalExp(level: Int, exp: Int): Int {
        return ExpCalculator.EXP_PER_LEVEL * level * (level - 1) / 2 + exp
    }

    fun getBreakTickets(): Int {
        return prefs.getInt("break_tickets", 0)
    }

    fun addBreakTicket(minutes: Int) {
        if (minutes <= 0) return
        val current = getBreakTickets()
        prefs.edit().putInt("break_tickets", current + minutes).apply()
    }

    fun useBreakTicket(minutes: Int): Boolean {
        if (minutes <= 0) return false
        val current = getBreakTickets()
        return if (current >= minutes) {
            prefs.edit().putInt("break_tickets", current - minutes).apply()
            true
        } else {
            false
        }
    }

    fun getCurrentTheme(): String {
        return prefs.getString(PREFS_CURRENT_THEME, THEME_DEFAULT) ?: THEME_DEFAULT
    }

    fun setCurrentTheme(themeId: String) {
        if (isItemUnlocked(themeId) || themeId == THEME_DEFAULT) {
            prefs.edit().putString(PREFS_CURRENT_THEME, themeId).apply()
        }
    }
}
