package com.kieronquinn.app.smartspacer.plugins.battery.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.util.SizeF
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.plugins.battery.BatteryPlugin.Companion.PACKAGE_NAME
import com.kieronquinn.app.smartspacer.plugins.battery.BuildConfig
import com.kieronquinn.app.smartspacer.plugins.battery.model.BatteryLevels
import com.kieronquinn.app.smartspacer.plugins.battery.repositories.BatteryRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.findViewByIdentifier
import com.kieronquinn.app.smartspacer.sdk.utils.getColumnSpan
import com.kieronquinn.app.smartspacer.sdk.utils.getResourceForIdentifier
import com.kieronquinn.app.smartspacer.sdk.utils.getRowSpan
import org.koin.android.ext.android.inject

class BatteryWidget: SmartspacerWidgetProvider() {

    companion object {
        val AUTHORITY = "${BuildConfig.APPLICATION_ID}.widgets.battery"

        private const val IDENTIFIER_LIST_CONTAINER = "$PACKAGE_NAME:id/list_item_wrapper"
        private const val IDENTIFIER_DEVICE_ICON = "$PACKAGE_NAME:id/device_icon"
        private const val IDENTIFIER_DEVICE_NAME = "$PACKAGE_NAME:id/device_name"
        private const val IDENTIFIER_BATTERY_LEVEL = "$PACKAGE_NAME:id/battery_level"
        private const val IDENTIFIER_STATUS_ICON = "$PACKAGE_NAME:id/status_icon"
        private const val IDENTIFIER_OTHER_DEVICES = "$PACKAGE_NAME:string/other_devices"

        private val COMPONENT_WIDGET = ComponentName(
            PACKAGE_NAME,
            "com.google.android.settings.intelligence.modules.batterywidget.impl.BatteryAppWidgetProvider"
        )

        fun getProvider(context: Context): AppWidgetProviderInfo? {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            return appWidgetManager.installedProviders.firstOrNull {
                it.provider == COMPONENT_WIDGET
            }
        }
    }

    private val width by lazy {
        getColumnSpan(5)
    }

    private val height by lazy {
        getRowSpan(5)
    }

    private val batteryRepository by inject<BatteryRepository>()

    @Synchronized
    override fun onWidgetChanged(smartspacerId: String, remoteViews: RemoteViews?) {
        if(remoteViews == null) return
        val size = SizeF(width.toFloat(), height.toFloat())
        val sized = getSizedRemoteView(remoteViews, size)
        val views = sized?.load() ?: return
        val listContainer = views.findViewByIdentifier<LinearLayout>(IDENTIFIER_LIST_CONTAINER)
            ?: return
        val otherDevices = listContainer.context.getOtherDevicesString()
        val batteryLevels = listContainer.children.drop(1).mapNotNull {
            it.loadBatteryLevel(otherDevices)
        }.toList()
        batteryRepository.setBatteryLevels(BatteryLevels(batteryLevels))
    }

    private fun View.loadBatteryLevel(otherDevices: String?): BatteryLevels.BatteryLevel? {
        val name = findViewByIdentifier<TextView>(IDENTIFIER_DEVICE_NAME)?.text?.toString()
            ?: return null
        if(name == otherDevices) return null
        val batteryLevel = findViewByIdentifier<TextView>(IDENTIFIER_BATTERY_LEVEL)
            ?.text?.toString() ?: return null
        val icon = findViewByIdentifier<ImageView>(IDENTIFIER_DEVICE_ICON)?.drawable?.toBitmap()
            ?: return null
        val isCharging = findViewByIdentifier<ImageView>(IDENTIFIER_STATUS_ICON)?.isVisible
            ?: false
        return BatteryLevels.BatteryLevel(name, batteryLevel, isCharging, icon)
    }

    override fun getAppWidgetProviderInfo(smartspacerId: String): AppWidgetProviderInfo? {
        return getProvider(provideContext())
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(
            width = width,
            height = height
        )
    }

    private fun Context.getOtherDevicesString(): String? {
        return getResourceForIdentifier(IDENTIFIER_OTHER_DEVICES)?.let {
            getString(it)
        }
    }

}