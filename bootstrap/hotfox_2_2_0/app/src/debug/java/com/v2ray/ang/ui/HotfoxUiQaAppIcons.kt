package com.v2ray.ang.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.v2ray.ang.R

/** Debug-only deterministic app glyphs. Production still uses real installed icons. */
internal object HotfoxUiQaAppIcons {
    fun drawable(context: Context, packageName: String): Drawable {
        val res = when (packageName) {
            "Мессенджер", "org.telegram.messenger" -> R.drawable.hf_qa_app_chat
            "Браузер", "com.android.chrome" -> R.drawable.hf_qa_app_browser
            "Финансы" -> R.drawable.hf_qa_app_bank
            "Видео", "com.google.android.youtube" -> R.drawable.hf_qa_app_video
            "Карты и навигация", "com.google.android.apps.maps" -> R.drawable.hf_qa_app_maps
            else -> R.drawable.hf_qa_app_generic
        }
        return requireNotNull(ContextCompat.getDrawable(context, res))
    }
}
