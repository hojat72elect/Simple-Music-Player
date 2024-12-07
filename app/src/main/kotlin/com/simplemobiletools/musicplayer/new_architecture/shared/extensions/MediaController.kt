package com.simplemobiletools.musicplayer.new_architecture.shared.extensions

import android.os.Bundle
import androidx.media3.session.MediaController
import com.simplemobiletools.musicplayer.new_architecture.shared.playback.CustomCommands

fun MediaController.sendCommand(command: CustomCommands, extras: Bundle = Bundle.EMPTY) = sendCustomCommand(command.sessionCommand, extras)
