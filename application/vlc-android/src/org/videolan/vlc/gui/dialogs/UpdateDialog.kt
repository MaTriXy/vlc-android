/*
 * ************************************************************************
 *  UpdateDialog.kt
 * *************************************************************************
 * Copyright © 2024 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.coroutines.launch
import org.videolan.tools.KEY_SHOW_UPDATE
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.databinding.DialogUpdateBinding
import org.videolan.vlc.util.AutoUpdate

const val UPDATE_URL = "update_url"

class UpdateDialog : VLCBottomSheetDialogFragment() {
    override fun getDefaultState(): Int = STATE_EXPANDED

    override fun needToManageOrientation(): Boolean = false

    private lateinit var updateURL: String
    private lateinit var binding: DialogUpdateBinding


    override fun initialFocusedView(): View = binding.title

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateURL = savedInstanceState?.getString(UPDATE_URL)
                ?: arguments?.getString(UPDATE_URL)
                        ?: throw IllegalStateException("Update URL not provided")

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(UPDATE_URL, updateURL)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogUpdateBinding.inflate(layoutInflater, container, false)
        binding.download.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!requireActivity().packageManager.canRequestPackageInstalls()) {
                    startActivityForResult(Intent(ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse(String.format("package:%s", requireActivity().packageName))), 1)
                    return@setOnClickListener
                }
            }
            lifecycleScope.launch {
                AutoUpdate.downloadAndInstall(requireActivity().application, updateURL) { loading ->
                        if (loading) {
                            binding.downloadProgress.visibility = View.VISIBLE
                            binding.downloadProgress.isIndeterminate = true
                        } else {
                            binding.downloadProgress.visibility = View.GONE
                    }
                }
            }
        }
        binding.openInBrowser.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, updateURL.toUri()))
        }
        binding.neverAgain.isChecked = !Settings.getInstance(requireActivity()).getBoolean(KEY_SHOW_UPDATE, true)
        binding.neverAgain.setOnCheckedChangeListener { _, isChecked ->
            Settings.getInstance(requireActivity()).putSingle(KEY_SHOW_UPDATE, !isChecked)
        }
        return binding.root
    }
}

