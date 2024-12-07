package com.simplemobiletools.musicplayer.new_architecture.shared.extensions

import android.content.Context
import com.simplemobiletools.musicplayer.models.FileDirItem

fun FileDirItem.isRecycleBinPath(context: Context): Boolean {
    return path.startsWith(context.recycleBinPath)
}
