package com.rudy.stoic.domain.model

enum class RingItemType {
    APP,
    FOLDER,
    SYSTEM_ACTION
}

data class RingItem(
    val id: String,
    val label: String,
    val type: RingItemType,
    val packageName: String? = null,
    val actionId: String? = null,
    val appList: List<String>? = null,
    val iconResId: Int? = null,
    val position: Int
)

object SystemActions {
    const val SEARCH = "system_search"
    const val SETTINGS = "system_settings"
    const val CALLS = "system_calls"
    const val MESSAGES = "system_messages"
}

fun defaultRingItems(): List<RingItem> = listOf(
    RingItem(
        id = "default_search",
        label = "Search",
        type = RingItemType.SYSTEM_ACTION,
        actionId = SystemActions.SEARCH,
        iconResId = android.R.drawable.ic_menu_search,
        position = 0
    ),
    RingItem(
        id = "default_settings",
        label = "Settings",
        type = RingItemType.SYSTEM_ACTION,
        actionId = SystemActions.SETTINGS,
        iconResId = android.R.drawable.ic_menu_manage,
        position = 1
    ),
    RingItem(
        id = "default_work",
        label = "Work",
        type = RingItemType.FOLDER,
        appList = emptyList(),
        iconResId = android.R.drawable.ic_menu_agenda,
        position = 2
    ),
    RingItem(
        id = "default_favourites",
        label = "Favourites",
        type = RingItemType.FOLDER,
        appList = emptyList(),
        iconResId = android.R.drawable.ic_menu_preferences,
        position = 3
    ),
    RingItem(
        id = "default_calls",
        label = "Calls",
        type = RingItemType.SYSTEM_ACTION,
        actionId = SystemActions.CALLS,
        iconResId = android.R.drawable.ic_menu_call,
        position = 4
    ),
    RingItem(
        id = "default_messages",
        label = "Messages",
        type = RingItemType.SYSTEM_ACTION,
        actionId = SystemActions.MESSAGES,
        iconResId = android.R.drawable.ic_dialog_email,
        position = 5
    )
)
