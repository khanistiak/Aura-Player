package com.istiak.auraplayer

import java.util.Locale

data class DirectoryNode(
    val name: String,
    val fullPath: String,
    val childFolders: List<DirectoryNode> = emptyList(),
    val directVideos: List<VideoItem> = emptyList(),
    val recursiveVideoCount: Int = 0,
    val recursiveSizeBytes: Long = 0L,
    val recursiveDurationMs: Long = 0L,
    val latestDateAddedSec: Long = 0L,
    val allRecursiveVideos: List<VideoItem> = emptyList()
) {
    val immediateFolderCount: Int get() = childFolders.size
    val directVideoCount: Int get() = directVideos.size
    val formattedRecursiveSize: String get() = formatFileSize(recursiveSizeBytes)
    val formattedRecursiveDuration: String get() = formatFolderDurationCompact(recursiveDurationMs)

    fun unwatchedVideoCount(
        watchedVideoIds: Set<Long>,
        seenBaselineEpochSec: Long = 0L
    ): Int {
        return allRecursiveVideos.count { it.isConsideredNew(watchedVideoIds, seenBaselineEpochSec) }
    }

    /**
     * Summary line showing immediate subfolder count + direct video count + total recursive size
     * (e.g. "4 videos, 1 folder  54 GB" or "12 folders  25 GB").
     */
    fun buildSummaryLine(
        includeTotalMedia: Boolean = true,
        includeFolderSize: Boolean = true,
        includeTotalDuration: Boolean = false
    ): String {
        val countParts = mutableListOf<String>()
        if (includeTotalMedia) {
            if (directVideoCount > 0) {
                countParts.add("$directVideoCount ${if (directVideoCount == 1) "video" else "videos"}")
            }
            if (immediateFolderCount > 0) {
                countParts.add("$immediateFolderCount ${if (immediateFolderCount == 1) "folder" else "folders"}")
            }
            if (countParts.isEmpty() && recursiveVideoCount > 0) {
                countParts.add("$recursiveVideoCount ${if (recursiveVideoCount == 1) "video" else "videos"}")
            }
        }

        val leftPart = countParts.joinToString(", ")
        val tailParts = mutableListOf<String>()
        if (includeFolderSize) {
            tailParts.add(formattedRecursiveSize)
        }
        if (includeTotalDuration && recursiveDurationMs > 0L) {
            tailParts.add(formattedRecursiveDuration)
        }
        val rightPart = tailParts.joinToString(" • ")

        return when {
            leftPart.isNotEmpty() && rightPart.isNotEmpty() -> "$leftPart  $rightPart"
            leftPart.isNotEmpty() -> leftPart
            else -> rightPart
        }
    }
}

object DirectoryTreeBuilder {

    /**
     * Normalizes MediaStore RELATIVE_PATH (Android 10+) or DATA path into a clean relative
     * directory path from Internal Storage root (e.g., "Pictures/Cox's Bazar Tour 2024").
     */
    fun normalizeDirectoryPath(
        relativePath: String?,
        dataPath: String?,
        fallbackBucket: String
    ): String {
        val rawRel = relativePath?.trim()?.trim('/')
        if (!rawRel.isNullOrBlank()) {
            return rawRel
        }

        val rawData = dataPath?.trim()
        if (!rawData.isNullOrBlank()) {
            val parentDir = rawData.substringBeforeLast('/', "")
            val stripped = parentDir
                .replace(Regex("^/storage/emulated/\\d+/?"), "")
                .replace(Regex("^/storage/[^/]+/?"), "")
                .replace(Regex("^/sdcard/?"), "")
                .trim('/')
            if (stripped.isNotBlank()) {
                return stripped
            }
        }

        return fallbackBucket.ifBlank { "Others" }
    }

    private class MutableTreeNode(
        val name: String,
        val fullPath: String
    ) {
        val children = linkedMapOf<String, MutableTreeNode>()
        val directVideos = mutableListOf<VideoItem>()
    }

    /**
     * Builds a genuine multi-level hierarchical directory tree starting at root ("Videos").
     */
    fun buildTree(
        videos: List<VideoItem>,
        sortField: SortField = SortField.TITLE,
        sortAscending: Boolean = true
    ): DirectoryNode {
        val root = MutableTreeNode(name = "Videos", fullPath = "")

        for (video in videos) {
            val cleanPath = video.relativePath.trim('/')
            val segments = if (cleanPath.isBlank()) {
                listOf(video.folderName.ifBlank { "Others" })
            } else {
                cleanPath.split('/').filter { it.isNotBlank() }
            }

            var current = root
            val cumulative = StringBuilder()
            for (segment in segments) {
                if (cumulative.isNotEmpty()) cumulative.append('/')
                cumulative.append(segment)
                val pathStr = cumulative.toString()
                current = current.children.getOrPut(segment) {
                    MutableTreeNode(name = segment, fullPath = pathStr)
                }
            }
            current.directVideos.add(video)
        }

        return finalizeNode(root, sortField, sortAscending)
    }

