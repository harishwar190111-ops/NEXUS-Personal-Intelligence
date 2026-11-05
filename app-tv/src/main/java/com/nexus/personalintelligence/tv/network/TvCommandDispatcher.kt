package com.nexus.personalintelligence.tv.network

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent
import com.nexus.personalintelligence.core.remote.CommandResult
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import com.nexus.personalintelligence.core.remote.diagnostics.CommandHistoryEntry
import com.nexus.personalintelligence.core.remote.diagnostics.CommandHistoryTracker
import com.nexus.personalintelligence.core.remote.events.EventCategory
import com.nexus.personalintelligence.core.remote.events.EventSeverity
import com.nexus.personalintelligence.core.remote.events.NexusEvent
import com.nexus.personalintelligence.core.remote.events.NexusEventStore
import com.nexus.personalintelligence.tv.data.AppRepository
import com.nexus.personalintelligence.tv.data.LaunchResult

class TvCommandDispatcher(
    private val context: Context,
    private val appRepo: AppRepository = AppRepository(context),
    val historyTracker: CommandHistoryTracker = CommandHistoryTracker(),
    val eventStore: NexusEventStore = NexusEventStore(),
    private val onNavigateDirection: ((RemoteCommandType) -> Boolean)? = null,
    private val onNavigateHome: (() -> Boolean)? = null,
    private val onTextReceived: ((String) -> Boolean)? = null,
    private val onPointerAction: ((RemoteCommand) -> Boolean)? = null,
    private val onKeyAction: ((RemoteCommand) -> Boolean)? = null
) {
    suspend fun dispatch(command: RemoteCommand): CommandResult {
        val startNs = System.nanoTime()
        val result = try {
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
                RemoteCommandType.HOME -> {
                    if (onNavigateHome?.invoke() == true) {
                        CommandResult(command.commandId, true, "Navigated to HOME")
                    } else {
                        try {
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(homeIntent)
                            CommandResult(command.commandId, true, "Navigated to HOME")
                        } catch (e: Exception) {
                            CommandResult(command.commandId, false, "HOME navigation unavailable: ${e.message}")
                        }
                    }
                }
                RemoteCommandType.SETTINGS -> {
                    try {
                        val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(settingsIntent)
                        CommandResult(command.commandId, true, "Opened TV settings")
                    } catch (e: Exception) {
                        CommandResult(command.commandId, false, "Settings navigation unavailable: ${e.message}")
                    }
                }
                RemoteCommandType.VOLUME_UP,
                RemoteCommandType.VOLUME_DOWN,
                RemoteCommandType.MUTE -> {
                    val audio = runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
                    if (audio != null) {
                        try {
                            when (command.type) {
                                RemoteCommandType.VOLUME_UP -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                                RemoteCommandType.VOLUME_DOWN -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                                RemoteCommandType.MUTE -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                                else -> {}
                            }
                            CommandResult(command.commandId, true, "Volume command executed")
                        } catch (e: Exception) {
                            CommandResult(command.commandId, false, "Volume adjustment failed: ${e.message}")
                        }
                    } else {
                        CommandResult(command.commandId, false, "Volume control unavailable: AudioManager not found")
                    }
                }
                RemoteCommandType.PLAY_PAUSE,
                RemoteCommandType.STOP,
                RemoteCommandType.PREVIOUS,
                RemoteCommandType.NEXT,
                RemoteCommandType.REWIND,
                RemoteCommandType.FAST_FORWARD -> {
                    val keycode = toKeyCode(command.type)
                    val audio = runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
                    if (audio != null && keycode != KeyEvent.KEYCODE_UNKNOWN) {
                        try {
                            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
                            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))
                            CommandResult(command.commandId, true, "Dispatched media key")
                        } catch (e: Exception) {
                            CommandResult(command.commandId, false, "Media key dispatch failed: ${e.message}")
                        }
                    } else {
                        CommandResult(command.commandId, false, "Media key dispatch unavailable: AudioManager not found")
                    }
                }
                RemoteCommandType.DPAD_UP,
                RemoteCommandType.DPAD_DOWN,
                RemoteCommandType.DPAD_LEFT,
                RemoteCommandType.DPAD_RIGHT,
                RemoteCommandType.DPAD_CENTER,
                RemoteCommandType.BACK,
                RemoteCommandType.MENU -> {
                    if (onNavigateDirection?.invoke(command.type) == true) {
                        CommandResult(command.commandId, true, "Navigation ${command.type.name} executed")
                    } else {
                        CommandResult(
                            commandId = command.commandId,
                            success = false,
                            message = "Navigation ${command.type.name} unavailable: no active navigation target or accessibility service"
                        )
                    }
                }
                RemoteCommandType.POWER -> {
                    CommandResult(
                        commandId = command.commandId,
                        success = false,
                        message = "Power control unavailable: requires device owner or system permission"
                    )
                }
                RemoteCommandType.POINTER_MOVE,
                RemoteCommandType.POINTER_TAP,
                RemoteCommandType.POINTER_DOUBLE_TAP,
                RemoteCommandType.POINTER_LONG_PRESS,
                RemoteCommandType.POINTER_SCROLL -> {
                    if (onPointerAction?.invoke(command) == true) {
                        CommandResult(command.commandId, true, "Pointer action processed")
                    } else {
                        CommandResult(
                            commandId = command.commandId,
                            success = false,
                            message = "Pointer control unavailable: requires TV Accessibility Service or active touch surface"
                        )
                    }
                }
                RemoteCommandType.TEXT_INPUT,
                RemoteCommandType.BACKSPACE,
                RemoteCommandType.ENTER -> {
                    val handled = when (command.type) {
                        RemoteCommandType.TEXT_INPUT -> onTextReceived?.invoke(command.text.orEmpty()) ?: false
                        RemoteCommandType.BACKSPACE -> onTextReceived?.invoke("\b") ?: false
                        RemoteCommandType.ENTER -> onTextReceived?.invoke("\n") ?: false
                        else -> false
                    }

                    if (handled) {
                        val label = when (command.type) {
                            RemoteCommandType.TEXT_INPUT -> "Text input delivered"
                            RemoteCommandType.BACKSPACE -> "Backspace delivered"
                            RemoteCommandType.ENTER -> "Enter delivered"
                            else -> "Input delivered"
                        }
                        CommandResult(command.commandId, true, label)
                    } else {
                        CommandResult(
                            commandId = command.commandId,
                            success = false,
                            message = "No active text receiver available on TV"
                        )
                    }
                }
                else -> {
                    if (onKeyAction?.invoke(command) == true) {
                        CommandResult(command.commandId, true, "Key input processed")
                    } else {
                        CommandResult(
                            commandId = command.commandId,
                            success = false,
                            message = "Key input injection unavailable: requires active focused input or accessibility service"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            CommandResult(
                commandId = command.commandId,
                success = false,
                message = "Execution failed: ${e.message}"
            )
        }

        val latencyMs = (System.nanoTime() - startNs) / 1_000_000L

        historyTracker.addEntry(
            CommandHistoryEntry(
                commandId = command.commandId,
                type = command.type.name,
                success = result.success,
                message = result.message,
                latencyMs = latencyMs
            )
        )

        eventStore.addEvent(
            NexusEvent(
                title = "Command: ${command.type.name}",
                message = result.message,
                severity = if (result.success) EventSeverity.INFO else EventSeverity.WARNING,
                category = EventCategory.COMMAND
            )
        )

        return result
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
