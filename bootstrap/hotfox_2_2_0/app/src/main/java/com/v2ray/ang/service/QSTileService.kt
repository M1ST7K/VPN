package com.v2ray.ang.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.Utils
import com.v2ray.ang.vpn.HotfoxEngineFacade
import com.v2ray.ang.vpn.QsTileUiMapper
import java.lang.ref.SoftReference

class QSTileService : TileService() {

    fun setState(state: Int) {
        applySessionAppearance()
        if (qsTile == null) return
        if (state == Tile.STATE_INACTIVE &&
            QsTileUiMapper.from(HotfoxEngineFacade.currentState()) == QsTileUiMapper.Appearance.INACTIVE
        ) {
            qsTile.state = Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

    private fun applySessionAppearance() {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(applicationContext, R.drawable.ic_hotfox_notification)
        val appearance = QsTileUiMapper.from(HotfoxEngineFacade.currentState())
        val labelId = resources.getIdentifier(
            QsTileUiMapper.labelResName(appearance),
            "string",
            packageName,
        )
        tile.label = if (labelId != 0) getString(labelId) else getString(R.string.app_name)
        tile.state = when (appearance) {
            QsTileUiMapper.Appearance.ACTIVE -> Tile.STATE_ACTIVE
            QsTileUiMapper.Appearance.CONNECTING -> Tile.STATE_UNAVAILABLE
            QsTileUiMapper.Appearance.ERROR,
            QsTileUiMapper.Appearance.INACTIVE,
            -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        applySessionAppearance()
        mMsgReceive = ReceiveMessageHandler(this)
        val mFilter = IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY)
        ContextCompat.registerReceiver(applicationContext, mMsgReceive, mFilter, Utils.receiverFlags())
        MessageUtil.sendMsg2Service(this, AppConfig.MSG_REGISTER_CLIENT, "")
    }

    override fun onStopListening() {
        super.onStopListening()
        try {
            applicationContext.unregisterReceiver(mMsgReceive)
            mMsgReceive = null
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to unregister receiver", e)
        }
    }

    override fun onClick() {
        super.onClick()
        val session = HotfoxEngineFacade.currentState()
        when {
            QsTileUiMapper.shouldStopOnClick(session) -> CoreServiceManager.stopVService(this)
            QsTileUiMapper.shouldStartOnClick(session) -> CoreServiceManager.startVServiceFromToggle(this)
            else -> applySessionAppearance()
        }
    }

    private var mMsgReceive: BroadcastReceiver? = null

    private class ReceiveMessageHandler(context: QSTileService) : BroadcastReceiver() {
        var mReference: SoftReference<QSTileService> = SoftReference(context)
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val context = mReference.get() ?: return
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING,
                AppConfig.MSG_STATE_NOT_RUNNING,
                AppConfig.MSG_STATE_START_SUCCESS,
                AppConfig.MSG_STATE_START_FAILURE,
                AppConfig.MSG_STATE_STOP_SUCCESS,
                -> context.applySessionAppearance()
            }
        }
    }
}
