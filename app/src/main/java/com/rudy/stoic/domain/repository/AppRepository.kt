package com.rudy.stoic.domain.repository

import com.rudy.stoic.domain.model.InstalledApp
import kotlinx.coroutines.flow.StateFlow

interface AppRepository {
    val installedApps: StateFlow<List<InstalledApp>>
    fun refreshApps()
}
