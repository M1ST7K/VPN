package com.v2ray.ang.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.v2ray.ang.R
import com.v2ray.ang.databinding.SheetHotfoxAddConnectionBinding

class HotfoxAddConnectionSheet : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SheetHotfoxAddConnectionBinding.inflate(inflater, container, false)
        binding.btnSheetClose.setOnClickListener { dismiss() }
        binding.rowAddHttps.setOnClickListener {
            startActivity(Intent(requireContext(), HotfoxHttpsImportActivity::class.java))
            dismiss()
        }
        binding.rowAddClipboard.setOnClickListener {
            (activity as? MainActivity)?.importClipboardFromUi()
            dismiss()
        }
        binding.rowAddQr.setOnClickListener {
            (activity as? MainActivity)?.importQrFromUi()
            dismiss()
        }
        binding.rowAddFile.setOnClickListener {
            (activity as? MainActivity)?.importFileFromUi()
            dismiss()
        }
        binding.rowAddAlwaysOn.setOnClickListener {
            startActivity(Intent(requireContext(), HotfoxAlwaysOnActivity::class.java))
            dismiss()
        }
        return binding.root
    }

    override fun getTheme(): Int = R.style.HotFox_BottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setDimAmount(0.18f)
            }
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
        }
        return dialog
    }
}
