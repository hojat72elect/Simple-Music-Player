package ca.hojat.smart.musicplayer.feature_tracks

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.shared.ui.views.BaseMusicAdapter
import ca.hojat.smart.musicplayer.databinding.ItemTrackBinding
import ca.hojat.smart.musicplayer.shared.ui.dialogs.ConfirmationDialog
import ca.hojat.smart.musicplayer.shared.ui.dialogs.EditDialog
import ca.hojat.smart.musicplayer.shared.extensions.applyColorFilter
import ca.hojat.smart.musicplayer.shared.extensions.audioHelper
import ca.hojat.smart.musicplayer.shared.extensions.beGone
import ca.hojat.smart.musicplayer.shared.extensions.beVisible
import ca.hojat.smart.musicplayer.shared.extensions.beVisibleIf
import ca.hojat.smart.musicplayer.shared.extensions.config
import ca.hojat.smart.musicplayer.shared.extensions.getFormattedDuration
import ca.hojat.smart.musicplayer.shared.extensions.getTrackCoverArt
import ca.hojat.smart.musicplayer.shared.extensions.highlightTextPart
import ca.hojat.smart.musicplayer.shared.extensions.setupViewBackground
import ca.hojat.smart.musicplayer.shared.extensions.swap
import ca.hojat.smart.musicplayer.shared.helpers.ALL_TRACKS_PLAYLIST_ID
import ca.hojat.smart.musicplayer.shared.helpers.PLAYER_SORT_BY_CUSTOM
import ca.hojat.smart.musicplayer.shared.helpers.ensureBackgroundThread
import ca.hojat.smart.musicplayer.shared.extensions.indexOfFirstOrNull
import ca.hojat.smart.musicplayer.shared.data.ItemMoveCallback
import ca.hojat.smart.musicplayer.shared.data.ItemTouchHelperContract
import ca.hojat.smart.musicplayer.shared.data.StartReorderDragListener
import ca.hojat.smart.musicplayer.shared.data.models.Events
import ca.hojat.smart.musicplayer.shared.data.models.Playlist
import ca.hojat.smart.musicplayer.shared.data.models.Track
import ca.hojat.smart.musicplayer.shared.BaseSimpleActivity
import ca.hojat.smart.musicplayer.shared.ui.views.MyRecyclerView
import org.greenrobot.eventbus.EventBus