    private fun finalizeNode(
        mutable: MutableTreeNode,
        sortField: SortField,
        sortAscending: Boolean
    ): DirectoryNode {
        val finalizedChildren = mutable.children.values
            .map { finalizeNode(it, sortField, sortAscending) }

        val sortedChildren = sortFolders(finalizedChildren, sortField, sortAscending)
        val sortedDirectVideos = sortVideos(mutable.directVideos, sortField, sortAscending)

        val recursiveVideos = mutableListOf<VideoItem>().apply {
            addAll(sortedDirectVideos)
            sortedChildren.forEach { addAll(it.allRecursiveVideos) }
        }

        val totalSize = recursiveVideos.sumOf { it.sizeBytes }
        val totalDuration = recursiveVideos.sumOf { it.durationMs }
        val maxDate = recursiveVideos.maxOfOrNull { it.dateAddedSec } ?: 0L

        return DirectoryNode(
            name = mutable.name,
            fullPath = mutable.fullPath,
            childFolders = sortedChildren,
            directVideos = sortedDirectVideos,
            recursiveVideoCount = recursiveVideos.size,
            recursiveSizeBytes = totalSize,
            recursiveDurationMs = totalDuration,
            latestDateAddedSec = maxDate,
            allRecursiveVideos = recursiveVideos
        )
    }

    fun getNodeAtPath(root: DirectoryNode, path: String): DirectoryNode {
        val clean = path.trim('/')
        if (clean.isEmpty()) return root
        val segments = clean.split('/').filter { it.isNotBlank() }
        var current = root
        for (segment in segments) {
            val next = current.childFolders.find { it.name == segment } ?: return current
            current = next
        }
        return current
    }

    /**
     * Returns breadcrumb pairs of (displayLabel, fullPath) starting with ("Videos", "").
     */
    fun getBreadcrumbs(path: String): List<Pair<String, String>> {
        val list = mutableListOf("Videos" to "")
        val clean = path.trim('/')
        if (clean.isEmpty()) return list

        val segments = clean.split('/').filter { it.isNotBlank() }
        val cumulative = StringBuilder()
        for (segment in segments) {
            if (cumulative.isNotEmpty()) cumulative.append('/')
            cumulative.append(segment)
            list.add(segment to cumulative.toString())
        }
        return list
    }

    fun getParentPath(path: String): String {
        val clean = path.trim('/')
        return if ('/' in clean) clean.substringBeforeLast('/') else ""
    }

    private fun sortFolders(
        nodes: List<DirectoryNode>,
        sortField: SortField,
        sortAscending: Boolean
    ): List<DirectoryNode> {
        val comparator = when (sortField) {
            SortField.TITLE, SortField.STATUS, SortField.RESOLUTION,
            SortField.FRAME_RATE, SortField.TYPE -> compareBy<DirectoryNode> {
                it.name.lowercase(Locale.US)
            }
            SortField.PATH -> compareBy { it.fullPath.lowercase(Locale.US) }
            SortField.DATE, SortField.PLAYED_TIME -> compareBy { it.latestDateAddedSec }
            SortField.LENGTH -> compareBy { it.recursiveDurationMs }
            SortField.SIZE -> compareBy { it.recursiveSizeBytes }
        }
        return if (sortAscending) nodes.sortedWith(comparator) else nodes.sortedWith(comparator.reversed())
    }

    private fun sortVideos(
        videos: List<VideoItem>,
        sortField: SortField,
        sortAscending: Boolean
    ): List<VideoItem> {
        val comparator = when (sortField) {
            SortField.TITLE -> compareBy<VideoItem> { it.displayName.lowercase(Locale.US) }
            SortField.DATE -> compareBy { it.dateAddedSec }
            SortField.PLAYED_TIME -> compareBy { it.dateAddedSec }
            SortField.STATUS -> compareBy { it.hasSubtitles }
            SortField.LENGTH -> compareBy { it.durationMs }
            SortField.SIZE -> compareBy { it.sizeBytes }
            SortField.RESOLUTION -> compareBy { it.width.toLong() * it.height.toLong() }
            SortField.PATH -> compareBy { it.relativePath.lowercase(Locale.US) }
            SortField.FRAME_RATE -> compareBy { it.frameRate }
            SortField.TYPE -> compareBy { it.extensionLabel }
        }
        return if (sortAscending) videos.sortedWith(comparator) else videos.sortedWith(comparator.reversed())
    }
}
