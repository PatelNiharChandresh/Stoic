package com.rudy.stoic.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import com.rudy.stoic.domain.model.GestureDirection
import com.rudy.stoic.domain.model.RingItem
import com.rudy.stoic.domain.model.RingItemType
import com.rudy.stoic.domain.model.SystemActions
import com.rudy.stoic.domain.model.defaultRingItems
import com.rudy.stoic.util.SystemActionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class PanelState {
    HOME,
    DRAWER_OPEN,
    QUICK_SETTINGS_OPEN
}

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _panelState = MutableStateFlow(PanelState.HOME)
    val panelState: StateFlow<PanelState> = _panelState.asStateFlow()

    private val _ringItems = MutableStateFlow(defaultRingItems())
    val ringItems: StateFlow<List<RingItem>> = _ringItems.asStateFlow()

    fun onRingItemTapped(item: RingItem, context: Context) {
        when (item.type) {
            RingItemType.APP -> {
                item.packageName?.let { SystemActionHelper.launchApp(context, it) }
            }
            RingItemType.FOLDER -> {
                // Deferred to Step 6 — folder expansion
            }
            RingItemType.SYSTEM_ACTION -> {
                when (item.actionId) {
                    SystemActions.SEARCH -> {
                        // Deferred to Step 7 — search overlay
                    }
                    SystemActions.SETTINGS -> SystemActionHelper.openSettings(context)
                    SystemActions.CALLS -> SystemActionHelper.openDialer(context)
                    SystemActions.MESSAGES -> SystemActionHelper.openMessages(context)
                }
            }
        }
    }

    fun onCenterGesture(direction: GestureDirection, context: Context) {
        when (direction) {
            GestureDirection.DOWN -> {
                SystemActionHelper.expandNotificationPanel(context)
            }
            GestureDirection.LEFT -> {
                _panelState.value = PanelState.DRAWER_OPEN
            }
            GestureDirection.RIGHT -> {
                _panelState.value = PanelState.QUICK_SETTINGS_OPEN
            }
        }
    }

    fun dismissPanel() {
        _panelState.value = PanelState.HOME
    }
}
