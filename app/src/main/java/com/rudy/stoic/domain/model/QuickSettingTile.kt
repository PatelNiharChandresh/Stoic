package com.rudy.stoic.domain.model

data class QuickSettingTile(
    val id: String,
    val label: String,
    val iconResId: Int,
    val isEnabled: Boolean,
    val type: QuickSettingType = QuickSettingType.TOGGLE
)

enum class QuickSettingType {
    TOGGLE,
    VALUE // e.g., brightness
}
