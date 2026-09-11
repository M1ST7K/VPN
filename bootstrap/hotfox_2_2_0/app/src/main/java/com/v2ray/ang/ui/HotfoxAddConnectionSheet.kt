package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun getTheme(): Int = R.style.HotFoxEditorialTheme
}
