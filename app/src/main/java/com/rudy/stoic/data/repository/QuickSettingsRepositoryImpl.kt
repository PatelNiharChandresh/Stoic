package com.rudy.stoic.data.repository

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.rudy.stoic.domain.model.QuickSettingTile
import com.rudy.stoic.domain.model.QuickSettingType
import com.rudy.stoic.domain.repository.QuickSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : QuickSettingsRepository {

    companion object {
        const val TILE_WIFI = "wifi"
        const val TILE_BLUETOOTH = "bluetooth"
        const val TILE_AIRPLANE = "airplane"
        const val TILE_BRIGHTNESS = "brightness"
        const val TILE_AUTO_ROTATE = "auto_rotate"
        const val TILE_DND = "dnd"
        const val TILE_FLASHLIGHT = "flashlight"
        const val TILE_LOCATION = "location"
    }

    private val _tiles = MutableStateFlow<List<QuickSettingTile>>(emptyList())
    override val tiles: StateFlow<List<QuickSettingTile>> = _tiles.asStateFlow()

    private var flashlightOn = false

    init {
        refreshStates()
    }

    override fun refreshStates() {
        _tiles.value = listOf(
            QuickSettingTile(
                id = TILE_WIFI,
                label = "WiFi",
                iconResId = android.R.drawable.ic_menu_manage,
                isEnabled = isWifiEnabled()
            ),
            QuickSettingTile(
                id = TILE_BLUETOOTH,
                label = "Bluetooth",
                iconResId = android.R.drawable.stat_sys_data_bluetooth,
                isEnabled = isBluetoothEnabled()
            ),
            QuickSettingTile(
                id = TILE_AIRPLANE,
                label = "Airplane",
                iconResId = android.R.drawable.ic_menu_send,
                isEnabled = isAirplaneModeOn()
            ),
            QuickSettingTile(
                id = TILE_BRIGHTNESS,
                label = "Brightness",
                iconResId = android.R.drawable.ic_menu_view,
                isEnabled = getBrightness() > 128,
                type = QuickSettingType.VALUE
            ),
            QuickSettingTile(
                id = TILE_AUTO_ROTATE,
                label = "Auto-Rotate",
                iconResId = android.R.drawable.ic_menu_rotate,
                isEnabled = isAutoRotateOn()
            ),
            QuickSettingTile(
                id = TILE_DND,
                label = "DND",
                iconResId = android.R.drawable.ic_menu_close_clear_cancel,
                isEnabled = isDndOn()
            ),
            QuickSettingTile(
                id = TILE_FLASHLIGHT,
                label = "Flashlight",
                iconResId = android.R.drawable.ic_menu_compass,
                isEnabled = flashlightOn
            ),
            QuickSettingTile(
                id = TILE_LOCATION,
                label = "Location",
                iconResId = android.R.drawable.ic_menu_mylocation,
                isEnabled = isLocationEnabled()
            )
        )
    }

    override fun toggle(tileId: String): Boolean {
        return when (tileId) {
            TILE_WIFI -> toggleWifi()
            TILE_BLUETOOTH -> toggleBluetooth()
            TILE_AIRPLANE -> { openSystemSetting(Settings.ACTION_AIRPLANE_MODE_SETTINGS); true }
            TILE_BRIGHTNESS -> toggleBrightness()
            TILE_AUTO_ROTATE -> toggleAutoRotate()
            TILE_DND -> { openSystemSetting(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS); true }
            TILE_FLASHLIGHT -> toggleFlashlight()
            TILE_LOCATION -> { openSystemSetting(Settings.ACTION_LOCATION_SOURCE_SETTINGS); true }
            else -> false
        }.also { refreshStates() }
    }

    private fun isWifiEnabled(): Boolean {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.isWifiEnabled ?: false
        } catch (_: Exception) { false }
    }

    @Suppress("DEPRECATION")
    private fun toggleWifi(): Boolean {
        return try {
            // On Android 10+ we can't toggle WiFi programmatically, open settings panel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                openSystemSetting(Settings.Panel.ACTION_WIFI)
                true
            } else {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiManager?.let {
                    it.isWifiEnabled = !it.isWifiEnabled
                    true
                } ?: false
            }
        } catch (_: Exception) { false }
    }

    private fun isBluetoothEnabled(): Boolean {
        return try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.isEnabled ?: false
        } catch (_: Exception) { false }
    }

    private fun toggleBluetooth(): Boolean {
        openSystemSetting(Settings.ACTION_BLUETOOTH_SETTINGS)
        return true
    }

    private fun isAirplaneModeOn(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }

    private fun getBrightness(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 128
            )
        } catch (_: Exception) { 128 }
    }

    private fun toggleBrightness(): Boolean {
        return try {
            if (!Settings.System.canWrite(context)) {
                openSystemSetting(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                return false
            }
            val current = getBrightness()
            val newVal = if (current > 128) 64 else 255
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, newVal
            )
            true
        } catch (_: Exception) { false }
    }

    private fun isAutoRotateOn(): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION, 0
        ) == 1
    }

    private fun toggleAutoRotate(): Boolean {
        return try {
            if (!Settings.System.canWrite(context)) {
                openSystemSetting(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                return false
            }
            val current = isAutoRotateOn()
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, if (current) 0 else 1
            )
            true
        } catch (_: Exception) { false }
    }

    private fun isDndOn(): Boolean {
        return try {
            val zenMode = Settings.Global.getInt(
                context.contentResolver,
                "zen_mode", 0
            )
            zenMode != 0
        } catch (_: Exception) { false }
    }

    private fun toggleFlashlight(): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.let { cm ->
                val cameraId = cm.cameraIdList.firstOrNull() ?: return false
                flashlightOn = !flashlightOn
                cm.setTorchMode(cameraId, flashlightOn)
                true
            } ?: false
        } catch (_: Exception) {
            flashlightOn = false
            false
        }
    }

    private fun isLocationEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                lm?.isLocationEnabled ?: false
            } else {
                @Suppress("DEPRECATION")
                val mode = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
                )
                mode != Settings.Secure.LOCATION_MODE_OFF
            }
        } catch (_: Exception) { false }
    }

    private fun openSystemSetting(action: String) {
        try {
            val intent = android.content.Intent(action).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}
