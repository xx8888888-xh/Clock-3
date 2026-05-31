package com.clock3.pet.model

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val type: ItemType
)

enum class ItemType {
    THEME,
    BREAK_TIME,
    REWARD
}
