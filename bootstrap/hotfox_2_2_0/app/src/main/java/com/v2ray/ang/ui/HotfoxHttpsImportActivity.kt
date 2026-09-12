package com.v2ray.ang.ui

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityHotfoxHttpsImportBinding
import com.v2ray.ang.extension.toast
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.viewmodel.MainViewModel
import androidx.activity.viewModels

class HotfoxHttpsImportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxHttpsImportBinding
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        binding = ActivityHotfoxHttpsImportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxSystemUi.constrainReadingWidth(binding.httpsForeground)
        binding.root.findViewById<android.view.View>(R.id.btn_header_back)?.apply {
            visibility = android.view.View.VISIBLE
            setOnClickListener { finish() }
        }
        binding.btnHttpsPaste.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) binding.etHttpsUrl.setText(text)
        }
        binding.btnHttpsAdd.setOnClickListener {
            val value = binding.etHttpsUrl.text?.toString()?.trim().orEmpty()
            if (!value.startsWith("https://", ignoreCase = true)) {
                toast(R.string.hotfox_https_need_link)
                return@setOnClickListener
            }
            val (count, countSub) = AngConfigManager.importBatchConfig(value, mainViewModel.subscriptionId, true)
            if (count + countSub > 0) {
                mainViewModel.reloadServerList()
                finish()
            } else {
                toast(R.string.hotfox_https_need_link)
            }
        }
    }
}
