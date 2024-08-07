package com.simplemobiletools.musicplayer.new_architecture.feature_albums

import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.musicplayer.dialogs.PermissionRequiredDialog
import com.simplemobiletools.musicplayer.extensions.hideKeyboard
import com.simplemobiletools.musicplayer.extensions.openNotificationSettings
import com.simplemobiletools.musicplayer.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.musicplayer.extensions.getFormattedDuration
import com.simplemobiletools.musicplayer.extensions.getProperPrimaryColor
import com.simplemobiletools.musicplayer.extensions.viewBinding
import com.simplemobiletools.musicplayer.helpers.NavigationIcon
import com.simplemobiletools.musicplayer.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.new_architecture.shared.SimpleMusicActivity
import com.simplemobiletools.musicplayer.new_architecture.feature_tracks.TracksActivity
import com.simplemobiletools.musicplayer.databinding.ActivityAlbumsBinding
import com.simplemobiletools.musicplayer.extensions.audioHelper
import com.simplemobiletools.musicplayer.helpers.ALBUM
import com.simplemobiletools.musicplayer.helpers.ARTIST
import com.simplemobiletools.musicplayer.models.Artist
import com.simplemobiletools.musicplayer.models.ListItem
import com.simplemobiletools.musicplayer.models.AlbumSection
import com.simplemobiletools.musicplayer.models.Album
import com.simplemobiletools.musicplayer.models.Track


class AlbumsActivity : SimpleMusicActivity() {

    private val binding by viewBinding(ActivityAlbumsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateMaterialActivityViews(binding.albumsCoordinator, binding.albumsHolder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.albumsList, binding.albumsToolbar)

        binding.albumsFastscroller.updateColors(getProperPrimaryColor())

        val artistType = object : TypeToken<Artist>() {}.type
        val artist = Gson().fromJson<Artist>(intent.getStringExtra(ARTIST), artistType)
        binding.albumsToolbar.title = artist.title

        ensureBackgroundThread {
            val albums = audioHelper.getArtistAlbums(artist.id)
            val listItems = ArrayList<ListItem>()
            val albumsSectionLabel = resources.getQuantityString(R.plurals.albums_plural, albums.size, albums.size)
            listItems.add(AlbumSection(albumsSectionLabel))
            listItems.addAll(albums)

            val albumTracks = audioHelper.getAlbumTracks(albums)
            val trackFullDuration = albumTracks.sumOf { it.duration }

            var tracksSectionLabel = resources.getQuantityString(R.plurals.tracks_plural, albumTracks.size, albumTracks.size)
            tracksSectionLabel += " • ${trackFullDuration.getFormattedDuration(true)}"
            listItems.add(AlbumSection(tracksSectionLabel))
            listItems.addAll(albumTracks)

            runOnUiThread {
                AlbumsTracksAdapter(this, listItems, binding.albumsList) {
                    hideKeyboard()
                    if (it is Album) {
                        Intent(this, TracksActivity::class.java).apply {
                            putExtra(ALBUM, Gson().toJson(it))
                            startActivity(this)
                        }
                    } else {
                        handleNotificationPermission { granted ->
                            if (granted) {
                                val startIndex = albumTracks.indexOf(it as Track)
                                prepareAndPlay(albumTracks, startIndex)
                            } else {
                                PermissionRequiredDialog(this,
                                    R.string.allow_notifications_music_player,
                                    { openNotificationSettings() })
                            }
                        }
                    }
                }.apply {
                    binding.albumsList.adapter = this
                }

                if (areSystemAnimationsEnabled) {
                    binding.albumsList.scheduleLayoutAnimation()
                }
            }
        }

        setupCurrentTrackBar(binding.currentTrackBar.root)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.albumsToolbar, NavigationIcon.Arrow)
    }
}
