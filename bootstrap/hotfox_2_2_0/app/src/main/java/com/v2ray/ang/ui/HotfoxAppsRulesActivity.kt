package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.databinding.ActivityHotfoxAppsRulesBinding

class HotfoxAppsRulesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHotfoxAppsRulesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAppsOpen.setOnClickListener {
            startActivity(Intent(this, PerAppProxyActivity::class.java))
        }
    }
}
