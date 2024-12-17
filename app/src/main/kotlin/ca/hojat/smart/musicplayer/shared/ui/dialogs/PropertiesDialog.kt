package ca.hojat.smart.musicplayer.shared.ui.dialogs

import android.app.Activity
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.shared.BaseSimpleActivity
import ca.hojat.smart.musicplayer.shared.data.models.FileDirItem
import ca.hojat.smart.musicplayer.shared.extensions.beGone
import ca.hojat.smart.musicplayer.shared.extensions.canModifyEXIF
import ca.hojat.smart.musicplayer.shared.extensions.formatAsResolution
import ca.hojat.smart.musicplayer.shared.extensions.formatDate
import ca.hojat.smart.musicplayer.shared.extensions.formatSize
import ca.hojat.smart.musicplayer.shared.extensions.getAlertDialogBuilder
import ca.hojat.smart.musicplayer.shared.extensions.getAndroidSAFUri
import ca.hojat.smart.musicplayer.shared.extensions.getDigest
import ca.hojat.smart.musicplayer.shared.extensions.getDoesFilePathExist
import ca.hojat.smart.musicplayer.shared.extensions.getExifCameraModel
import ca.hojat.smart.musicplayer.shared.extensions.getExifDateTaken
import ca.hojat.smart.musicplayer.shared.extensions.getExifProperties
import ca.hojat.smart.musicplayer.shared.extensions.getFileInputStreamSync
import ca.hojat.smart.musicplayer.shared.extensions.getFilenameFromPath
import ca.hojat.smart.musicplayer.shared.extensions.getIsPathDirectory
import ca.hojat.smart.musicplayer.shared.extensions.getLongValue
import ca.hojat.smart.musicplayer.shared.extensions.isAudioSlow
import ca.hojat.smart.musicplayer.shared.extensions.isImageSlow
import ca.hojat.smart.musicplayer.shared.extensions.isPathOnInternalStorage
import ca.hojat.smart.musicplayer.shared.extensions.isPathOnOTG
import ca.hojat.smart.musicplayer.shared.extensions.isRestrictedSAFOnlyRoot
import ca.hojat.smart.musicplayer.shared.extensions.isVideoSlow
import ca.hojat.smart.musicplayer.shared.extensions.removeValues
import ca.hojat.smart.musicplayer.shared.extensions.setupDialogStuff
import ca.hojat.smart.musicplayer.shared.helpers.MD5
import ca.hojat.smart.musicplayer.shared.helpers.ensureBackgroundThread
import ca.hojat.smart.musicplayer.shared.helpers.sumByInt
import ca.hojat.smart.musicplayer.shared.helpers.sumByLong
import ca.hojat.smart.musicplayer.shared.ui.views.MyTextView
import ca.hojat.smart.musicplayer.shared.usecases.ShowToastUseCase
import java.io.File


class PropertiesDialog : BasePropertiesDialog {
    private var mCountHiddenItems = false

    /**
     * A File Properties dialog constructor with an optional parameter, usable at 1 file selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes (reasonable only at directory properties)
     */
    constructor(activity: Activity, path: String, countHiddenItems: Boolean = false) : super(
        activity
    ) {
        if (!activity.getDoesFilePathExist(path) && !path.startsWith("content://")) {
            ShowToastUseCase(activity, String.format(activity.getString(R.string.source_file_doesnt_exist), path))
            return
        }

        mCountHiddenItems = countHiddenItems
        addProperties(path)

        val builder = activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)

        if (!path.startsWith("content://") && path.canModifyEXIF() && activity.isPathOnInternalStorage(
                path
            )
        ) {
            if (Environment.isExternalStorageManager()) {
                builder.setNeutralButton(R.string.remove_exif, null)
            }
        }

