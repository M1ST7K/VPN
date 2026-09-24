package com.v2ray.ang.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import kotlin.concurrent.thread

/**
 * Debug-only entry for secret-gated engineering-runtime E2E.
 * Launch via adb; the subscription URL is never an Intent extra.
 */
@Suppress("DEPRECATION")
class HotfoxEngineeringE2eActivity : Activity() {
    private var cycles: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cycles = intent.getIntExtra("cycles", 3).coerceIn(1, 5)
        LogUtil.i(AppConfig.TAG, "${HotfoxEngineeringRuntimeE2e.LOG_PREFIX}: starting")
        maybePrepareThenRun()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_VPN) {
            finish()
            return
        }
        if (resultCode != RESULT_OK) {
            HotfoxEngineeringRuntimeE2e.writeReport(
                applicationContext,
                "engineeringRuntimeE2e=FAIL\nreason=vpn-permission-denied\nphysicalDeviceE2e=NOT_EXECUTED\n",
            )
            finish()
            return
        }
        runOnBackground()
    }

    private fun maybePrepareThenRun() {
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            startActivityForResult(prepare, REQ_VPN)
            return
        }
        runOnBackground()
    }

    private fun runOnBackground() {
        thread(name = "hotfox-e2e") {
            val report = runCatching {
                HotfoxEngineeringRuntimeE2e.run(applicationContext, cycles)
            }.getOrElse { error ->
                LogUtil.e(AppConfig.TAG, "${HotfoxEngineeringRuntimeE2e.LOG_PREFIX}: ${error.javaClass.simpleName}")
                "engineeringRuntimeE2e=FAIL\nreason=exception:${error.javaClass.simpleName}\nphysicalDeviceE2e=NOT_EXECUTED\n"
            }
            runOnUiThread { writeAndFinish(report) }
        }
    }

    private fun writeAndFinish(report: String) {
        HotfoxEngineeringRuntimeE2e.writeReport(applicationContext, report)
        finish()
    }

    companion object {
        private const val REQ_VPN = 91
    }
}
