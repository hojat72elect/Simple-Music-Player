package ca.hojat.smart.musicplayer.feature_albums

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.databinding.ItemAlbumBinding
import ca.hojat.smart.musicplayer.databinding.ItemSectionBinding
import ca.hojat.smart.musicplayer.databinding.ItemTrackBinding
import ca.hojat.smart.musicplayer.shared.BaseSimpleActivity
import ca.hojat.smart.musicplayer.shared.data.models.Album
import ca.hojat.smart.musicplayer.shared.data.models.AlbumSection
import ca.hojat.smart.musicplayer.shared.data.models.ListItem
import ca.hojat.smart.musicplayer.shared.data.models.Track
import ca.hojat.smart.musicplayer.shared.extensions.audioHelper
import ca.hojat.smart.musicplayer.shared.extensions.beGone
import ca.hojat.smart.musicplayer.shared.extensions.beVisible
import ca.hojat.smart.musicplayer.shared.extensions.getAlbumCoverArt
import ca.hojat.smart.musicplayer.shared.extensions.getFormattedDuration
import ca.hojat.smart.musicplayer.shared.extensions.getTrackCoverArt
import ca.hojat.smart.musicplayer.shared.extensions.indexOfFirstOrNull
import ca.hojat.smart.musicplayer.shared.extensions.setupViewBackground
import ca.hojat.smart.musicplayer.shared.helpers.ensureBackgroundThread
import ca.hojat.smart.musicplayer.shared.ui.dialogs.ConfirmationDialog
import ca.hojat.smart.musicplayer.shared.ui.dialogs.EditDialog
import ca.hojat.smart.musicplayer.shared.ui.views.BaseMusicAdapter
import ca.hojat.smart.musicplayer.shared.ui.views.MyRecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller

// we show both albums and individual tracks here
class AlbumsTracksAdapter(
    activity: BaseSimpleActivity, items: ArrayList<ListItem>, recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : BaseMusicAdapter<ListItem>(items, activity, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    private val ITEM_SECTION = 0
    private val ITEM_ALBUM = 1
    private val ITEM_TRACK = 2

    override fun getActionMenuId() = R.menu.cab_albums_tracks

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {
            ITEM_SECTION -> ItemSectionBinding.inflate(layoutInflater, parent, false)
            ITEM_ALBUM -> ItemAlbumBinding.inflate(layoutInflater, parent, false)
            else -> ItemTrackBinding.inflate(layoutInflater, parent, false)
        }

        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return
        val allowClicks = item !is AlbumSection
        holder.bindView(item, allowClicks, allowClicks) { itemView, _ ->
            when (item) {
                is AlbumSection -> setupSection(itemView, item)
                is Album -> setupAlbum(itemView, item)
                else -> setupTrack(itemView, item as Track)
            }
        }
        bindViewHolder(holder)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AlbumSection -> ITEM_SECTION
            is Album -> ITEM_ALBUM
            else -> ITEM_TRACK
        }
    }

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_play_next).isVisible = shouldShowPlayNext()
            findItem(R.id.cab_rename).isVisible = shouldShowRename()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_add_to_playlist -> addToPlaylist()
            R.id.cab_add_to_queue -> addToQueue()
            R.id.cab_properties -> showProperties()
            R.id.cab_delete -> askConfirmDelete()
            R.id.cab_share -> shareFiles()
            R.id.cab_rename -> displayEditDialog()
            R.id.cab_select_all -> selectAll()
            R.id.cab_play_next -> playNextInQueue()
        }
    }

    override fun getSelectableItemCount() = items.filter { it !is AlbumSection }.size

    override fun getIsItemSelectable(position: Int) = items[position] !is AlbumSection

    private fun askConfirmDelete() {
        ConfirmationDialog(context) {
            ensureBackgroundThread {
                val positions = ArrayList<Int>()
                val selectedTracks = getAllSelectedTracks()
                val selectedAlbums = getSelectedAlbums()

                positions += selectedTracks.mapNotNull { track -> items.indexOfFirstOrNull { it is Track && it.mediaStoreId == track.mediaStoreId } }
                positions += selectedAlbums.mapNotNull { album -> items.indexOfFirstOrNull { it is Album && it.id == album.id } }

                context.deleteTracks(selectedTracks) {
                    context.runOnUiThread {
                        positions.sortDescending()
                        removeSelectedItems(positions)
                        positions.forEach {
                            items.removeAt(it)
                        }

                        // finish activity if all tracks are deleted
                        if (items.none { it is Track }) {
                            context.finish()
                        }
                    }
                }
            }
        }
    }

    override fun getAllSelectedTracks(): List<Track> {
        val tracks = getSelectedTracks().toMutableList()
        tracks.addAll(context.audioHelper.getAlbumTracks(getSelectedAlbums()))
        return tracks
    }

    private fun getSelectedAlbums(): List<Album> = getSelectedItems().filterIsInstance<Album>().toList()

    private fun setupAlbum(view: View, album: Album) {
        ItemAlbumBinding.bind(view).apply {
            root.setupViewBackground(context)
            albumFrame.isSelected = selectedKeys.contains(album.hashCode())
            albumTitle.text = album.title
            albumTitle.setTextColor(textColor)
            albumTracks.text = resources.getQuantityString(R.plurals.tracks_plural, album.trackCnt, album.trackCnt)
            albumTracks.setTextColor(textColor)

            context.getAlbumCoverArt(album) { coverArt ->
                loadImage(albumImage, coverArt, placeholderBig)
            }
        }
    }

    private fun setupTrack(view: View, track: Track) {
        ItemTrackBinding.bind(view).apply {
            root.setupViewBackground(context)
            trackFrame.isSelected = selectedKeys.contains(track.hashCode())
            trackTitle.text = track.title
            trackTitle.setTextColor(textColor)
            trackInfo.text = track.album
            trackInfo.setTextColor(textColor)

            trackId.beGone()
            trackImage.beVisible()
            trackDuration.text = track.duration.getFormattedDuration()
            trackDuration.setTextColor(textColor)

            context.getTrackCoverArt(track) { coverArt ->
                loadImage(trackImage, coverArt, placeholder)
            }
        }
    }

    private fun setupSection(view: View, section: AlbumSection) {
        ItemSectionBinding.bind(view).apply {
            itemSection.text = section.title
            itemSection.setTextColor(textColor)
        }
    }

    override fun onChange(position: Int): CharSequence {
        return when (val listItem = items.getOrNull(position)) {
            is Track -> listItem.title
            is Album -> listItem.title
            is AlbumSection -> listItem.title
            else -> ""
        }
    }

    private fun displayEditDialog() {
        getSelectedTracks().firstOrNull()?.let { selectedTrack ->
            EditDialog(context as BaseSimpleActivity, selectedTrack) { track ->
                val trackIndex = items.indexOfFirst { (it as? Track)?.mediaStoreId == track.mediaStoreId }
                if (trackIndex != -1) {
                    items[trackIndex] = track
                    notifyItemChanged(trackIndex)
                    finishActMode()
                }

                context.refreshQueueAndTracks(track)
            }
        }
    }
}
