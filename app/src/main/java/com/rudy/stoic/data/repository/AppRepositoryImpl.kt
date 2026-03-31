package com.rudy.stoic.data.repository

import android.content.Context
import android.content.Intent
import com.rudy.stoic.domain.model.InstalledApp
import com.rudy.stoic.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    override val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    init {
        refreshApps()
    }

    override fun refreshApps() {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = pm.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != context.packageName } // Exclude self
            .map { resolveInfo ->
                InstalledApp(
                    name = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(pm)
                )
            }
            .sortedBy { it.name.lowercase() }
            .distinctBy { it.packageName }

        _installedApps.value = apps
    }
}
