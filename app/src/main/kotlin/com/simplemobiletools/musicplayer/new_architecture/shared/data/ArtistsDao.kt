package com.simplemobiletools.musicplayer.new_architecture.shared.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.musicplayer.new_architecture.shared.data.models.Artist

@Dao
interface ArtistsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artist: Artist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(artists: List<Artist>)

    @Query("SELECT * FROM artists")
    fun getAll(): List<Artist>

    @Query("DELETE FROM artists WHERE id = :id")
    fun deleteArtist(id: Long)
}
