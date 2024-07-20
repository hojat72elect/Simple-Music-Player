package com.simplemobiletools.musicplayer.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.databinding.DialogExportSettingsBinding
import com.simplemobiletools.musicplayer.extensions.baseConfig
import com.simplemobiletools.musicplayer.extensions.beGone
import com.simplemobiletools.musicplayer.extensions.getAlertDialogBuilder
import com.simplemobiletools.musicplayer.extensions.getDoesFilePathExist
import com.simplemobiletools.musicplayer.extensions.getFilenameFromPath
import com.simplemobiletools.musicplayer.extensions.humanizePath
import com.simplemobiletools.musicplayer.extensions.internalStoragePath
import com.simplemobiletools.musicplayer.extensions.isAValidFilename
import com.simplemobiletools.musicplayer.extensions.setupDialogStuff
import com.simplemobiletools.musicplayer.extensions.toast
import com.simplemobiletools.musicplayer.extensions.value
import com.simplemobiletools.musicplayer.new_architecture.shared.BaseSimpleActivity

class ExportSettingsDialog(
    val activity: BaseSimpleActivity, val defaultFilename: String, val hidePath: Boolean,
    callback: (path: String, filename: String) -> Unit
) {
    init {
        val lastUsedFolder = activity.baseConfig.lastExportedSettingsFolder
        var folder =
            if (lastUsedFolder.isNotEmpty() && activity.getDoesFilePathExist(lastUsedFolder)) {
                lastUsedFolder
            } else {
                activity.internalStoragePath
            }

        val view = DialogExportSettingsBinding.inflate(activity.layoutInflater, null, false).apply {
            exportSettingsFilename.setText(defaultFilename.removeSuffix(".txt"))

            if (hidePath) {
                exportSettingsPathHint.beGone()
            } else {
                exportSettingsPath.setText(activity.humanizePath(folder))
                exportSettingsPath.setOnClickListener {
                    FilePickerDialog(activity, folder, false, showFAB = true) {
                        exportSettingsPath.setText(activity.humanizePath(it))
                        folder = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view.root,
                    this,
                    R.string.export_settings
                ) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        var filename = view.exportSettingsFilename.value
                        if (filename.isEmpty()) {
                            activity.toast(R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        filename += ".txt"
                        val newPath = "${folder.trimEnd('/')}/$filename"
                        if (!newPath.getFilenameFromPath().isAValidFilename()) {
                            activity.toast(R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        activity.baseConfig.lastExportedSettingsFolder = folder
                        if (!hidePath && activity.getDoesFilePathExist(newPath)) {
                            val title = String.format(
                                activity.getString(R.string.file_already_exists_overwrite),
                                newPath.getFilenameFromPath()
                            )
                            ConfirmationDialog(activity, title) {
                                callback(newPath, filename)
                                alertDialog.dismiss()
                            }
                        } else {
                            callback(newPath, filename)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
