package com.simplemobiletools.musicplayer.new_architecture.shared.ui.dialogs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.databinding.DialogExportPlaylistBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.beGone
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.config
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getAlertDialogBuilder
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getCurrentFormattedDateTime
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getParentPath
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.hideKeyboard
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.humanizePath
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.internalStoragePath
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.isAValidFilename
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.setupDialogStuff
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.toast
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.value
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.viewBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.new_architecture.shared.BaseSimpleActivity
import com.simplemobiletools.musicplayer.new_architecture.shared.ui.dialogs.filepicker.FilePickerDialog
import java.io.File

@RequiresApi(Build.VERSION_CODES.O)
class ExportPlaylistDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    private val hidePath: Boolean,
    private val callback: (file: File) -> Unit
) {
    private var ignoreClicks = false
    private var realPath = path.ifEmpty { activity.internalStoragePath }
    private val binding by activity.viewBinding(DialogExportPlaylistBinding::inflate)

    init {
        binding.apply {
            exportPlaylistFolder.text = activity.humanizePath(realPath)

            val fileName = "playlist_${getCurrentFormattedDateTime()}"
            exportPlaylistFilename.setText(fileName)

            if (hidePath) {
                exportPlaylistFolderLabel.beGone()
                exportPlaylistFolder.beGone()
            } else {
                exportPlaylistFolder.setOnClickListener {
                    activity.hideKeyboard(exportPlaylistFilename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        exportPlaylistFolder.text = activity.humanizePath(it)
                        realPath = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    binding.root,
                    this,
                    R.string.export_playlist
                ) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.exportPlaylistFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename.m3u")
                                if (!hidePath && file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ignoreClicks = true
                                ensureBackgroundThread {
                                    activity.config.lastExportPath =
                                        file.absolutePath.getParentPath()
                                    callback(file)
                                    alertDialog.dismiss()
                                }
                            }

                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
