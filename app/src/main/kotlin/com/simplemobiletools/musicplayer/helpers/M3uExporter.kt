package com.simplemobiletools.musicplayer.helpers

import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.new_architecture.shared.BaseSimpleActivity
import com.simplemobiletools.musicplayer.extensions.showErrorToast
import com.simplemobiletools.musicplayer.extensions.toast
import com.simplemobiletools.musicplayer.extensions.writeLn
import com.simplemobiletools.musicplayer.models.Track
import java.io.OutputStream

class M3uExporter(val activity: BaseSimpleActivity) {
    private var failedEvents = 0
    private var exportedEvents = 0

    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    fun exportPlaylist(
        outputStream: OutputStream?,
        tracks: ArrayList<Track>,
        callback: (result: ExportResult) -> Unit
    ) {
        if (outputStream == null) {
            callback(ExportResult.EXPORT_FAIL)
            return
        }

        activity.toast(R.string.exporting)

        try {
            outputStream.bufferedWriter().use { out ->
                out.writeLn(M3U_HEADER)
                for (track in tracks) {
                    out.writeLn(M3U_ENTRY + track.duration + M3U_DURATION_SEPARATOR + track.artist + " - " + track.title)
                    out.writeLn(track.path)
                    exportedEvents++
                }
            }
        } catch (e: Exception) {
            failedEvents++
            activity.showErrorToast(e)
        } finally {
            outputStream.close()
        }

        callback(
            when {
                exportedEvents == 0 -> ExportResult.EXPORT_FAIL
                failedEvents > 0 -> ExportResult.EXPORT_PARTIAL
                else -> ExportResult.EXPORT_OK
            }
        )
    }
}