class TracksAdapter(
    activity: BaseSimpleActivity,
    recyclerView: MyRecyclerView,
    val sourceType: Int,
    val folder: String? = null,
    val playlist: Playlist? = null,
    items: ArrayList<Track>,
    itemClick: (Any) -> Unit
) : BaseMusicAdapter<Track>(items, activity, recyclerView, itemClick),
    RecyclerViewFastScroller.OnPopupTextUpdate, ItemTouchHelperContract {

    private var touchHelper: ItemTouchHelper? = null
    private var startReorderDragListener: StartReorderDragListener

    init {
        touchHelper = ItemTouchHelper(ItemMoveCallback(this))
        touchHelper!!.attachToRecyclerView(recyclerView)

        startReorderDragListener = object : StartReorderDragListener {
            override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                touchHelper?.startDrag(viewHolder)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_tracks

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrackBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = items.getOrNull(position) ?: return
        holder.bindView(track, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, track, holder)
        }
        bindViewHolder(holder)
    }

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_remove_from_playlist).isVisible = isPlaylistContent()
            findItem(R.id.cab_rename).isVisible = shouldShowRename()
            findItem(R.id.cab_play_next).isVisible = shouldShowPlayNext()
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
            R.id.cab_rename -> displayEditDialog()
            R.id.cab_remove_from_playlist -> removeFromPlaylist()
            R.id.cab_delete -> askConfirmDelete()
            R.id.cab_share -> shareFiles()
            R.id.cab_select_all -> selectAll()
            R.id.cab_play_next -> playNextInQueue()
        }
    }

    override fun onActionModeCreated() {
        if (isPlaylistContent()) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun onActionModeDestroyed() {
        if (isPlaylistContent()) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    private fun removeFromPlaylist() {
        ensureBackgroundThread {
            val positions = ArrayList<Int>()
            val selectedTracks = getSelectedTracks()
            selectedTracks.forEach { track ->
                val position = items.indexOfFirst { it.mediaStoreId == track.mediaStoreId }
                if (position != -1) {
                    positions.add(position)
                }
            }

            context.audioHelper.deleteTracks(selectedTracks)
            // this is to make sure these tracks aren't automatically re-added to the 'All tracks' playlist on rescan
            val removedTrackIds = selectedTracks.filter { it.playListId == ALL_TRACKS_PLAYLIST_ID }
                .map { it.mediaStoreId.toString() }
            if (removedTrackIds.isNotEmpty()) {
                val config = context.config
                config.tracksRemovedFromAllTracksPlaylist =
                    config.tracksRemovedFromAllTracksPlaylist.apply {
                        addAll(removedTrackIds)
                    }
            }

            EventBus.getDefault().post(Events.PlaylistsUpdated())
            context.runOnUiThread {
                positions.sortDescending()
                removeSelectedItems(positions)
                positions.forEach {
                    items.removeAt(it)
                }
            }
        }
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(context) {
            ensureBackgroundThread {
                val positions = ArrayList<Int>()
                val selectedTracks = getSelectedTracks()
                selectedTracks.forEach { track ->
                    val position = items.indexOfFirst { it.mediaStoreId == track.mediaStoreId }
                    if (position != -1) {
                        positions.add(position)
                    }
                }

                context.deleteTracks(selectedTracks) {
                    context.runOnUiThread {
                        positions.sortDescending()
                        removeSelectedItems(positions)
                        positions.forEach {
                            if (items.size > it) {
                                items.removeAt(it)
                            }
                        }

                        finishActMode()

                        // finish activity if all tracks are deleted
                        if (items.isEmpty() && !isPlaylistContent()) {
                            context.finish()
                        }
                    }
                }
            }
        }
    }

    override fun getSelectedTracks(): List<Track> =
        items.filter { selectedKeys.contains(it.hashCode()) }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView(view: View, track: Track, holder: ViewHolder) {
        ItemTrackBinding.bind(view).apply {
            root.setupViewBackground(context)
            trackFrame.isSelected = selectedKeys.contains(track.hashCode())
            trackTitle.text =
                if (textToHighlight.isEmpty()) track.title else track.title.highlightTextPart(
                    textToHighlight,
                    properPrimaryColor
                )
            trackInfo.text = if (textToHighlight.isEmpty()) {
                "${track.artist} • ${track.album}"
            } else {
                ("${track.artist} • ${track.album}").highlightTextPart(
                    textToHighlight,
                    properPrimaryColor
                )
            }
            trackDragHandle.beVisibleIf(isPlaylistContent() && selectedKeys.isNotEmpty())
            trackDragHandle.applyColorFilter(textColor)
            trackDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startReorderDragListener.requestDrag(holder)
                }
                false
            }

            arrayOf(trackId, trackTitle, trackInfo, trackDuration).forEach {
                it.setTextColor(textColor)
            }

            trackDuration.text = track.duration.getFormattedDuration()
            activity.getTrackCoverArt(track) { coverArt ->
                loadImage(trackImage, coverArt, placeholderBig)
            }

            trackImage.beVisible()
            trackId.beGone()
        }
    }

    override fun onChange(position: Int): String {
        val sorting = if (isPlaylistContent() && playlist != null) {
            context.config.getProperPlaylistSorting(playlist.id)
        } else if (sourceType == TYPE_FOLDER && folder != null) {
            context.config.getProperFolderSorting(folder)
        } else {
            context.config.trackSorting
        }

        return items.getOrNull(position)?.getBubbleText(sorting) ?: ""
    }

    private fun displayEditDialog() {
        getSelectedTracks().firstOrNull()?.let { selectedTrack ->
            EditDialog(context, selectedTrack) { track ->
                val trackIndex = items.indexOfFirstOrNull { it.mediaStoreId == track.mediaStoreId }
                    ?: return@EditDialog
                items[trackIndex] = track
                notifyItemChanged(trackIndex)
                finishActMode()

                context.refreshQueueAndTracks(track)
            }
        }
    }

    override fun onRowClear(myViewHolder: ViewHolder?) {
        ensureBackgroundThread {
            var index = 0
            items.forEach {
                it.orderInPlaylist = index++
                context.audioHelper.updateOrderInPlaylist(index, it.id)
            }
        }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        context.config.saveCustomPlaylistSorting(playlist!!.id, PLAYER_SORT_BY_CUSTOM)
        items.swap(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {}


    private fun isPlaylistContent() = sourceType == TYPE_PLAYLIST

    companion object {
        const val TYPE_PLAYLIST = 1
        const val TYPE_FOLDER = 2
        const val TYPE_ALBUM = 3
        const val TYPE_TRACKS = 4
    }
}