package com.dtox.app.data

enum class ShopItemType { THEME, PATTERN }

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val type: ShopItemType,
    /** Theme id or pattern id, depending on [type]. */
    val value: String,
) {
    companion object {
        val ALL = listOf(
            ShopItem(
                id = "theme:midnight",
                name = "Midnight",
                description = "Deep-blue night palette",
                cost = 40,
                type = ShopItemType.THEME,
                value = "midnight",
            ),
            ShopItem(
                id = "theme:paper",
                name = "Paper",
                description = "Warm light theme",
                cost = 50,
                type = ShopItemType.THEME,
                value = "paper",
            ),
            ShopItem(
                id = "pattern:coherent",
                name = "Coherent 6·6",
                description = "Six slow breaths a minute",
                cost = 80,
                type = ShopItemType.PATTERN,
                value = "coherent",
            ),
            ShopItem(
                id = "pattern:extended",
                name = "Extended Exhale 4·8",
                description = "A long, calming exhale",
                cost = 80,
                type = ShopItemType.PATTERN,
                value = "extended",
            ),
        )

        fun byId(id: String): ShopItem? = ALL.firstOrNull { it.id == id }
    }
}
