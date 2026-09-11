package com.v2ray.ang.ui

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.v2ray.ang.R
import com.v2ray.ang.databinding.DialogHotfoxAddConnectionBinding

class HotfoxAddConnectionSheet : DialogFragment() {
    fun interface Host {
        fun onAddConnectionAction(action: Action)
    }

    enum class Action { HTTPS, CLIPBOARD, QR, FILE, ALWAYS_ON }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.HotFoxSheetDialog)
        val binding = DialogHotfoxAddConnectionBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        val host = (parentFragment as? Host) ?: (activity as? Host)
        fun send(action: Action) {
            host?.onAddConnectionAction(action)
            dismissAllowingStateLoss()
        }
        binding.rowAddHttps.setOnClickListener { send(Action.HTTPS) }
        binding.rowAddClipboard.setOnClickListener { send(Action.CLIPBOARD) }
        binding.rowAddQr.setOnClickListener { send(Action.QR) }
        binding.rowAddFile.setOnClickListener { send(Action.FILE) }
        binding.rowAddAlwaysOn.setOnClickListener { send(Action.ALWAYS_ON) }
        return dialog
    }

    companion object {
        const val TAG = "hotfox_add_connection"
        fun show(fm: FragmentManager) {
            if (fm.findFragmentByTag(TAG) != null) return
            HotfoxAddConnectionSheet().show(fm, TAG)
        }
    }
}
