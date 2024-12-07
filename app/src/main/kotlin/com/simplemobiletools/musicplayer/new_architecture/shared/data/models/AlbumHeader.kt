package com.simplemobiletools.musicplayer.new_architecture.shared.data.models

data class AlbumHeader(val id: Long, val title: String, val coverArt: Any, val year: Int, val trackCnt: Int, val duration: Int, val artist: String) : ListItem()
