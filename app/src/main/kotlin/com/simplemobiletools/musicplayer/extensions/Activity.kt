package com.simplemobiletools.musicplayer.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.TransactionTooLargeException
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplemobiletools.commons.databinding.DialogTitleBinding
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.dialogs.DonateDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RateStarsDialog
import com.simplemobiletools.commons.dialogs.UpgradeToProDialog
import com.simplemobiletools.commons.dialogs.WhatsNewDialog
import com.simplemobiletools.commons.dialogs.WritePermissionDialog
import com.simplemobiletools.commons.dialogs.WritePermissionDialog.WritePermissionDialogMode
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.canManageMedia
import com.simplemobiletools.commons.extensions.deleteFromMediaStore
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.getInternalStoragePath
import com.simplemobiletools.commons.extensions.getSomeDocumentFile
import com.simplemobiletools.commons.extensions.hasProperStoredTreeUri
import com.simplemobiletools.commons.extensions.isAccessibleWithSAFSdk30
import com.simplemobiletools.commons.extensions.isPathOnInternalStorage
import com.simplemobiletools.commons.extensions.isRestrictedSAFOnlyRoot
import com.simplemobiletools.commons.extensions.needsStupidWritePermissions
import com.simplemobiletools.commons.extensions.renameAndroidSAFDocument
import com.simplemobiletools.commons.extensions.renameDocumentSdk30
import com.simplemobiletools.commons.extensions.rescanPath
import com.simplemobiletools.commons.extensions.rescanPaths
import com.simplemobiletools.commons.extensions.scanPathRecursively
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.updateInMediaStore
import com.simplemobiletools.commons.extensions.updateLastModified
import com.simplemobiletools.commons.helpers.CREATE_DOCUMENT_SDK_30
import com.simplemobiletools.commons.helpers.EXTRA_SHOW_ADVANCED
import com.simplemobiletools.commons.helpers.OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB
import com.simplemobiletools.commons.helpers.OPEN_DOCUMENT_TREE_FOR_SDK_30
import com.simplemobiletools.commons.helpers.OPEN_DOCUMENT_TREE_OTG
import com.simplemobiletools.commons.helpers.OPEN_DOCUMENT_TREE_SD
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isOnMainThread
import com.simplemobiletools.commons.helpers.isRPlus
import com.simplemobiletools.commons.models.Android30RenameFormat
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.views.MyTextView
import com.simplemobiletools.musicplayer.BuildConfig
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.activities.BaseSimpleActivity
import com.simplemobiletools.musicplayer.dialogs.SelectPlaylistDialog
import com.simplemobiletools.musicplayer.helpers.FLAG_MANUAL_CACHE
import com.simplemobiletools.musicplayer.helpers.RoomHelper
import com.simplemobiletools.musicplayer.models.Track
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


