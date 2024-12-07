package com.simplemobiletools.musicplayer.new_architecture.home.player

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.media3.common.MediaItem
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.beGoneIf
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getProperPrimaryColor
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.viewBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.NavigationIcon
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.databinding.ActivityQueueBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.ui.dialogs.PlaylistDialog
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.currentMediaItemsShuffled
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.toTracks
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.toTrack
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.currentMediaItems
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.indexOfTrack
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.shuffledMediaItemsIndices
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.isReallyPlaying
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.lazySmoothScroll
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.RoomHelper
import com.simplemobiletools.musicplayer.new_architecture.shared.data.models.Track
import com.simplemobiletools.musicplayer.new_architecture.shared.SimpleControllerActivity

class QueueActivity : SimpleControllerActivity() {
    private var searchMenuItem: MenuItem? = null
    private var isSearchOpen = false
    private var tracksIgnoringSearch = ArrayList<Track>()

    private val binding by viewBinding(ActivityQueueBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        updateMaterialActivityViews(binding.queueCoordinator, binding.queueList, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.queueList, binding.queueToolbar)

        setupAdapter()
        binding.queueFastscroller.updateColors(getProperPrimaryColor())
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.queueToolbar, NavigationIcon.Arrow, searchMenuItem = searchMenuItem)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isSearchOpen && searchMenuItem != null) {
            searchMenuItem!!.collapseActionView()
        } else {
            super.onBackPressed()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        getAdapter()?.updateCurrentTrack()
    }

    private fun setupOptionsMenu() {
        setupSearch(binding.queueToolbar.menu)
        binding.queueToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.create_playlist_from_queue -> createPlaylistFromQueue()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        onSearchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                onSearchOpened()
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                onSearchClosed()
                isSearchOpen = false
                return true
            }
        })
    }

    private fun onSearchOpened() {
        val adapter = getAdapter() ?: return
        tracksIgnoringSearch = adapter.items
        adapter.updateItems(tracksIgnoringSearch, forceUpdate = true)
    }

    private fun onSearchClosed() {
        val adapter = getAdapter() ?: return
        adapter.updateItems(tracksIgnoringSearch, forceUpdate = true)
        binding.queuePlaceholder.beGoneIf(tracksIgnoringSearch.isNotEmpty())
    }

    private fun onSearchQueryChanged(text: String) {
        val filtered = tracksIgnoringSearch.filter { it.title.contains(text, true) }.toMutableList() as ArrayList<Track>
        getAdapter()?.updateItems(filtered, text)
        binding.queuePlaceholder.beGoneIf(filtered.isNotEmpty())
    }

    private fun getAdapter(): QueueAdapter? {
        return binding.queueList.adapter as? QueueAdapter
    }

    private fun setupAdapter() {
        if (getAdapter() == null) {
            withPlayer {
                val tracks = currentMediaItemsShuffled.toTracks().toMutableList() as ArrayList<Track>
                binding.queueList.adapter = QueueAdapter(
                    activity = this@QueueActivity,
                    items = tracks,
                    currentTrack = currentMediaItem?.toTrack(),
                    recyclerView = binding.queueList
                ) {
                    withPlayer {
                        val startIndex = currentMediaItems.indexOfTrack(it as Track)
                        seekTo(startIndex, 0)
                        if (!isReallyPlaying) {
                            play()
                        }
                    }
                }

                if (areSystemAnimationsEnabled) {
                    binding.queueList.scheduleLayoutAnimation()
                }

                val currentPosition = shuffledMediaItemsIndices.indexOf(currentMediaItemIndex)
                if (currentPosition > 0) {
                    binding.queueList.lazySmoothScroll(currentPosition)
                }
            }
        }
    }

    private fun createPlaylistFromQueue() {
        PlaylistDialog(this) { newPlaylistId ->
            val tracks = ArrayList<Track>()
            getAdapter()?.items?.forEach {
                it.playListId = newPlaylistId
                tracks.add(it)
            }

            ensureBackgroundThread {
                RoomHelper(this).insertTracksWithPlaylist(tracks)
            }
        }
    }
}
