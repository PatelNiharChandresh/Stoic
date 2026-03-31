package com.rudy.stoic.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudy.stoic.data.local.datastore.RingConfigDataStore
import com.rudy.stoic.domain.model.InstalledApp
import com.rudy.stoic.domain.model.RingItem
import com.rudy.stoic.domain.model.RingItemType
import com.rudy.stoic.domain.model.SystemActions
import com.rudy.stoic.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RingConfigViewModel @Inject constructor(
    private val ringConfigDataStore: RingConfigDataStore,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _ringItems = MutableStateFlow<List<RingItem>>(emptyList())
    val ringItems: StateFlow<List<RingItem>> = _ringItems.asStateFlow()

    val installedApps: StateFlow<List<InstalledApp>> = appRepository.installedApps

    companion object {
        const val MAX_ITEMS = 8
        const val MIN_ITEMS = 1
    }

    init {
        viewModelScope.launch {
            ringConfigDataStore.ringItems.collect { items ->
                _ringItems.value = items
            }
        }
    }

    fun addAppItem(app: InstalledApp) {
        val current = _ringItems.value
        if (current.size >= MAX_ITEMS) return

        val newItem = RingItem(
            id = UUID.randomUUID().toString(),
            label = app.name,
            type = RingItemType.APP,
            packageName = app.packageName,
            position = current.size
        )
        saveItems(current + newItem)
    }

    fun addSystemAction(actionId: String, label: String) {
        val current = _ringItems.value
        if (current.size >= MAX_ITEMS) return

        // Don't add duplicate system actions
        if (current.any { it.actionId == actionId }) return

        val iconRes = when (actionId) {
            SystemActions.SEARCH -> android.R.drawable.ic_menu_search
            SystemActions.SETTINGS -> android.R.drawable.ic_menu_manage
            SystemActions.CALLS -> android.R.drawable.ic_menu_call
            SystemActions.MESSAGES -> android.R.drawable.ic_dialog_email
            else -> android.R.drawable.ic_menu_help
        }

        val newItem = RingItem(
            id = UUID.randomUUID().toString(),
            label = label,
            type = RingItemType.SYSTEM_ACTION,
            actionId = actionId,
            iconResId = iconRes,
            position = current.size
        )
        saveItems(current + newItem)
    }

    fun addFolder(name: String) {
        val current = _ringItems.value
        if (current.size >= MAX_ITEMS) return

        val newItem = RingItem(
            id = UUID.randomUUID().toString(),
            label = name,
            type = RingItemType.FOLDER,
            appList = emptyList(),
            iconResId = android.R.drawable.ic_menu_agenda,
            position = current.size
        )
        saveItems(current + newItem)
    }

    fun removeItem(item: RingItem) {
        val current = _ringItems.value
        if (current.size <= MIN_ITEMS) return

        val updated = current.filter { it.id != item.id }
            .mapIndexed { index, ringItem -> ringItem.copy(position = index) }
        saveItems(updated)
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = _ringItems.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        val updated = current.mapIndexed { index, ringItem -> ringItem.copy(position = index) }
        saveItems(updated)
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        val current = _ringItems.value
        val updated = current.map { item ->
            if (item.id == folderId && item.type == RingItemType.FOLDER) {
                val apps = item.appList.orEmpty().toMutableList()
                if (packageName !in apps) {
                    apps.add(packageName)
                }
                item.copy(appList = apps)
            } else {
                item
            }
        }
        saveItems(updated)
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        val current = _ringItems.value
        val updated = current.map { item ->
            if (item.id == folderId && item.type == RingItemType.FOLDER) {
                item.copy(appList = item.appList.orEmpty().filter { it != packageName })
            } else {
                item
            }
        }
        saveItems(updated)
    }

    private fun saveItems(items: List<RingItem>) {
        _ringItems.value = items
        viewModelScope.launch {
            ringConfigDataStore.saveRingItems(items)
        }
    }
}
