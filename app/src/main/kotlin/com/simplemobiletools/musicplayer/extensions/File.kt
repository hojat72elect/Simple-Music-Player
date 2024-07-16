package com.simplemobiletools.musicplayer.extensions

import android.content.Context
import com.simplemobiletools.commons.helpers.NOMEDIA
import com.simplemobiletools.commons.helpers.audioExtensions
import com.simplemobiletools.commons.helpers.photoExtensions
import com.simplemobiletools.commons.helpers.rawExtensions
import com.simplemobiletools.commons.helpers.videoExtensions
import com.simplemobiletools.musicplayer.models.FileDirItem
import java.io.File

fun File.toFileDirItem(context: Context) = FileDirItem(absolutePath, name, context.getIsPathDirectory(absolutePath), 0, length(), lastModified())
fun File.containsNoMedia(): Boolean {
    return if (!isDirectory) {
        false
    } else {
        File(this, NOMEDIA).exists()
    }
}
fun File.isMediaFile() = absolutePath.isMediaFile()
fun File.isGif() = absolutePath.endsWith(".gif", true)
fun File.isApng() = absolutePath.endsWith(".apng", true)
fun File.isVideoFast() = videoExtensions.any { absolutePath.endsWith(it, true) }
fun File.isImageFast() = photoExtensions.any { absolutePath.endsWith(it, true) }
fun File.isAudioFast() = audioExtensions.any { absolutePath.endsWith(it, true) }
fun File.isRawFast() = rawExtensions.any { absolutePath.endsWith(it, true) }
fun File.isSvg() = absolutePath.isSvg()
fun File.isPortrait() = absolutePath.isPortrait()
fun File.isImageSlow() = absolutePath.isImageFast() || getMimeType().startsWith("image")
fun File.isVideoSlow() = absolutePath.isVideoFast() || getMimeType().startsWith("video")
fun File.isAudioSlow() = absolutePath.isAudioFast() || getMimeType().startsWith("audio")
fun File.getMimeType() = absolutePath.getMimeType()