        builder.apply {
            mActivity.setupDialogStuff(mDialogView.root, this, R.string.properties) { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    removeEXIFFromPath(path)
                }
            }
        }
    }

    private fun addProperties(path: String) {
        val fileDirItem =
            FileDirItem(path, path.getFilenameFromPath(), mActivity.getIsPathDirectory(path))
        addProperty(R.string.name, fileDirItem.name)
        addProperty(R.string.path, fileDirItem.getParentPath())
        addProperty(R.string.size, "…", R.id.properties_size)

        ensureBackgroundThread {
            val fileCount = fileDirItem.getProperFileCount(mActivity, mCountHiddenItems)
            val size = fileDirItem.getProperSize(mActivity, mCountHiddenItems).formatSize()

            val directChildrenCount = if (fileDirItem.isDirectory) {
                fileDirItem.getDirectChildrenCount(mActivity, mCountHiddenItems).toString()
            } else {
                0
            }

            this.mActivity.runOnUiThread {
                (mDialogView.propertiesHolder.findViewById<LinearLayout>(R.id.properties_size)
                    .findViewById<MyTextView>(R.id.property_value)).text = size

                if (fileDirItem.isDirectory) {
                    (mDialogView.propertiesHolder.findViewById<LinearLayout>(R.id.properties_file_count)
                        .findViewById<MyTextView>(R.id.property_value)).text = fileCount.toString()
                    (mDialogView.propertiesHolder.findViewById<LinearLayout>(R.id.properties_direct_children_count)
                        .findViewById<MyTextView>(R.id.property_value)).text =
                        directChildrenCount.toString()
                }
            }

            if (!fileDirItem.isDirectory) {
                val projection = arrayOf(MediaStore.Images.Media.DATE_MODIFIED)
                val uri = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DATA} = ?"
                val selectionArgs = arrayOf(path)
                val cursor =
                    mActivity.contentResolver.query(uri, projection, selection, selectionArgs, null)
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        val dateModified =
                            cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000L
                        updateLastModified(mActivity, mDialogView.root, dateModified)
                    } else {
                        updateLastModified(
                            mActivity,
                            mDialogView.root,
                            fileDirItem.getLastModified(mActivity)
                        )
                    }
                }

                val exif = if (mActivity.isPathOnOTG(fileDirItem.path)) {
                    ExifInterface(
                        (mActivity as BaseSimpleActivity).getFileInputStreamSync(
                            fileDirItem.path
                        )!!
                    )
                } else if (fileDirItem.path.startsWith("content://")) {
                    try {
                        ExifInterface(
                            mActivity.contentResolver.openInputStream(
                                Uri.parse(
                                    fileDirItem.path
                                )
                            )!!
                        )
                    } catch (e: Exception) {
                        return@ensureBackgroundThread
                    }
                } else if (mActivity.isRestrictedSAFOnlyRoot(path)) {
                    try {
                        ExifInterface(
                            mActivity.contentResolver.openInputStream(
                                mActivity.getAndroidSAFUri(
                                    path
                                )
                            )!!
                        )
                    } catch (e: Exception) {
                        return@ensureBackgroundThread
                    }
                } else {
                    try {
                        ExifInterface(fileDirItem.path)
                    } catch (e: Exception) {
                        return@ensureBackgroundThread
                    }
                }

                val latLon = FloatArray(2)
                if (exif.getLatLong(latLon)) {
                    mActivity.runOnUiThread {
                        addProperty(R.string.gps_coordinates, "${latLon[0]}, ${latLon[1]}")
                    }
                }

                val altitude = exif.getAltitude(0.0)
                if (altitude != 0.0) {
                    mActivity.runOnUiThread {
                        addProperty(R.string.altitude, "${altitude}m")
                    }
                }
            }
        }

        when {
            fileDirItem.isDirectory -> {
                addProperty(
                    R.string.direct_children_count,
                    "…",
                    R.id.properties_direct_children_count
                )
                addProperty(R.string.files_count, "…", R.id.properties_file_count)
            }

            fileDirItem.path.isImageSlow() -> {
                fileDirItem.getResolution(mActivity)
                    ?.let { addProperty(R.string.resolution, it.formatAsResolution()) }
            }

            fileDirItem.path.isAudioSlow() -> {
                fileDirItem.getDuration(mActivity)?.let { addProperty(R.string.duration, it) }
                fileDirItem.getTitle(mActivity)?.let { addProperty(R.string.song_title, it) }
                fileDirItem.getArtist(mActivity)?.let { addProperty(R.string.artist, it) }
                fileDirItem.getAlbum(mActivity)?.let { addProperty(R.string.album, it) }
            }

            fileDirItem.path.isVideoSlow() -> {
                fileDirItem.getDuration(mActivity)?.let { addProperty(R.string.duration, it) }
                fileDirItem.getResolution(mActivity)
                    ?.let { addProperty(R.string.resolution, it.formatAsResolution()) }
                fileDirItem.getArtist(mActivity)?.let { addProperty(R.string.artist, it) }
                fileDirItem.getAlbum(mActivity)?.let { addProperty(R.string.album, it) }
            }
        }

        if (fileDirItem.isDirectory) {
            addProperty(
                R.string.last_modified,
                fileDirItem.getLastModified(mActivity).formatDate(mActivity)
            )
        } else {
            addProperty(R.string.last_modified, "…", R.id.properties_last_modified)
            try {
                addExifProperties(path, mActivity)
            } catch (e: Exception) {
                ShowToastUseCase(mActivity, "The error : $e")
                return
            }


            addProperty(R.string.md5, "…", R.id.properties_md5)
            ensureBackgroundThread {
                val md5 = if (mActivity.isRestrictedSAFOnlyRoot(path)) {
                    mActivity.contentResolver.openInputStream(mActivity.getAndroidSAFUri(path))?.getDigest(MD5)
                } else {
                    File(path).getDigest(MD5)
                }

                mActivity.runOnUiThread {
                    if (md5 != null) {
                        (mDialogView.propertiesHolder.findViewById<LinearLayout>(R.id.properties_md5)
                            .findViewById<MyTextView>(R.id.property_value)).text = md5
                    } else {
                        mDialogView.propertiesHolder.findViewById<LinearLayout>(R.id.properties_md5)
                            .beGone()
                    }
                }
            }

        }
    }

    private fun updateLastModified(activity: Activity, view: View, timestamp: Long) {
        activity.runOnUiThread {
            (view.findViewById<LinearLayout>(R.id.properties_last_modified)
                .findViewById<MyTextView>(R.id.property_value)).text =
                timestamp.formatDate(activity)
        }
    }

    /**
     * A File Properties dialog constructor with an optional parameter, usable at multiple items selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param paths the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes
     */
    constructor(activity: Activity, paths: List<String>, countHiddenItems: Boolean = false) : super(
        activity
    ) {
        mCountHiddenItems = countHiddenItems

        val fileDirItems = ArrayList<FileDirItem>(paths.size)
        paths.forEach {
            val fileDirItem =
                FileDirItem(it, it.getFilenameFromPath(), activity.getIsPathDirectory(it))
            fileDirItems.add(fileDirItem)
        }

        val isSameParent = isSameParent(fileDirItems)

        addProperty(R.string.items_selected, paths.size.toString())
        if (isSameParent) {
            addProperty(R.string.path, fileDirItems[0].getParentPath())
        }

        addProperty(R.string.size, "…", R.id.properties_size)
        addProperty(R.string.files_count, "…", R.id.properties_file_count)

        ensureBackgroundThread {
            val fileCount =
                fileDirItems.sumByInt { it.getProperFileCount(activity, countHiddenItems) }
            val size =
                fileDirItems.sumByLong { it.getProperSize(activity, countHiddenItems) }.formatSize()
            activity.runOnUiThread {
                (mDialogView.propertiesHolder.findViewById<LinearLayout>(R.id.properties_size)
                    .findViewById<MyTextView>(R.id.property_value)).text = size
                (mDialogView.propertiesHolder.findViewById<LinearLayout>(R.id.properties_file_count)
                    .findViewById<MyTextView>(R.id.property_value)).text = fileCount.toString()
            }
        }

        val builder = activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)

        if (!paths.any { it.startsWith("content://") } && paths.any { it.canModifyEXIF() } && paths.any {
                activity.isPathOnInternalStorage(
                    it
                )
            }) {
            if (Environment.isExternalStorageManager()) {
                builder.setNeutralButton(R.string.remove_exif, null)
            }
        }

        builder.apply {
            mActivity.setupDialogStuff(mDialogView.root, this, R.string.properties) { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    removeEXIFFromPaths(paths)
                }
            }
        }
    }

    private fun addExifProperties(path: String, activity: Activity) {
        val exif = if (activity.isPathOnOTG(path)) {
            ExifInterface((activity as BaseSimpleActivity).getFileInputStreamSync(path)!!)
        } else if (path.startsWith("content://")) {
            try {
                ExifInterface(activity.contentResolver.openInputStream(Uri.parse(path))!!)
            } catch (e: Exception) {
                return
            }
        } else if (activity.isRestrictedSAFOnlyRoot(path)) {
            try {
                ExifInterface(
                    activity.contentResolver.openInputStream(
                        activity.getAndroidSAFUri(
                            path
                        )
                    )!!
                )
            } catch (e: Exception) {
                return
            }
        } else {
            ExifInterface(path)
        }

        val dateTaken = exif.getExifDateTaken(activity)
        if (dateTaken.isNotEmpty()) {
            addProperty(R.string.date_taken, dateTaken)
        }

        val cameraModel = exif.getExifCameraModel()
        if (cameraModel.isNotEmpty()) {
            addProperty(R.string.camera, cameraModel)
        }

        val exifString = exif.getExifProperties()
        if (exifString.isNotEmpty()) {
            addProperty(R.string.exif, exifString)
        }
    }

    private fun removeEXIFFromPath(path: String) {
        ConfirmationDialog(mActivity, "", R.string.remove_exif_confirmation) {
            try {
                ExifInterface(path).removeValues()
                ShowToastUseCase(mActivity, R.string.exif_removed)
                mPropertyView.findViewById<LinearLayout>(R.id.properties_holder).removeAllViews()
                addProperties(path)
            } catch (e: Exception) {
                ShowToastUseCase(mActivity, "The error : $e")
            }
        }
    }

    private fun removeEXIFFromPaths(paths: List<String>) {
        ConfirmationDialog(mActivity, "", R.string.remove_exif_confirmation) {
            try {
                paths.filter { mActivity.isPathOnInternalStorage(it) && it.canModifyEXIF() }
                    .forEach {
                        ExifInterface(it).removeValues()
                    }
                ShowToastUseCase(mActivity, R.string.exif_removed)
            } catch (e: Exception) {
                ShowToastUseCase(mActivity, "The error : $e")
            }
        }
    }

    private fun isSameParent(fileDirItems: List<FileDirItem>): Boolean {
        var parent = fileDirItems[0].getParentPath()
        for (file in fileDirItems) {
            val curParent = file.getParentPath()
            if (curParent != parent) {
                return false
            }

            parent = curParent
        }
        return true
    }
}
