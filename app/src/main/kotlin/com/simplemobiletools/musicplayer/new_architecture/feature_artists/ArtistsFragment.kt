package com.simplemobiletools.musicplayer.new_architecture.feature_artists

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.google.gson.Gson
import com.simplemobiletools.musicplayer.new_architecture.shared.BaseSimpleActivity
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.beGoneIf
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.beVisibleIf
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.hideKeyboard
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.new_architecture.feature_albums.AlbumsActivity
import com.simplemobiletools.musicplayer.databinding.FragmentArtistsBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.ui.dialogs.ChangeSortingDialog
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.audioHelper
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.config
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.mediaScanner
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.viewBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.MyViewPagerFragment
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.ARTIST
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.TAB_ARTISTS
import com.simplemobiletools.musicplayer.new_architecture.shared.data.models.Artist
import com.simplemobiletools.musicplayer.new_architecture.shared.data.models.sortSafely

// Artists -> Albums -> Tracks
class ArtistsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    private var artists = ArrayList<Artist>()
    private val binding by viewBinding(FragmentArtistsBinding::bind)

    override fun setupFragment(activity: BaseSimpleActivity) {
        ensureBackgroundThread {
            val cachedArtists = activity.audioHelper.getAllArtists()
            activity.runOnUiThread {
                gotArtists(activity, cachedArtists)
            }
        }
    }

    private fun gotArtists(activity: BaseSimpleActivity, cachedArtists: ArrayList<Artist>) {
        artists = cachedArtists
        activity.runOnUiThread {
            val scanning = activity.mediaScanner.isScanning()
            binding.artistsPlaceholder.text = if (scanning) {
                context.getString(R.string.loading_files)
            } else {
                context.getString(R.string.no_items_found)
            }
            binding.artistsPlaceholder.beVisibleIf(artists.isEmpty())

            val adapter = binding.artistsList.adapter
            if (adapter == null) {
                ArtistsAdapter(activity, artists, binding.artistsList) {
                    activity.hideKeyboard()
                    Intent(activity, AlbumsActivity::class.java).apply {
                        putExtra(ARTIST, Gson().toJson(it as Artist))
                        activity.startActivity(this)
                    }
                }.apply {
                    binding.artistsList.adapter = this
                }

                if (context.areSystemAnimationsEnabled) {
                    binding.artistsList.scheduleLayoutAnimation()
                }
            } else {
                val oldItems = (adapter as ArtistsAdapter).items
                if (oldItems.sortedBy { it.id }.hashCode() != artists.sortedBy { it.id }.hashCode()) {
                    adapter.updateItems(artists)
                }
            }
        }
    }

    override fun finishActMode() {
        getAdapter()?.finishActMode()
    }

    override fun onSearchQueryChanged(text: String) {
        val filtered = artists.filter { it.title.contains(text, true) }.toMutableList() as ArrayList<Artist>
        getAdapter()?.updateItems(filtered, text)
        binding.artistsPlaceholder.beVisibleIf(filtered.isEmpty())
    }

    override fun onSearchClosed() {
        getAdapter()?.updateItems(artists)
        binding.artistsPlaceholder.beGoneIf(artists.isNotEmpty())
    }

    override fun onSortOpen(activity: BaseSimpleActivity) {
        ChangeSortingDialog(activity, TAB_ARTISTS) {
            val adapter = getAdapter() ?: return@ChangeSortingDialog
            artists.sortSafely(activity.config.artistSorting)
            adapter.updateItems(artists, forceUpdate = true)
        }
    }

    override fun setupColors(textColor: Int, adjustedPrimaryColor: Int) {
        binding.artistsPlaceholder.setTextColor(textColor)
        binding.artistsFastscroller.updateColors(adjustedPrimaryColor)
        getAdapter()?.updateColors(textColor)
    }

    private fun getAdapter() = binding.artistsList.adapter as? ArtistsAdapter
}
