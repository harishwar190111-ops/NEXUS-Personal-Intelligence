package com.nexus.personalintelligence.tv.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import com.nexus.personalintelligence.core.remote.CommandResult
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import com.nexus.personalintelligence.tv.data.AppRepository
import com.nexus.personalintelligence.tv.data.LaunchResult

class TvCommandDispatcher(
    private val context: Context,
    private val appRepo: AppRepository = AppRepository(context),
    private val onTextReceived: ((String) -> Unit)? = null
) {
    suspend fun dispatch(command: RemoteCommand): CommandResult {
        return try {
            when (command.type) {
                RemoteCommandType.LAUNCH_APP -> {
                    val pkg = command.packageName
                    if (pkg.isNullOrBlank()) {
                        CommandResult(command.commandId, false, "Missing packageName in LAUNCH_APP")
                    } else {
                        when (val res = appRepo.launch(pkg)) {
                            is LaunchResult.Started -> CommandResult(command.commandId, true, "Launched $pkg")
                            is LaunchResult.Failed -> CommandResult(command.commandId, false, "Launch failed: ${res.reason}")
                        }
                    }
                }
                RemoteCommandType.OPEN_URL -> {
                    val url = command.targetUrl
                    if (url.isNullOrBlank()) {
                        CommandResult(command.commandId, false, "Missing targetUrl in OPEN_URL")
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        CommandResult(command.commandId, true, "Opened $url")
                    }
                }
                RemoteCommandType.TEXT_INPUT -> {
                    val text = command.text.orEmpty()
                    onTextReceived?.invoke(text)
                    CommandResult(command.commandId, true, "Received text input ($text)")
                }
                RemoteCommandType.BACKSPACE -> {
                    onTextReceived?.invoke("\b")
                    CommandResult(command.commandId, true, "Received backspace")
                }
                RemoteCommandType.ENTER -> {
                    onTextReceived?.invoke("\n")
                    CommandResult(command.commandId, true, "Received enter")
                }
                else -> {
                    // D-pad, media, volume, navigation commands
                    val keycode = toKeyCode(command.type)
                    CommandResult(
                        commandId = command.commandId,
                        success = true,
                        message = "Command ${command.type} acknowledged (key: $keycode)"
                    )
                }
            }
        } catch (e: Exception) {
            CommandResult(
                commandId = command.commandId,
                success = false,
                message = "Execution failed: ${e.message}"
            )
        }
    }

    companion object {
        fun toKeyCode(type: RemoteCommandType): Int = when (type) {
            RemoteCommandType.DPAD_UP -> KeyEvent.KEYCODE_DPAD_UP
            RemoteCommandType.DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
            RemoteCommandType.DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
            RemoteCommandType.DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
            RemoteCommandType.DPAD_CENTER -> KeyEvent.KEYCODE_DPAD_CENTER
            RemoteCommandType.BACK -> KeyEvent.KEYCODE_BACK
            RemoteCommandType.HOME -> KeyEvent.KEYCODE_HOME
            RemoteCommandType.MENU -> KeyEvent.KEYCODE_MENU
            RemoteCommandType.SETTINGS -> KeyEvent.KEYCODE_SETTINGS
            RemoteCommandType.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            RemoteCommandType.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
            RemoteCommandType.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            RemoteCommandType.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            RemoteCommandType.REWIND -> KeyEvent.KEYCODE_MEDIA_REWIND
            RemoteCommandType.FAST_FORWARD -> KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
            RemoteCommandType.VOLUME_UP -> KeyEvent.KEYCODE_VOLUME_UP
            RemoteCommandType.VOLUME_DOWN -> KeyEvent.KEYCODE_VOLUME_DOWN
            RemoteCommandType.MUTE -> KeyEvent.KEYCODE_VOLUME_MUTE
            RemoteCommandType.POWER -> KeyEvent.KEYCODE_POWER
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }
}
