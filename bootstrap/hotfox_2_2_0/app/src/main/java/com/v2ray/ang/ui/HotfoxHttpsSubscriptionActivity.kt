package com.v2ray.ang.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityHotfoxHttpsSubscriptionBinding
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import com.v2ray.ang.vpn.HotfoxHttpsImportPolicy
import com.v2ray.ang.vpn.HotfoxImportUiRefresh
import com.v2ray.ang.vpn.HotfoxServerSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HotfoxHttpsSubscriptionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxHttpsSubscriptionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxHttpsSubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxChrome.bindBack(this)
        binding.btnHttpsPaste.setOnClickListener {
            val clip = runCatching { Utils.getClipboard(this) }.getOrNull().orEmpty()
            if (clip.isNotBlank()) binding.etHttpsUrl.setText(clip)
        }
        binding.btnHttpsAdd.setOnClickListener { submit() }
    }

    private fun submit() {
        val value = binding.etHttpsUrl.text?.toString().orEmpty()
        if (!HotfoxHttpsImportPolicy.isHttpsSubscriptionUrl(value)) {
            toast(R.string.hotfox_https_need_https)
            return
        }
        binding.btnHttpsAdd.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val (count, countSub) = AngConfigManager.importBatchConfig(value.trim(), "", true)
                withContext(Dispatchers.Main) {
                    if (HotfoxImportUiRefresh.shouldReloadAfterBatch(count, countSub)) {
                        HotfoxServerSelection.ensureValidSelection()
                        binding.etHttpsUrl.setText("")
                        toast(getString(R.string.title_import_config_count, count.coerceAtLeast(countSub)))
                        finish()
                    } else {
                        toastError(R.string.toast_failure)
                        binding.btnHttpsAdd.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "https import failed", e)
                withContext(Dispatchers.Main) {
                    toastError(R.string.toast_failure)
                    binding.btnHttpsAdd.isEnabled = true
                }
            }
        }
    }
}
