package dev.ao3m3.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DiscoverRoute

@Serializable
data object SearchRoute

@Serializable
data object LibraryRoute

@Serializable
data object SettingsRoute

@Serializable
data object LoginRoute

@Serializable
data class WorkRoute(val workId: Long)

/** [chapterId] of -1 means "resume wherever the reader left off". */
@Serializable
data class ReaderRoute(val workId: Long, val chapterId: Long = -1L)
