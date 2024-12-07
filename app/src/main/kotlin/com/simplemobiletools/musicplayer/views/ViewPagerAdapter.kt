package com.simplemobiletools.musicplayer.views

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.simplemobiletools.musicplayer.databinding.FragmentAlbumsBinding
import com.simplemobiletools.musicplayer.databinding.FragmentArtistsBinding
import com.simplemobiletools.musicplayer.databinding.FragmentFoldersBinding
import com.simplemobiletools.musicplayer.databinding.FragmentGenresBinding
import com.simplemobiletools.musicplayer.databinding.FragmentPlaylistsBinding
import com.simplemobiletools.musicplayer.databinding.FragmentTracksBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getProperPrimaryColor
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getProperTextColor
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getVisibleTabs
import com.simplemobiletools.musicplayer.new_architecture.shared.MyViewPagerFragment
import com.simplemobiletools.musicplayer.helpers.TAB_ALBUMS
import com.simplemobiletools.musicplayer.helpers.TAB_ARTISTS
import com.simplemobiletools.musicplayer.helpers.TAB_FOLDERS
import com.simplemobiletools.musicplayer.helpers.TAB_GENRES
import com.simplemobiletools.musicplayer.helpers.TAB_PLAYLISTS
import com.simplemobiletools.musicplayer.helpers.TAB_TRACKS
import com.simplemobiletools.musicplayer.new_architecture.feature_playlists.PlaylistsFragment
import com.simplemobiletools.musicplayer.new_architecture.feature_tracks.TracksFragment
import com.simplemobiletools.musicplayer.new_architecture.shared.BaseSimpleActivity


class ViewPagerAdapter(val activity: BaseSimpleActivity) : PagerAdapter() {
    private val fragments = arrayListOf<MyViewPagerFragment>()
    private var primaryItem: MyViewPagerFragment? = null

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return getFragment(position, container).apply {
            fragments.add(this)
            container.addView(this)
            setupFragment(activity)
            setupColors(activity.getProperTextColor(), activity.getProperPrimaryColor())
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        fragments.remove(item)
        container.removeView(item as View)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        primaryItem = `object` as MyViewPagerFragment
    }

    override fun getCount() = activity.getVisibleTabs().size

    override fun isViewFromObject(view: View, item: Any) = view == item

    private fun getFragment(position: Int, container: ViewGroup): MyViewPagerFragment {
        val tab = activity.getVisibleTabs()[position]
        val layoutInflater = activity.layoutInflater
        return when (tab) {
            TAB_PLAYLISTS -> FragmentPlaylistsBinding.inflate(layoutInflater, container, false).root
            TAB_FOLDERS -> FragmentFoldersBinding.inflate(layoutInflater, container, false).root
            TAB_ARTISTS -> FragmentArtistsBinding.inflate(layoutInflater, container, false).root
            TAB_ALBUMS -> FragmentAlbumsBinding.inflate(layoutInflater, container, false).root
            TAB_TRACKS -> FragmentTracksBinding.inflate(layoutInflater, container, false).root
            TAB_GENRES -> FragmentGenresBinding.inflate(layoutInflater, container, false).root
            else -> throw IllegalArgumentException("Unknown tab: $tab")
        }
    }

    fun getAllFragments() = fragments

    fun getCurrentFragment() = primaryItem

    fun getPlaylistsFragment() = fragments.find { it is PlaylistsFragment }

    fun getTracksFragment() = fragments.find { it is TracksFragment }
}
