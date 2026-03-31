package com.rudy.stoic.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rudy.stoic.domain.model.RingItem
import com.rudy.stoic.domain.model.RingItemType
import com.rudy.stoic.domain.model.defaultRingItems
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ringConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ring_config")

@Singleton
class RingConfigDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_RING_ITEMS = stringPreferencesKey("ring_items_json")
    }

    val ringItems: Flow<List<RingItem>> = context.ringConfigDataStore.data.map { prefs ->
        val json = prefs[KEY_RING_ITEMS]
        if (json != null) {
            deserializeItems(json)
        } else {
            defaultRingItems()
        }
    }

    suspend fun saveRingItems(items: List<RingItem>) {
        context.ringConfigDataStore.edit { prefs ->
            prefs[KEY_RING_ITEMS] = serializeItems(items)
        }
    }

    private fun serializeItems(items: List<RingItem>): String {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("type", item.type.name)
                put("packageName", item.packageName ?: "")
                put("actionId", item.actionId ?: "")
                put("iconResId", item.iconResId ?: 0)
                put("position", item.position)
                val appListArray = JSONArray()
                item.appList?.forEach { appListArray.put(it) }
                put("appList", appListArray)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun deserializeItems(json: String): List<RingItem> {
        return try {
            val jsonArray = JSONArray(json)
            val items = mutableListOf<RingItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val appListArray = obj.optJSONArray("appList")
                val appList = if (appListArray != null) {
                    (0 until appListArray.length()).map { appListArray.getString(it) }
                } else {
                    emptyList()
                }
                items.add(
                    RingItem(
                        id = obj.getString("id"),
                        label = obj.getString("label"),
                        type = RingItemType.valueOf(obj.getString("type")),
                        packageName = obj.optString("packageName").ifEmpty { null },
                        actionId = obj.optString("actionId").ifEmpty { null },
                        iconResId = obj.optInt("iconResId", 0).let { if (it == 0) null else it },
                        position = obj.getInt("position"),
                        appList = appList
                    )
                )
            }
            items.sortedBy { it.position }
        } catch (_: Exception) {
            defaultRingItems()
        }
    }
}
