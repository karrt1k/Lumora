package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

object FolderBlockListManager {

    private const val PREFS_NAME = "lumora_folder_blocklist"
    private const val KEY_BLOCKED_VIDEOS = "blocked_video_folder_ids"
    private const val KEY_BLOCKED_AUDIOS = "blocked_audio_folder_paths"

    private val _blockedVideoFolderIds = mutableStateOf<Set<String>>(emptySet())
    val blockedVideoFolderIds: State<Set<String>> = _blockedVideoFolderIds

    private val _blockedAudioFolderPaths = mutableStateOf<Set<String>>(emptySet())
    val blockedAudioFolderPaths: State<Set<String>> = _blockedAudioFolderPaths

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        val videos = prefs.getStringSet(KEY_BLOCKED_VIDEOS, emptySet()) ?: emptySet()
        val audios = prefs.getStringSet(KEY_BLOCKED_AUDIOS, emptySet()) ?: emptySet()
        _blockedVideoFolderIds.value = videos.toSet()
        _blockedAudioFolderPaths.value = audios.toSet()
        isInitialized = true
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun unblockVideoFolder(context: Context, folderId: String) {
        init(context)
        val current = _blockedVideoFolderIds.value.toMutableSet()
        current.remove(folderId)
        _blockedVideoFolderIds.value = current
        getPrefs(context).edit().putStringSet(KEY_BLOCKED_VIDEOS, current).apply()
    }

    fun unblockAudioFolder(context: Context, folderPath: String) {
        init(context)
        val current = _blockedAudioFolderPaths.value.toMutableSet()
        current.remove(folderPath)
        _blockedAudioFolderPaths.value = current
        getPrefs(context).edit().putStringSet(KEY_BLOCKED_AUDIOS, current).apply()
    }

    fun updateBlockList(
        context: Context,
        videoFolderIds: Set<String>,
        audioFolderPaths: Set<String>
    ) {
        init(context)
        _blockedVideoFolderIds.value = videoFolderIds.toSet()
        _blockedAudioFolderPaths.value = audioFolderPaths.toSet()
        getPrefs(context).edit()
            .putStringSet(KEY_BLOCKED_VIDEOS, videoFolderIds)
            .putStringSet(KEY_BLOCKED_AUDIOS, audioFolderPaths)
            .apply()
    }

}