fun Activity.launchViewIntent(url: String) {
    hideKeyboard()
    ensureBackgroundThread {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            try {
                startActivity(this)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_browser_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}

const val DEVELOPER_PLAY_STORE_URL = "https://play.google.com/store/apps/dev?id=9070296388022589266"

fun Activity.launchMoreAppsFromUsIntent() {
    launchViewIntent(DEVELOPER_PLAY_STORE_URL)
}

fun BaseSimpleActivity.checkWhatsNew(releases: List<Release>, currVersion: Int) {
    if (baseConfig.lastVersion == 0) {
        baseConfig.lastVersion = currVersion
        return
    }

    val newReleases = arrayListOf<Release>()
    releases.filterTo(newReleases) { it.id > baseConfig.lastVersion }

    if (newReleases.isNotEmpty()) {
        WhatsNewDialog(this, newReleases)
    }

    baseConfig.lastVersion = currVersion
}

fun BaseSimpleActivity.isShowingOTGDialog(path: String): Boolean {
    return if (!isRPlus() && isPathOnOTG(path) && (baseConfig.OTGTreeUri.isEmpty() || !hasProperStoredTreeUri(
            true
        ))
    ) {
        showOTGPermissionDialog(path)
        true
    } else {
        false
    }
}


fun BaseSimpleActivity.showOTGPermissionDialog(path: String) {
    runOnUiThread {
        if (!isDestroyed && !isFinishing) {
            WritePermissionDialog(this, WritePermissionDialogMode.Otg) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    try {
                        startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                        checkedDocumentPath = path
                        return@apply
                    } catch (e: Exception) {
                        type = "*/*"
                    }

                    try {
                        startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                        checkedDocumentPath = path
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
    }
}

@SuppressLint("InlinedApi")
fun BaseSimpleActivity.isShowingSAFDialogSdk30(path: String): Boolean {
    return if (isAccessibleWithSAFSdk30(path) && !hasProperStoredFirstParentUri(path)) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                val level = getFirstParentLevel(path)
                WritePermissionDialog(
                    this,
                    WritePermissionDialogMode.OpenDocumentTreeSDK30(
                        path.getFirstParentPath(
                            this,
                            level
                        )
                    )
                ) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra(EXTRA_SHOW_ADVANCED, true)
                        putExtra(
                            DocumentsContract.EXTRA_INITIAL_URI,
                            createFirstParentTreeUriUsingRootTree(path)
                        )
                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_FOR_SDK_30)
                            checkedDocumentPath = path
                            return@apply
                        } catch (e: Exception) {
                            type = "*/*"
                        }

                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_FOR_SDK_30)
                            checkedDocumentPath = path
                        } catch (e: ActivityNotFoundException) {
                            toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                        } catch (e: Exception) {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

@SuppressLint("InlinedApi")
fun BaseSimpleActivity.isShowingSAFCreateDocumentDialogSdk30(path: String): Boolean {
    return if (!hasProperStoredDocumentUriSdk30(path)) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                WritePermissionDialog(this, WritePermissionDialogMode.CreateDocumentSDK30) {
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        type = DocumentsContract.Document.MIME_TYPE_DIR
                        putExtra(EXTRA_SHOW_ADVANCED, true)
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(
                            DocumentsContract.EXTRA_INITIAL_URI,
                            buildDocumentUriSdk30(path.getParentPath())
                        )
                        putExtra(Intent.EXTRA_TITLE, path.getFilenameFromPath())
                        try {
                            startActivityForResult(this, CREATE_DOCUMENT_SDK_30)
                            checkedDocumentPath = path
                            return@apply
                        } catch (e: Exception) {
                            type = "*/*"
                        }

                        try {
                            startActivityForResult(this, CREATE_DOCUMENT_SDK_30)
                            checkedDocumentPath = path
                        } catch (e: ActivityNotFoundException) {
                            toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                        } catch (e: Exception) {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

fun BaseSimpleActivity.isShowingAndroidSAFDialog(path: String): Boolean {
    return if (isRestrictedSAFOnlyRoot(path) && (getAndroidTreeUri(path).isEmpty() || !hasProperStoredAndroidTreeUri(
            path
        ))
    ) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                ConfirmationAdvancedDialog(
                    this,
                    "",
                    R.string.confirm_storage_access_android_text,
                    R.string.ok,
                    R.string.cancel
                ) { success ->
                    if (success) {
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            putExtra(EXTRA_SHOW_ADVANCED, true)
                            putExtra(
                                DocumentsContract.EXTRA_INITIAL_URI,
                                createAndroidDataOrObbUri(path)
                            )
                            try {
                                startActivityForResult(
                                    this,
                                    OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB
                                )
                                checkedDocumentPath = path
                                return@apply
                            } catch (e: Exception) {
                                type = "*/*"
                            }

                            try {
                                startActivityForResult(
                                    this,
                                    OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB
                                )
                                checkedDocumentPath = path
                            } catch (e: ActivityNotFoundException) {
                                toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                            } catch (e: Exception) {
                                toast(R.string.unknown_error_occurred)
                            }
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

private fun BaseSimpleActivity.deleteSdk30(fileDirItem: FileDirItem, callback: ((wasSuccess: Boolean) -> Unit)?) {
    val fileUris = getFileUrisFromFileDirItems(arrayListOf(fileDirItem))
    deleteSDK30Uris(fileUris) { success ->
        runOnUiThread {
            callback?.invoke(success)
        }
    }
}

private fun deleteRecursively(file: File, context: Context): Boolean {
    if (file.isDirectory) {
        val files = file.listFiles() ?: return file.delete()
        for (child in files) {
            deleteRecursively(child, context)
        }
    }

    val deleted = file.delete()
    if (deleted) {
        context.deleteFromMediaStore(file.absolutePath)
    }
    return deleted
}

fun BaseSimpleActivity.deleteFileBg(
    fileDirItem: FileDirItem,
    allowDeleteFolder: Boolean = false,
    isDeletingMultipleFiles: Boolean,
    callback: ((wasSuccess: Boolean) -> Unit)? = null,
) {
    val path = fileDirItem.path
    if (isRestrictedSAFOnlyRoot(path)) {
        deleteAndroidSAFDirectory(path, allowDeleteFolder, callback)
    } else {
        val file = File(path)
        if (!isRPlus() && file.absolutePath.startsWith(internalStoragePath) && !file.canWrite()) {
            callback?.invoke(false)
            return
        }

        var fileDeleted = !isPathOnOTG(path) && ((!file.exists() && file.length() == 0L) || file.delete())
        if (fileDeleted) {
            deleteFromMediaStore(path) { needsRescan ->
                if (needsRescan) {
                    rescanAndDeletePath(path) {
                        runOnUiThread {
                            callback?.invoke(true)
                        }
                    }
                } else {
                    runOnUiThread {
                        callback?.invoke(true)
                    }
                }
            }
        } else {
            if (getIsPathDirectory(file.absolutePath) && allowDeleteFolder) {
                fileDeleted = deleteRecursively(file, this)
            }

            if (!fileDeleted) {
                if (needsStupidWritePermissions(path)) {
                    handleSAFDialog(path) {
                        if (it) {
                            trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
                        }
                    }
                } else if (isAccessibleWithSAFSdk30(path)) {
                    if (canManageMedia()) {
                        deleteSdk30(fileDirItem, callback)
                    } else {
                        handleSAFDialogSdk30(path) {
                            if (it) {
                                deleteDocumentWithSAFSdk30(fileDirItem, allowDeleteFolder, callback)
                            }
                        }
                    }
                } else if (isRPlus() && !isDeletingMultipleFiles) {
                    deleteSdk30(fileDirItem, callback)
                } else {
                    callback?.invoke(false)
                }
            }
        }
    }
}

private fun BaseSimpleActivity.renameCasually(
    oldPath: String,
    newPath: String,
    isRenamingMultipleFiles: Boolean,
    callback: ((success: Boolean, android30RenameFormat: Android30RenameFormat) -> Unit)?
) {
    val oldFile = File(oldPath)
    val newFile = File(newPath)
    val tempFile = try {
        createTempFile(oldFile) ?: return
    } catch (exception: Exception) {
        if (isRPlus() && exception is java.nio.file.FileSystemException) {
            // if we are renaming multiple files at once, we should give the Android 30+ permission dialog all uris together, not one by one
            if (isRenamingMultipleFiles) {
                callback?.invoke(false, Android30RenameFormat.CONTENT_RESOLVER)
            } else {
                val fileUris =
                    getFileUrisFromFileDirItems(arrayListOf(File(oldPath).toFileDirItem(this)))
                updateSDK30Uris(fileUris) { success ->
                    if (success) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, newPath.getFilenameFromPath())
                        }

                        try {
                            contentResolver.update(fileUris.first(), values, null, null)
                            callback?.invoke(true, Android30RenameFormat.NONE)
                        } catch (e: Exception) {
                            showErrorToast(e)
                            callback?.invoke(false, Android30RenameFormat.NONE)
                        }
                    } else {
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        } else {
            if (exception is IOException && File(oldPath).isDirectory && isRestrictedWithSAFSdk30(
                    oldPath
                )
            ) {
                toast(R.string.cannot_rename_folder)
            } else {
                showErrorToast(exception)
            }
            callback?.invoke(false, Android30RenameFormat.NONE)
        }
        return
    }

    val oldToTempSucceeds = oldFile.renameTo(tempFile)
    val tempToNewSucceeds = tempFile.renameTo(newFile)
    if (oldToTempSucceeds && tempToNewSucceeds) {
        if (newFile.isDirectory) {
            updateInMediaStore(oldPath, newPath)
            rescanPath(newPath) {
                runOnUiThread {
                    callback?.invoke(true, Android30RenameFormat.NONE)
                }
                if (!oldPath.equals(newPath, true)) {
                    deleteFromMediaStore(oldPath)
                }
                scanPathRecursively(newPath)
            }
        } else {
            if (!baseConfig.keepLastModified) {
                newFile.setLastModified(System.currentTimeMillis())
            }
            updateInMediaStore(oldPath, newPath)
            scanPathsRecursively(arrayListOf(newPath)) {
                if (!oldPath.equals(newPath, true)) {
                    deleteFromMediaStore(oldPath)
                }
                runOnUiThread {
                    callback?.invoke(true, Android30RenameFormat.NONE)
                }
            }
        }
    } else {
        tempFile.delete()
        newFile.delete()
        if (isRPlus()) {
            // if we are renaming multiple files at once, we should give the Android 30+ permission dialog all uris together, not one by one
            if (isRenamingMultipleFiles) {
                callback?.invoke(false, Android30RenameFormat.SAF)
            } else {
                val fileUris =
                    getFileUrisFromFileDirItems(arrayListOf(File(oldPath).toFileDirItem(this)))
                updateSDK30Uris(fileUris) { success ->
                    if (!success) {
                        return@updateSDK30Uris
                    }
                    try {
                        val sourceUri = fileUris.first()
                        val sourceFile = File(oldPath).toFileDirItem(this)

                        if (oldPath.equals(newPath, true)) {
                            val tempDestination = try {
                                createTempFile(File(sourceFile.path)) ?: return@updateSDK30Uris
                            } catch (exception: Exception) {
                                showErrorToast(exception)
                                callback?.invoke(false, Android30RenameFormat.NONE)
                                return@updateSDK30Uris
                            }

                            val copyTempSuccess =
                                copySingleFileSdk30(sourceFile, tempDestination.toFileDirItem(this))
                            if (copyTempSuccess) {
                                contentResolver.delete(sourceUri, null)
                                tempDestination.renameTo(File(newPath))
                                if (!baseConfig.keepLastModified) {
                                    newFile.setLastModified(System.currentTimeMillis())
                                }
                                updateInMediaStore(oldPath, newPath)
                                scanPathsRecursively(arrayListOf(newPath)) {
                                    runOnUiThread {
                                        callback?.invoke(true, Android30RenameFormat.NONE)
                                    }
                                }
                            } else {
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        } else {
                            val destinationFile = FileDirItem(
                                newPath,
                                newPath.getFilenameFromPath(),
                                sourceFile.isDirectory,
                                sourceFile.children,
                                sourceFile.size,
                                sourceFile.modified
                            )
                            val copySuccessful = copySingleFileSdk30(sourceFile, destinationFile)
                            if (copySuccessful) {
                                if (!baseConfig.keepLastModified) {
                                    newFile.setLastModified(System.currentTimeMillis())
                                }
                                contentResolver.delete(sourceUri, null)
                                updateInMediaStore(oldPath, newPath)
                                scanPathsRecursively(arrayListOf(newPath)) {
                                    runOnUiThread {
                                        callback?.invoke(true, Android30RenameFormat.NONE)
                                    }
                                }
                            } else {
                                toast(R.string.unknown_error_occurred)
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        }

                    } catch (e: Exception) {
                        showErrorToast(e)
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        } else {
            toast(R.string.unknown_error_occurred)
            callback?.invoke(false, Android30RenameFormat.NONE)
        }
    }
}

fun Activity.showDonateOrUpgradeDialog() {
    if (getCanAppBeUpgraded()) {
        UpgradeToProDialog(this)
    } else if (!isOrWasThankYouInstalled()) {
        DonateDialog(this)
    }
}

fun Activity.rescanPaths(paths: List<String>, callback: (() -> Unit)? = null) {
    applicationContext.rescanPaths(paths, callback)
}

fun Activity.createTempFile(file: File): File? {
    return if (file.isDirectory) {
        createTempDir("temp", "${System.currentTimeMillis()}", file.parentFile)
    } else {
        if (isRPlus()) {
            // this can throw FileSystemException, lets catch and handle it at the place calling this function
            kotlin.io.path.createTempFile(
                file.parentFile.toPath(),
                "temp",
                "${System.currentTimeMillis()}"
            ).toFile()
        } else {
            createTempFile("temp", "${System.currentTimeMillis()}", file.parentFile)
        }
    }
}

fun Activity.rescanPath(path: String, callback: (() -> Unit)? = null) {
    applicationContext.rescanPath(path, callback)
}


fun BaseSimpleActivity.isShowingSAFDialog(path: String): Boolean {
    return if ((!isRPlus() && isPathOnSD(path) && !isSDCardSetAsDefaultStorage() && (baseConfig.sdTreeUri.isEmpty() || !hasProperStoredTreeUri(
            false
        )))
    ) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                WritePermissionDialog(this, WritePermissionDialogMode.SdCard) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra(EXTRA_SHOW_ADVANCED, true)
                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_SD)
                            checkedDocumentPath = path
                            return@apply
                        } catch (e: Exception) {
                            type = "*/*"
                        }

                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_SD)
                            checkedDocumentPath = path
                        } catch (e: ActivityNotFoundException) {
                            toast(
                                com.simplemobiletools.commons.R.string.system_service_disabled,
                                Toast.LENGTH_LONG
                            )
                        } catch (e: Exception) {
                            toast(com.simplemobiletools.commons.R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

fun BaseSimpleActivity.copySingleFileSdk30(source: FileDirItem, destination: FileDirItem): Boolean {
    val directory = destination.getParentPath()
    if (!createDirectorySync(directory)) {
        val error = String.format(getString(R.string.could_not_create_folder), directory)
        showErrorToast(error)
        return false
    }

    var inputStream: InputStream? = null
    var out: OutputStream? = null
    try {

        out = getFileOutputStreamSync(destination.path, source.path.getMimeType())
        inputStream = getFileInputStreamSync(source.path)!!

        var copiedSize = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = inputStream.read(buffer)
        while (bytes >= 0) {
            out!!.write(buffer, 0, bytes)
            copiedSize += bytes
            bytes = inputStream.read(buffer)
        }

        out?.flush()

        return if (source.size == copiedSize && getDoesFilePathExist(destination.path)) {
            if (baseConfig.keepLastModified) {
                copyOldLastModified(source.path, destination.path)
                val lastModified = File(source.path).lastModified()
                if (lastModified != 0L) {
                    File(destination.path).setLastModified(lastModified)
                }
            }
            true
        } else {
            false
        }
    } finally {
        inputStream?.close()
        out?.close()
    }
}


fun BaseSimpleActivity.copyOldLastModified(sourcePath: String, destinationPath: String) {
    val projection =
        arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)
    val uri = MediaStore.Files.getContentUri("external")
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    var selectionArgs = arrayOf(sourcePath)
    val cursor =
        applicationContext.contentResolver.query(uri, projection, selection, selectionArgs, null)

    cursor?.use {
        if (cursor.moveToFirst()) {
            val dateTaken = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
            val dateModified = cursor.getIntValue(MediaStore.Images.Media.DATE_MODIFIED)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATE_TAKEN, dateTaken)
                put(MediaStore.Images.Media.DATE_MODIFIED, dateModified)
            }

            selectionArgs = arrayOf(destinationPath)
            applicationContext.contentResolver.update(uri, values, selection, selectionArgs)
        }
    }
}


fun Activity.getFinalUriFromPath(path: String, applicationId: String): Uri? {
    val uri = try {
        ensurePublicUri(path, applicationId)
    } catch (e: Exception) {
        showErrorToast(e)
        return null
    }

    if (uri == null) {
        toast(R.string.unknown_error_occurred)
        return null
    }

    return uri
}

fun Activity.appLaunched(appId: String) {
    baseConfig.internalStoragePath = getInternalStoragePath()
    updateSDCardPath()
    baseConfig.appId = appId
    if (baseConfig.appRunCount == 0) {
        baseConfig.wasOrangeIconChecked = true
        checkAppIconColor()
    } else if (!baseConfig.wasOrangeIconChecked) {
        baseConfig.wasOrangeIconChecked = true
        val primaryColor = resources.getColor(R.color.color_primary)
        if (baseConfig.appIconColor != primaryColor) {
            getAppIconColors().forEachIndexed { index, color ->
                toggleAppIconColor(appId, index, color, false)
            }

            val defaultClassName =
                "${baseConfig.appId.removeSuffix(".debug")}.activities.SplashActivity"
            packageManager.setComponentEnabledSetting(
                ComponentName(baseConfig.appId, defaultClassName),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )

            val orangeClassName =
                "${baseConfig.appId.removeSuffix(".debug")}.activities.SplashActivity.Orange"
            packageManager.setComponentEnabledSetting(
                ComponentName(baseConfig.appId, orangeClassName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            baseConfig.appIconColor = primaryColor
            baseConfig.lastIconColor = primaryColor
        }
    }

    baseConfig.appRunCount++
    if (baseConfig.appRunCount % 30 == 0 && !isAProApp()) {
        if (!resources.getBoolean(R.bool.hide_google_relations)) {
            showDonateOrUpgradeDialog()
        }
    }

    if (baseConfig.appRunCount % 40 == 0 && !baseConfig.wasAppRated) {
        if (!resources.getBoolean(R.bool.hide_google_relations)) {
            RateStarsDialog(this)
        }
    }
}

fun BaseSimpleActivity.renameFile(
    oldPath: String,
    newPath: String,
    isRenamingMultipleFiles: Boolean,
    callback: ((success: Boolean, android30RenameFormat: Android30RenameFormat) -> Unit)? = null
) {
    if (isRestrictedSAFOnlyRoot(oldPath)) {
        handleAndroidSAFDialog(oldPath) {
            if (!it) {
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
                return@handleAndroidSAFDialog
            }

            try {
                ensureBackgroundThread {
                    val success = renameAndroidSAFDocument(oldPath, newPath)
                    runOnUiThread {
                        callback?.invoke(success, Android30RenameFormat.NONE)
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
            }
        }
    } else if (isAccessibleWithSAFSdk30(oldPath)) {
        if (canManageMedia() && !File(oldPath).isDirectory && isPathOnInternalStorage(oldPath)) {
            renameCasually(oldPath, newPath, isRenamingMultipleFiles, callback)
        } else {
            handleSAFDialogSdk30(oldPath) {
                if (!it) {
                    return@handleSAFDialogSdk30
                }

                try {
                    ensureBackgroundThread {
                        val success = renameDocumentSdk30(oldPath, newPath)
                        if (success) {
                            updateInMediaStore(oldPath, newPath)
                            rescanPath(newPath) {
                                runOnUiThread {
                                    callback?.invoke(true, Android30RenameFormat.NONE)
                                }
                                if (!oldPath.equals(newPath, true)) {
                                    deleteFromMediaStore(oldPath)
                                }
                                scanPathRecursively(newPath)
                            }
                        } else {
                            runOnUiThread {
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        }
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                    runOnUiThread {
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        }
    } else if (needsStupidWritePermissions(newPath)) {
        handleSAFDialog(newPath) {
            if (!it) {
                return@handleSAFDialog
            }

            val document = getSomeDocumentFile(oldPath)
            if (document == null || (File(oldPath).isDirectory != document.isDirectory)) {
                runOnUiThread {
                    toast(R.string.unknown_error_occurred)
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
                return@handleSAFDialog
            }

            try {
                ensureBackgroundThread {
                    try {
                        DocumentsContract.renameDocument(
                            applicationContext.contentResolver,
                            document.uri,
                            newPath.getFilenameFromPath()
                        )
                    } catch (ignored: FileNotFoundException) {
                        // FileNotFoundException is thrown in some weird cases, but renaming works just fine
                    } catch (e: Exception) {
                        showErrorToast(e)
                        callback?.invoke(false, Android30RenameFormat.NONE)
                        return@ensureBackgroundThread
                    }

                    updateInMediaStore(oldPath, newPath)
                    rescanPaths(arrayListOf(oldPath, newPath)) {
                        if (!baseConfig.keepLastModified) {
                            updateLastModified(newPath, System.currentTimeMillis())
                        }
                        deleteFromMediaStore(oldPath)
                        runOnUiThread {
                            callback?.invoke(true, Android30RenameFormat.NONE)
                        }
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
            }
        }
    } else renameCasually(oldPath, newPath, isRenamingMultipleFiles, callback)
}

fun Activity.setupDialogStuff(
    view: View,
    dialog: AlertDialog.Builder,
    titleId: Int = 0,
    titleText: String = "",
    cancelOnTouchOutside: Boolean = true,
    callback: ((alertDialog: AlertDialog) -> Unit)? = null
) {
    if (isDestroyed || isFinishing) {
        return
    }

    val textColor = getProperTextColor()
    val backgroundColor = getProperBackgroundColor()
    val primaryColor = getProperPrimaryColor()
    if (view is ViewGroup) {
        updateTextColors(view)
    } else if (view is MyTextView) {
        view.setColors(textColor, primaryColor, backgroundColor)
    }

    if (dialog is MaterialAlertDialogBuilder) {
        dialog.create().apply {
            if (titleId != 0) {
                setTitle(titleId)
            } else if (titleText.isNotEmpty()) {
                setTitle(titleText)
            }

            setView(view)
            setCancelable(cancelOnTouchOutside)
            if (!isFinishing) {
                show()
            }
            getButton(Dialog.BUTTON_POSITIVE)?.setTextColor(primaryColor)
            getButton(Dialog.BUTTON_NEGATIVE)?.setTextColor(primaryColor)
            getButton(Dialog.BUTTON_NEUTRAL)?.setTextColor(primaryColor)
            callback?.invoke(this)
        }
    } else {
        var title: DialogTitleBinding? = null
        if (titleId != 0 || titleText.isNotEmpty()) {
            title = DialogTitleBinding.inflate(layoutInflater, null, false)
            title.dialogTitleTextview.apply {
                if (titleText.isNotEmpty()) {
                    text = titleText
                } else {
                    setText(titleId)
                }
                setTextColor(textColor)
            }
        }

        // if we use the same primary and background color, use the text color for dialog confirmation buttons
        val dialogButtonColor = if (primaryColor == baseConfig.backgroundColor) {
            textColor
        } else {
            primaryColor
        }

        dialog.create().apply {
            setView(view)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCustomTitle(title?.root)
            setCanceledOnTouchOutside(cancelOnTouchOutside)
            if (!isFinishing) {
                show()
            }
            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(dialogButtonColor)

            val bgDrawable = when {
                isBlackAndWhiteTheme() -> resources.getDrawable(
                    R.drawable.black_dialog_background,
                    theme
                )

                baseConfig.isUsingSystemTheme -> resources.getDrawable(
                    R.drawable.dialog_you_background,
                    theme
                )

                else -> resources.getColoredDrawableWithColor(
                    R.drawable.dialog_bg,
                    baseConfig.backgroundColor
                )
            }

            window?.setBackgroundDrawable(bgDrawable)
            callback?.invoke(this)
        }
    }
}

fun Activity.sharePathIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(EXTRA_STREAM, newUri)
            type = getUriMimeType(path, newUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            grantUriPermission("android", newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(Intent.createChooser(this, getString(R.string.share_via)))
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                } else {
                    showErrorToast(e)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}

fun Activity.sharePathsIntent(paths: List<String>, applicationId: String) {
    ensureBackgroundThread {
        if (paths.size == 1) {
            sharePathIntent(paths.first(), applicationId)
        } else {
            val uriPaths = java.util.ArrayList<String>()
            val newUris = paths.map {
                val uri = getFinalUriFromPath(it, applicationId) ?: return@ensureBackgroundThread
                uriPaths.add(uri.path!!)
                uri
            } as java.util.ArrayList<Uri>

            var mimeType = uriPaths.getMimeType()
            if (mimeType.isEmpty() || mimeType == "*/*") {
                mimeType = paths.getMimeType()
            }

            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(EXTRA_STREAM, newUris)

                try {
                    startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.no_app_found)
                } catch (e: RuntimeException) {
                    if (e.cause is TransactionTooLargeException) {
                        toast(R.string.maximum_share_reached)
                    } else {
                        showErrorToast(e)
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }
}

inline fun <T : ViewBinding> Activity.viewBinding(crossinline bindingInflater: (LayoutInflater) -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(layoutInflater)
    }


fun Activity.getAlertDialogBuilder() = if (baseConfig.isUsingSystemTheme) {
    MaterialAlertDialogBuilder(this)
} else {
    AlertDialog.Builder(this)
}

private fun createCasualFileOutputStream(
    activity: BaseSimpleActivity,
    targetFile: File
): OutputStream? {
    if (targetFile.parentFile?.exists() == false) {
        targetFile.parentFile?.mkdirs()
    }

    return try {
        FileOutputStream(targetFile)
    } catch (e: Exception) {
        activity.showErrorToast(e)
        null
    }
}

fun BaseSimpleActivity.getFileOutputStream(
    fileDirItem: FileDirItem,
    allowCreatingNewFile: Boolean = false,
    callback: (outputStream: OutputStream?) -> Unit
) {
    val targetFile = File(fileDirItem.path)
    when {
        isRestrictedSAFOnlyRoot(fileDirItem.path) -> {
            handleAndroidSAFDialog(fileDirItem.path) {
                if (!it) {
                    return@handleAndroidSAFDialog
                }

                val uri = getAndroidSAFUri(fileDirItem.path)
                if (!getDoesFilePathExist(fileDirItem.path)) {
                    createAndroidSAFFile(fileDirItem.path)
                }
                callback.invoke(applicationContext.contentResolver.openOutputStream(uri, "wt"))
            }
        }

        needsStupidWritePermissions(fileDirItem.path) -> {
            handleSAFDialog(fileDirItem.path) {
                if (!it) {
                    return@handleSAFDialog
                }

                var document = getDocumentFile(fileDirItem.path)
                if (document == null && allowCreatingNewFile) {
                    document = getDocumentFile(fileDirItem.getParentPath())
                }

                if (document == null) {
                    showFileCreateError(fileDirItem.path)
                    callback(null)
                    return@handleSAFDialog
                }

                if (!getDoesFilePathExist(fileDirItem.path)) {
                    document = getDocumentFile(fileDirItem.path) ?: document.createFile(
                        "",
                        fileDirItem.name
                    )
                }

                if (document?.exists() == true) {
                    try {
                        callback(
                            applicationContext.contentResolver.openOutputStream(
                                document.uri,
                                "wt"
                            )
                        )
                    } catch (e: FileNotFoundException) {
                        showErrorToast(e)
                        callback(null)
                    }
                } else {
                    showFileCreateError(fileDirItem.path)
                    callback(null)
                }
            }
        }

        isAccessibleWithSAFSdk30(fileDirItem.path) -> {
            handleSAFDialogSdk30(fileDirItem.path) {
                if (!it) {
                    return@handleSAFDialogSdk30
                }

                callback.invoke(
                    try {
                        val uri = createDocumentUriUsingFirstParentTreeUri(fileDirItem.path)
                        if (!getDoesFilePathExist(fileDirItem.path)) {
                            createSAFFileSdk30(fileDirItem.path)
                        }
                        applicationContext.contentResolver.openOutputStream(uri, "wt")
                    } catch (e: Exception) {
                        null
                    } ?: createCasualFileOutputStream(this, targetFile)
                )
            }
        }

        isRestrictedWithSAFSdk30(fileDirItem.path) -> {
            callback.invoke(
                try {
                    val fileUri = getFileUrisFromFileDirItems(arrayListOf(fileDirItem))
                    applicationContext.contentResolver.openOutputStream(fileUri.first(), "wt")
                } catch (e: Exception) {
                    null
                } ?: createCasualFileOutputStream(this, targetFile)
            )
        }

        else -> {
            callback.invoke(createCasualFileOutputStream(this, targetFile))
        }
    }
}

fun Activity.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Activity.hideKeyboardSync() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    currentFocus?.clearFocus()
}

fun Activity.hideKeyboard() {
    if (isOnMainThread()) {
        hideKeyboardSync()
    } else {
        Handler(Looper.getMainLooper()).post {
            hideKeyboardSync()
        }
    }
}

fun Activity.addTracksToPlaylist(tracks: List<Track>, callback: () -> Unit) {
    SelectPlaylistDialog(this) { playlistId ->
        val tracksToAdd = ArrayList<Track>()
        tracks.forEach {
            it.id = 0
            it.playListId = playlistId
            tracksToAdd.add(it)
        }

        ensureBackgroundThread {
            RoomHelper(this).insertTracksWithPlaylist(tracksToAdd)

            runOnUiThread {
                callback()
            }
        }
    }
}

fun Activity.maybeRescanTrackPaths(tracks: List<Track>, callback: (tracks: List<Track>) -> Unit) {
    val tracksWithoutId =
        tracks.filter { it.mediaStoreId == 0L || (it.flags and FLAG_MANUAL_CACHE != 0) }
    if (tracksWithoutId.isNotEmpty()) {
        val pathsToRescan = tracksWithoutId.map { it.path }
        rescanPaths(pathsToRescan) {
            for (track in tracks) {
                if (track.mediaStoreId == 0L || (track.flags and FLAG_MANUAL_CACHE != 0)) {
                    track.mediaStoreId = getMediaStoreIdFromPath(track.path)
                }
            }

            callback(tracks)
        }
    } else {
        callback(tracks)
    }
}

fun Activity.showTrackProperties(selectedTracks: List<Track>) {
    val selectedPaths = selectedTracks.map { track ->
        track.path.ifEmpty {
            ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                track.mediaStoreId
            ).toString()
        }
    }

    if (selectedPaths.size <= 1) {
        PropertiesDialog(this, selectedPaths.first(), false)
    } else {
        PropertiesDialog(this, selectedPaths, false)
    }
}

fun Activity.ensureActivityNotDestroyed(callback: () -> Unit) {
    if (!isFinishing && !isDestroyed) {
        callback()
    }
}

fun Activity.shareFiles(tracks: List<Track>) {
    val paths = tracks.map { it.path }
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}


fun Activity.launchPurchaseThankYouIntent() {
    hideKeyboard()
    try {
        launchViewIntent("market://details?id=com.simplemobiletools.thankyou")
    } catch (ignored: Exception) {
        launchViewIntent(getString(R.string.thank_you_url))
    }
}

fun Activity.launchUpgradeToProIntent() {
    hideKeyboard()
    try {
        launchViewIntent("market://details?id=${baseConfig.appId.removeSuffix(".debug")}.pro")
    } catch (ignored: Exception) {
        launchViewIntent(getStoreUrl())
    }
}


fun Activity.launchViewIntent(id: Int) = launchViewIntent(getString(id))
