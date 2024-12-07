package com.simplemobiletools.musicplayer.new_architecture.shared.data.models

class Events {
    class SleepTimerChanged(val seconds: Int)
    class PlaylistsUpdated
    class RefreshFragments
    class RefreshTracks
}
