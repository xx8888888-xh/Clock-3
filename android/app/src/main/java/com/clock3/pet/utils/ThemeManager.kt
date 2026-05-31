package com.clock3.pet.utils

import android.content.Context
import com.clock3.pet.R
import com.clock3.pet.data.ShopRepository

object ThemeManager {
    @Volatile
    private var shopRepository: ShopRepository? = null

    fun applyTheme(context: Context, themeId: String) {
        when (themeId) {
            ShopRepository.THEME_CHERRY_BLOSSOM -> context.setTheme(R.style.Theme_Clock3_CherryBlossom)
            ShopRepository.THEME_OCEAN -> context.setTheme(R.style.Theme_Clock3_Ocean)
            ShopRepository.THEME_FOREST -> context.setTheme(R.style.Theme_Clock3_Forest)
            ShopRepository.THEME_NIGHT -> context.setTheme(R.style.Theme_Clock3_Night)
            else -> context.setTheme(R.style.Theme_Clock3)
        }
    }

    fun getCurrentTheme(context: Context): String {
        val repo = shopRepository ?: synchronized(this) {
            shopRepository ?: ShopRepository(context.applicationContext).also { shopRepository = it }
        }
        return repo.getCurrentTheme()
    }
}
