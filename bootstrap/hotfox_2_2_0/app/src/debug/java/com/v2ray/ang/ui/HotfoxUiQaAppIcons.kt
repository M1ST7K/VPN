package com.v2ray.ang.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.v2ray.ang.R

/** Debug-only deterministic app glyphs. Production still uses real installed icons. */
internal object HotfoxUiQaAppIcons {
    fun drawable(context: Context, packageName: String): Drawable {
        val key = packageName.substringBefore('\n')
        val res = when {
            key.contains("telegram") || key == "Мессенджер" -> R.drawable.hf_qa_app_chat
            key.contains("chrome") || key == "Браузер" -> R.drawable.hf_qa_app_browser
            key.contains("sberbank") || key == "Финансы" -> R.drawable.hf_qa_app_bank
            key.contains("whatsapp") -> R.drawable.hf_qa_app_chat
            key.contains("youtube") || key == "Видео" -> R.drawable.hf_qa_app_video
            key.contains("maps") || key.contains("навигация") -> R.drawable.hf_qa_app_maps
            key.contains("gmail") || key.contains("почта") -> R.drawable.hf_qa_app_generic
            key.contains("spotify") || key.contains("музыка") -> R.drawable.hf_qa_app_generic
            else -> R.drawable.hf_qa_app_generic
        }
        return requireNotNull(ContextCompat.getDrawable(context, res))
    }
}
