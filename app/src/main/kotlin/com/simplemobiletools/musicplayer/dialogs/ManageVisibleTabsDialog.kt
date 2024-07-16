package com.simplemobiletools.musicplayer.dialogs

import com.simplemobiletools.commons.helpers.isQPlus
import com.simplemobiletools.commons.views.MyAppCompatCheckbox
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.activities.BaseSimpleActivity
import com.simplemobiletools.musicplayer.databinding.DialogManageVisibleTabsBinding
import com.simplemobiletools.musicplayer.extensions.beGone
import com.simplemobiletools.musicplayer.extensions.config
import com.simplemobiletools.musicplayer.extensions.getAlertDialogBuilder
import com.simplemobiletools.musicplayer.extensions.setupDialogStuff
import com.simplemobiletools.musicplayer.extensions.viewBinding
import com.simplemobiletools.musicplayer.helpers.TAB_ALBUMS
import com.simplemobiletools.musicplayer.helpers.TAB_ARTISTS
import com.simplemobiletools.musicplayer.helpers.TAB_FOLDERS
import com.simplemobiletools.musicplayer.helpers.TAB_GENRES
import com.simplemobiletools.musicplayer.helpers.TAB_PLAYLISTS
import com.simplemobiletools.musicplayer.helpers.TAB_TRACKS
import com.simplemobiletools.musicplayer.helpers.allTabsMask


class ManageVisibleTabsDialog(
    val activity: BaseSimpleActivity,
    val callback: (result: Int) -> Unit
) {
    private val binding by activity.viewBinding(DialogManageVisibleTabsBinding::inflate)
    private val tabs = LinkedHashMap<Int, MyAppCompatCheckbox>()

    init {
        tabs.apply {
            put(TAB_PLAYLISTS, binding.manageVisibleTabsPlaylists)
            put(TAB_FOLDERS, binding.manageVisibleTabsFolders)
            put(TAB_ARTISTS, binding.manageVisibleTabsArtists)
            put(TAB_ALBUMS, binding.manageVisibleTabsAlbums)
            put(TAB_TRACKS, binding.manageVisibleTabsTracks)
            put(TAB_GENRES, binding.manageVisibleTabsGenres)
        }

        if (!isQPlus()) {
            tabs.remove(TAB_FOLDERS)
            binding.manageVisibleTabsFolders.beGone()
        }

        val showTabs = activity.config.showTabs
        for ((key, value) in tabs) {
            value.isChecked = showTabs and key != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        for ((key, value) in tabs) {
            if (value.isChecked) {
                result += key
            }
        }

        if (result == 0) {
            result = allTabsMask
        }

        callback(result)
    }
}
