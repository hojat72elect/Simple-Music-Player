package com.simplemobiletools.musicplayer.helpers

import com.simplemobiletools.musicplayer.extensions.showErrorToast
import com.simplemobiletools.musicplayer.new_architecture.shared.SimpleActivity
import com.simplemobiletools.musicplayer.extensions.audioHelper
import com.simplemobiletools.musicplayer.models.Track
import java.io.File
import net.bjoernpetersen.m3u.M3uParser
import net.bjoernpetersen.m3u.model.M3uEntry

class M3uImporter(
    val activity: SimpleActivity,
    val callback: (result: ImportResult) -> Unit
) {
    private var failedEvents = 0
    private var exportedEvents = 0

    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    fun importPlaylist(path: String, playListId: Int) {
        val inputStream = if (path.contains("/")) {
            File(path).inputStream()
        } else {
            activity.assets.open(path)
        }

        try {
            val m3uEntries: List<M3uEntry> = M3uParser.parse(inputStream.reader())

            val existingTracks = activity.audioHelper.getAllTracks()
                .filter { it.playListId == 0 }

            val playlistItems = mutableListOf<Track>()
            for (m3uEntry in m3uEntries) {
                for (track in existingTracks) {
                    if (m3uEntry.location.toString() == track.path || m3uEntry.title == track.title) {
                        val copy = track.copy(id = 0, playListId = playListId)
                        playlistItems.add(copy)
                    }
                }
            }

            activity.audioHelper.insertTracks(playlistItems)
            exportedEvents = playlistItems.size
        } catch (e: Exception) {
            failedEvents++
            activity.showErrorToast(e)
        } finally {
            inputStream.close()
        }

        callback(
            when {
                exportedEvents == 0 -> ImportResult.IMPORT_FAIL
                failedEvents > 0 -> ImportResult.IMPORT_PARTIAL
                else -> ImportResult.IMPORT_OK
            }
        )
    }
}
