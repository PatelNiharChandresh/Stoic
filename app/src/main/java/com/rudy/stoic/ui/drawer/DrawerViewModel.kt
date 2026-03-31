package com.rudy.stoic.ui.drawer

import android.content.Context
import androidx.lifecycle.ViewModel
import com.rudy.stoic.domain.model.InstalledApp
import com.rudy.stoic.domain.repository.AppRepository
import com.rudy.stoic.util.SystemActionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    val installedApps: StateFlow<List<InstalledApp>> = appRepository.installedApps

    fun launchApp(app: InstalledApp, context: Context) {
        SystemActionHelper.launchApp(context, app.packageName)
    }

    fun refreshApps() {
        appRepository.refreshApps()
    }
}
