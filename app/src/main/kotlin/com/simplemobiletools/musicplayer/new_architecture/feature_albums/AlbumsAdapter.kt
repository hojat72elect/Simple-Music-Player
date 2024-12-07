package com.simplemobiletools.musicplayer.new_architecture.feature_albums

import android.view.View
import android.view.ViewGroup
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import com.simplemobiletools.musicplayer.new_architecture.shared.BaseSimpleActivity
import com.simplemobiletools.musicplayer.new_architecture.shared.ui.dialogs.ConfirmationDialog
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.highlightTextPart
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.setupViewBackground
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.views.MyRecyclerView
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.views.BaseMusicAdapter
import com.simplemobiletools.musicplayer.databinding.ItemAlbumBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.audioHelper
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.config
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getAlbumCoverArt
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.indexOfFirstOrNull
import com.simplemobiletools.musicplayer.models.Album
import com.simplemobiletools.musicplayer.models.Track

class AlbumsAdapter(activity: BaseSimpleActivity, items: ArrayList<Album>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    BaseMusicAdapter<Album>(items, activity, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    override fun getActionMenuId() = R.menu.cab_albums

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = items.getOrNull(position) ?: return
        holder.bindView(album, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, album)
        }
        bindViewHolder(holder)
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_add_to_playlist -> addToPlaylist()
            R.id.cab_add_to_queue -> addToQueue()
            R.id.cab_delete -> askConfirmDelete()
            R.id.cab_share -> shareFiles()
            R.id.cab_select_all -> selectAll()
        }
    }

    override fun getSelectedTracks(): List<Track> {
        return context.audioHelper.getAlbumTracks(getSelectedItems())
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(context) {
            ensureBackgroundThread {
                val selectedAlbums = getSelectedItems()
                val positions = selectedAlbums.mapNotNull { album -> items.indexOfFirstOrNull { it.id == album.id } } as ArrayList<Int>
                val tracks = context.audioHelper.getAlbumTracks(selectedAlbums)
                context.audioHelper.deleteAlbums(selectedAlbums)

                context.deleteTracks(tracks) {
                    context.runOnUiThread {
                        positions.sortDescending()
                        removeSelectedItems(positions)
                        positions.forEach {
                            if (items.size > it) {
                                items.removeAt(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupView(view: View, album: Album) {
        ItemAlbumBinding.bind(view).apply {
            root.setupViewBackground(context)
            albumFrame.isSelected = selectedKeys.contains(album.hashCode())
            albumTitle.text = if (textToHighlight.isEmpty()) album.title else album.title.highlightTextPart(textToHighlight, properPrimaryColor)
            albumTitle.setTextColor(textColor)

            val tracks = resources.getQuantityString(R.plurals.tracks_plural, album.trackCnt, album.trackCnt)
            albumTracks.text = tracks
            albumTracks.setTextColor(textColor)

            context.getAlbumCoverArt(album) { coverArt ->
                loadImage(albumImage, coverArt, placeholderBig)
            }
        }
    }

    override fun onChange(position: Int) = items.getOrNull(position)?.getBubbleText(context.config.albumSorting) ?: ""
}
