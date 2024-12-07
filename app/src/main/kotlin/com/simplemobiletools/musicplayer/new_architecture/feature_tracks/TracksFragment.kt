package com.simplemobiletools.musicplayer.new_architecture.feature_tracks

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.simplemobiletools.musicplayer.new_architecture.shared.ui.dialogs.PermissionRequiredDialog
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.new_architecture.shared.BaseSimpleActivity
import com.simplemobiletools.musicplayer.databinding.FragmentTracksBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.ui.dialogs.ChangeSortingDialog
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.audioHelper
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.beGoneIf
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.beVisibleIf
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.config
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getParentPath
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.hideKeyboard
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.mediaScanner
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.openNotificationSettings
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.viewBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.MyViewPagerFragment
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.TAB_TRACKS
import com.simplemobiletools.musicplayer.models.Track
import com.simplemobiletools.musicplayer.models.sortSafely

// Artists -> Albums -> Tracks
class TracksFragment(context: Context, attributeSet: AttributeSet) :
    MyViewPagerFragment(context, attributeSet) {
    private var tracks = ArrayList<Track>()
    private val binding by viewBinding(FragmentTracksBinding::bind)

    override fun setupFragment(activity: BaseSimpleActivity) {
        ensureBackgroundThread {
            tracks = context.audioHelper.getAllTracks()

            val excludedFolders = context.config.excludedFolders
            tracks = tracks.filter {
                !excludedFolders.contains(it.path.getParentPath())
            }.toMutableList() as ArrayList<Track>

            activity.runOnUiThread {
                val scanning = activity.mediaScanner.isScanning()
                binding.tracksPlaceholder.text = if (scanning) {
                    context.getString(R.string.loading_files)
                } else {
                    context.getString(R.string.no_items_found)
                }
                binding.tracksPlaceholder.beVisibleIf(tracks.isEmpty())
                val adapter = binding.tracksList.adapter
                if (adapter == null) {
                    TracksAdapter(
                        activity = activity,
                        recyclerView = binding.tracksList,
                        sourceType = TracksAdapter.TYPE_TRACKS,
                        items = tracks
                    ) {
                        activity.hideKeyboard()
                        activity.handleNotificationPermission { granted ->
                            if (granted) {
                                val startIndex = tracks.indexOf(it as Track)
                                prepareAndPlay(tracks, startIndex)
                            } else {
                                if (context is Activity) {
                                    PermissionRequiredDialog(
                                        activity,
                                        R.string.allow_notifications_music_player,
                                        { activity.openNotificationSettings() }
                                    )
                                }
                            }
                        }
                    }.apply {
                        binding.tracksList.adapter = this
                    }

                    if (context.areSystemAnimationsEnabled) {
                        binding.tracksList.scheduleLayoutAnimation()
                    }
                } else {
                    (adapter as TracksAdapter).updateItems(tracks)
                }
            }
        }
    }

    override fun finishActMode() {
        getAdapter()?.finishActMode()
    }

    override fun onSearchQueryChanged(text: String) {
        val filtered = tracks.filter {
            it.title.contains(text, true) || ("${it.artist} - ${it.album}").contains(text, true)
        }.toMutableList() as ArrayList<Track>
        getAdapter()?.updateItems(filtered, text)
        binding.tracksPlaceholder.beVisibleIf(filtered.isEmpty())
    }

    override fun onSearchClosed() {
        getAdapter()?.updateItems(tracks)
        binding.tracksPlaceholder.beGoneIf(tracks.isNotEmpty())
    }

    override fun onSortOpen(activity: BaseSimpleActivity) {
        ChangeSortingDialog(activity, TAB_TRACKS) {
            val adapter = getAdapter() ?: return@ChangeSortingDialog
            tracks.sortSafely(activity.config.trackSorting)
            adapter.updateItems(tracks, forceUpdate = true)
        }
    }

    override fun setupColors(textColor: Int, adjustedPrimaryColor: Int) {
        binding.tracksPlaceholder.setTextColor(textColor)
        binding.tracksFastscroller.updateColors(adjustedPrimaryColor)
        getAdapter()?.updateColors(textColor)
    }

    private fun getAdapter() = binding.tracksList.adapter as? TracksAdapter
}
