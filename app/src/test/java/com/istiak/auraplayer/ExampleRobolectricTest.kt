package com.istiak.auraplayer

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Player", appName)
    }

    @Test
    fun `video repository groups folders and formats resolution framerate labels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = VideoRepository(context)
        val sampleVideos = listOf(
            VideoItem(
                id = 1L,
                uri = Uri.parse("content://media/external/video/media/1"),
                title = "Zeta 4K Demo",
                displayName = "zeta.mp4",
                durationMs = 15_000L,
                sizeBytes = 50_000_000L,
                width = 3840,
                height = 2160,
                folderName = "Cinema 4K",
                relativePath = "Movies/Cinema 4K",
                frameRate = 24f,
                dateAddedSec = 100L
            ),
            VideoItem(
                id = 2L,
                uri = Uri.parse("content://media/external/video/media/2"),
                title = "Alpha 1080P Clip",
                displayName = "alpha.mp4",
                durationMs = 8_000L,
                sizeBytes = 10_000_000L,
                width = 1920,
                height = 1080,
                folderName = "Camera",
                relativePath = "DCIM/Camera",
                frameRate = 23.98f,
                dateAddedSec = 200L
            )
        )

        val sortedAsc = repo.filterAndSortVideos(sampleVideos, SortField.TITLE, true, "")
        assertEquals("alpha.mp4", sortedAsc.first().displayName)
        assertEquals("2160p@24", sampleVideos.first().resolutionFramerateLabel)
        assertEquals("1080p@23.98", sampleVideos[1].resolutionFramerateLabel)

        val folders = repo.groupVideosByFolder(sampleVideos, SortField.TITLE, true)
        assertEquals(2, folders.size)
        assertTrue(folders.any { it.name == "Cinema 4K" && it.has4K })
    }

    @Test
    fun `directory tree builder constructs multi-level hierarchy and breadcrumbs`() {
        val videos = listOf(
            VideoItem(
                id = 10L,
                uri = Uri.parse("content://media/external/video/media/10"),
                title = "Coxs Bazar Drone",
                displayName = "drone.mp4",
                durationMs = 20_000L,
                sizeBytes = 40_000_000L,
                width = 1920,
                height = 1080,
                folderName = "Cox's Bazar Tour 2024",
                relativePath = "Pictures/Cox's Bazar Tour 2024",
                dateAddedSec = 100L
            ),
            VideoItem(
                id = 11L,
                uri = Uri.parse("content://media/external/video/media/11"),
                title = "Screen Walkthrough",
                displayName = "screen.mp4",
                durationMs = 12_000L,
                sizeBytes = 12_000_000L,
                width = 1920,
                height = 1080,
                folderName = "Screenshots",
                relativePath = "Pictures/Screenshots",
                dateAddedSec = 110L
            ),
            VideoItem(
                id = 12L,
                uri = Uri.parse("content://media/external/video/media/12"),
                title = "Movie Root Clip",
                displayName = "root_movie.mp4",
                durationMs = 30_000L,
                sizeBytes = 100_000_000L,
                width = 3840,
                height = 2160,
                folderName = "Movies",
                relativePath = "Movies",
                dateAddedSec = 120L
            )
        )

        val root = DirectoryTreeBuilder.buildTree(videos, SortField.TITLE, sortAscending = true)
        assertEquals("Videos", root.name)
        // Root must ONLY have top-level folders (Movies, Pictures), never flattened subfolders
        assertEquals(listOf("Movies", "Pictures"), root.childFolders.map { it.name })
        assertTrue(root.directVideos.isEmpty())

        val picturesNode = DirectoryTreeBuilder.getNodeAtPath(root, "Pictures")
        assertEquals(2, picturesNode.immediateFolderCount)
        assertEquals(0, picturesNode.directVideoCount)
        assertEquals(2, picturesNode.recursiveVideoCount)
        assertEquals(2, picturesNode.unwatchedVideoCount(emptySet()))
        assertEquals(1, picturesNode.unwatchedVideoCount(setOf(10L)))

        val breadcrumbs = DirectoryTreeBuilder.getBreadcrumbs("Pictures/Cox's Bazar Tour 2024")
        assertEquals(
            listOf(
                "Videos" to "",
                "Pictures" to "Pictures",
                "Cox's Bazar Tour 2024" to "Pictures/Cox's Bazar Tour 2024"
            ),
            breadcrumbs
        )
        assertEquals("Pictures", DirectoryTreeBuilder.getParentPath("Pictures/Cox's Bazar Tour 2024"))
        assertEquals("", DirectoryTreeBuilder.getParentPath("Pictures"))
    }

    @Test
    fun `duration formatting uses H mm ss for hours and mm ss below one hour`() {
        assertEquals("00:00", formatDurationMillis(0L))
        assertEquals("14:06", formatDurationMillis((14 * 60 + 6) * 1000L))
        assertEquals("1:57:30", formatDurationMillis((3600 + 57 * 60 + 30) * 1000L))
        assertEquals(DecoderPriority.HW, DecoderPriority.fromStored("HW"))
        assertEquals(DecoderPriority.HW_PLUS, DecoderPriority.fromStored("HW_PLUS"))
        assertEquals(DecoderPriority.SW, DecoderPriority.fromStored("SW"))
        assertEquals(DecoderPriority.SW, DecoderPriority.fromStored("SOFTWARE"))
    }

    @Test
    fun `private vault manager initializes empty list cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vaultManager = PrivateVaultManager(context)
        val hiddenVideos = vaultManager.getVaultVideos()
        assertTrue(hiddenVideos.isEmpty())
    }

    @Test
    fun `video repository supports all ten sort fields and default display fields`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = VideoRepository(context)
        val v1 = VideoItem(
            id = 1L,
            uri = Uri.parse("content://media/external/video/media/1"),
            title = "Short 4K MKV",
            displayName = "short.mkv",
            durationMs = 5_000L,
            sizeBytes = 100_000L,
            width = 3840,
            height = 2160,
            folderName = "Movies",
            relativePath = "Movies",
            frameRate = 60f,
            dateAddedSec = 100L
        )
        val v2 = VideoItem(
            id = 2L,
            uri = Uri.parse("content://media/external/video/media/2"),
            title = "Long HD MP4",
            displayName = "long.mp4",
            durationMs = 60_000L,
            sizeBytes = 20_000L,
            width = 1920,
            height = 1080,
            folderName = "DCIM",
            relativePath = "DCIM",
            frameRate = 24f,
            dateAddedSec = 200L
        )
        val list = listOf(v1, v2)

        assertEquals(v1.id, repo.filterAndSortVideos(list, SortField.LENGTH, true, "").first().id)
        assertEquals(v2.id, repo.filterAndSortVideos(list, SortField.RESOLUTION, true, "").first().id)
        assertEquals(v2.id, repo.filterAndSortVideos(list, SortField.SIZE, true, "").first().id)
        assertEquals(v2.id, repo.filterAndSortVideos(list, SortField.PATH, true, "").first().id)
        assertEquals(v1.id, repo.filterAndSortVideos(list, SortField.TYPE, true, "").first().id)
        assertEquals(v2.id, repo.filterAndSortVideos(list, SortField.FRAME_RATE, true, "").first().id)

        assertTrue(DisplayField.THUMBNAILS in DisplayField.defaultSet)
        assertTrue(DisplayField.DURATION in DisplayField.defaultSet)
        assertTrue(DisplayField.RESOLUTION in DisplayField.defaultSet)
        assertTrue(DisplayField.SIZE in DisplayField.defaultSet)
        assertTrue(DisplayField.TOTAL_MEDIA in DisplayField.defaultSet)
        assertTrue(DisplayField.FOLDER_SIZE in DisplayField.defaultSet)
        assertTrue(DisplayField.TOTAL_DURATION in DisplayField.defaultSet)
        assertTrue(DisplayField.PROGRESS_BAR in DisplayField.defaultSet)
        assertTrue(DisplayField.EXTENSION !in DisplayField.defaultSet)
        assertTrue(DisplayField.PLAYED_TIME !in DisplayField.defaultSet)
    }

    @Test
    fun `codec diagnostics includes bundled ffmpeg audio software decoders`() {
        val codecs = queryDeviceCodecDiagnostics()
        val mimes = codecs.flatMap { it.mimeTypes }
        assertTrue(mimes.contains("audio/eac3"))
        assertTrue(mimes.contains("audio/ac3"))
        assertTrue(mimes.contains("audio/vnd.dts"))
        assertTrue(mimes.contains("audio/vnd.dts.hd"))
        assertTrue(mimes.contains("audio/true-hd"))
        assertTrue(mimes.contains("audio/mlp"))
    }

    @Test
    fun `playlist repository seeds Favorites and supports creating and adding videos`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PlaylistRepository(context)
        val created = repo.createPlaylist("Action 4K")
        repo.addVideosToPlaylists(setOf(PlaylistRepository.FAVORITES_ID, created.id), listOf(101L, 202L))

        val playlists = repo.playlistsFlow.first()
        val favorites = playlists.find { it.id == PlaylistRepository.FAVORITES_ID }
        val custom = playlists.find { it.name == "Action 4K" }
        assertTrue(favorites != null && favorites.videoIds.containsAll(listOf(101L, 202L)))
        assertTrue(custom != null && custom.videoIds.containsAll(listOf(101L, 202L)))

        val storageDirs = VideoRepository(context).collectAvailableStorageDirectories(
            allVideos = emptyList(),
            extraCreatedFolders = setOf("Movies/Weekend")
        )
        assertTrue("Movies" in storageDirs)
        assertTrue("Movies/Weekend" in storageDirs)
    }

    @Test
    fun `media info inspection generates structured stream specs and raw report`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sampleVideo = VideoItem(
            id = 42L,
            uri = Uri.parse("content://media/external/video/media/42"),
            title = "Neon.Horizon.4K",
            displayName = "Neon.Horizon.2160p.HEVC.mkv",
            durationMs = 125_000L,
            sizeBytes = 524_288_000L,
            width = 3840,
            height = 2160,
            folderName = "Movies",
            relativePath = "Movies",
            frameRate = 24f,
            videoCodec = "HEVC 10-bit (HW+)",
            audioTrackInfo = "E-AC-3 5.1",
            subtitleInfo = "English [SRT]",
            hasSubtitles = true,
            dateAddedSec = 1_700_000_000L,
            mimeType = "video/x-matroska"
        )
        val info = inspectVideoMediaInfo(context, sampleVideo)
        assertEquals("MATROSKA", info.containerBadge)
        assertTrue(info.videoStreams.isNotEmpty())
        assertTrue(info.audioStreams.isNotEmpty())
        assertTrue(info.subtitleStreams.isNotEmpty())
        assertTrue(info.rawDump.contains("General"))
        assertTrue(info.rawDump.contains("Video #1"))
        assertTrue(info.rawDump.contains("Audio #1"))
    }

    @Test
    fun `application package and ambient palette tokens match specification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.istiak.auraplayer", context.packageName)
        assertEquals(
            androidx.compose.ui.graphics.Color(0xFFDCEEFF),
            com.istiak.auraplayer.ui.theme.AmbientSkyBlueTop
        )
        assertEquals(
            androidx.compose.ui.graphics.Color(0xFFECE2FF),
            com.istiak.auraplayer.ui.theme.AmbientLilacLavenderTopRight
        )
        assertEquals(
            androidx.compose.ui.graphics.Color(0xFFF8F9FE),
            com.istiak.auraplayer.ui.theme.AmbientSoftOffWhiteBase
        )
    }

    @Test
    fun `playback memory repository persists and restores per-video state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memoryRepo = PlaybackMemoryRepository(context)
        val uri = "content://media/external/video/media/999"
        memoryRepo.saveMemory(
            VideoPlaybackMemory(
                videoId = 999L,
                videoUri = uri,
                resumePositionMs = 48_500L,
                selectedAudioGroupIndex = 0,
                selectedAudioTrackIndex = 1,
                selectedSubtitleGroupIndex = 1,
                selectedSubtitleTrackIndex = 2,
                subtitleSyncOffsetMs = -250L,
                scaleMode = PlayerScaleMode.CROP
            )
        )
        val restored = memoryRepo.getMemory(999L, uri)
        assertTrue(restored != null)
        assertEquals(48_500L, restored?.resumePositionMs)
        assertEquals(1, restored?.selectedAudioTrackIndex)
        assertEquals(2, restored?.selectedSubtitleTrackIndex)
        assertEquals(-250L, restored?.subtitleSyncOffsetMs)
        assertEquals(PlayerScaleMode.CROP, restored?.scaleMode)
    }

    @Test
    fun `open subtitles helper searches and writes srt subtitle to cache`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resultsEn = OpenSubtitlesHelper.searchSubtitles("Cyberpunk.2077.2160p.HEVC.mkv", "en")
        val resultsBn = OpenSubtitlesHelper.searchSubtitles("Cyberpunk.2077.2160p.HEVC.mkv", "bn")
        val resultsHi = OpenSubtitlesHelper.searchSubtitles("Cyberpunk.2077.2160p.HEVC.mkv", "hi")
        assertTrue(resultsEn.isNotEmpty() && resultsEn.first().languageLabel.contains("English"))
        assertTrue(resultsBn.isNotEmpty() && resultsBn.first().languageLabel.contains("Bengali"))
        assertTrue(resultsHi.isNotEmpty() && resultsHi.first().languageLabel.contains("Hindi"))

        val (_, downloadedFile) = OpenSubtitlesHelper.downloadSubtitleToCache(
            context = context,
            result = resultsEn.first(),
            videoTitle = "Cyberpunk 2077"
        )
        assertTrue(downloadedFile.exists())
        assertTrue(downloadedFile.name.endsWith(".srt"))
        assertEquals("subtitles", downloadedFile.parentFile?.name)
    }

    @Test
    fun `private vault manager uses AuraPlayer dot-private directory and vault kv format`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vaultManager = PrivateVaultManager(context)
        val vaultDir = vaultManager.vaultDir
        assertEquals(".private", vaultDir.name)
        assertEquals("AuraPlayer", vaultDir.parentFile?.name)
        assertTrue(java.io.File(vaultDir, ".nomedia").exists())

        val hexId = vaultManager.generateUpperHexId()
        assertEquals(32, hexId.length)
        assertTrue(hexId.all { it in '0'..'9' || it in 'A'..'F' })
    }

    @Test
    fun `private vault lockVideo writes vault kv mapping and unlocks cleanly with non-overwriting fallback`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vaultManager = PrivateVaultManager(context)
        val stagingDir = java.io.File(context.filesDir, "test_movies").apply { mkdirs() }
        val sourceFile = java.io.File(stagingDir, "atomic_test.mp4")
        val originalBytes = ByteArray(80_000) { idx -> ((idx * 17 + 3) and 0xFF).toByte() }
        sourceFile.writeBytes(originalBytes)

        val videoItem = VideoItem(
            id = 777L,
            uri = Uri.fromFile(sourceFile),
            title = "Atomic Test",
            displayName = "atomic_test.mp4",
            durationMs = 10_000L,
            sizeBytes = originalBytes.size.toLong(),
            width = 1920,
            height = 1080,
            folderName = "test_movies",
            relativePath = "test_movies",
            dateAddedSec = 1_700_000_000L
        )

        val firstLock = vaultManager.lockVideo(videoItem)
        assertTrue(firstLock is MoveToVaultResult.Completed)
        assertTrue(!sourceFile.exists())

        val kvEntries = vaultManager.readVaultKvEntries()
        assertEquals(1, kvEntries.size)
        val kvEntry = kvEntries.first()
        assertTrue(kvEntry.privateFilePath.endsWith(".private"))
        assertEquals(32, kvEntry.hexId.length)
        assertEquals(sourceFile.absolutePath, kvEntry.originalAbsolutePath)
        assertTrue(vaultManager.vaultKvFile.readText().contains("${kvEntry.privateFilePath}>${sourceFile.absolutePath}"))

        val firstVaultItems = vaultManager.getVaultVideos()
        assertEquals(1, firstVaultItems.size)
        assertEquals("atomic_test.mp4", firstVaultItems.first().displayName)
        assertEquals(Uri.fromFile(java.io.File(kvEntry.privateFilePath)), firstVaultItems.first().uri)
        assertTrue(sourceFile.absolutePath in vaultManager.getLockedOriginalPaths())

        // Locking the same path again must not create duplicate entries
        val secondLock = vaultManager.lockVideo(videoItem)
        assertTrue(secondLock is MoveToVaultResult.Completed)
        assertEquals(1, vaultManager.getVaultVideos().size)

        // Unlock restores file to exact original path via zero-copy renameTo
        val unlocked = vaultManager.unlockVideo(firstVaultItems.first())
        assertTrue(unlocked)
        assertTrue(sourceFile.exists())
        assertTrue(vaultManager.getVaultVideos().isEmpty())
        val restoredBytes = sourceFile.readBytes()
        assertEquals(originalBytes.size, restoredBytes.size)
        for (i in originalBytes.indices) {
            assertEquals(originalBytes[i], restoredBytes[i])
        }

        // Verify (1) non-overwriting restore path when a file already exists at originalAbsolutePath
        val nonOverwriting = vaultManager.resolveNonOverwritingRestorePath(sourceFile.absolutePath)
        assertTrue(nonOverwriting.endsWith("atomic_test(1).mp4"))

        assertTrue(stagingDir.deleteRecursively())
    }

    @Test
    fun `settings datastore persists video orientation, folder filters, gestures, and private folder flags`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        store.setVideoOrientationMode(VideoOrientationMode.SENSOR_LANDSCAPE)
        store.setHideSecureFolderSettings(true)
        store.setVaultSecurityQuestion("First pet?", "Luna")
        store.setHiddenFolderScanTarget(library = true, tree = false, folders = true)
        store.setMarkerFiles(setOf(".nomedia", ".ignore"))
        store.setMinVideoDurationSec(30)
        store.setMinFileSizeMb(5)
        store.setBlacklistedFolders(setOf("Download/Temp"))
        store.setWhitelistedFolders(setOf("Movies"), enabled = true)
        store.setHorizontalSwipeSensitivity(75)
        store.setHoldForSpeedMultiplier(2.5f)
        store.setDoubleTapSeekAreaWidthPercent(40)

        val state = store.settingsFlow.first()
        assertEquals(VideoOrientationMode.SENSOR_LANDSCAPE, state.videoOrientationMode)
        assertEquals(8, VideoOrientationMode.entries.size)
        assertTrue(state.hideSecureFolderSettings)
        assertEquals("First pet?", state.vaultSecurityQuestion)
        assertEquals("Luna", state.vaultSecurityAnswer)
        assertTrue(state.hiddenFolderScanLibrary)
        assertTrue(!state.hiddenFolderScanTree)
        assertTrue(state.hiddenFolderScanFolders)
        assertTrue(".ignore" in state.markerFiles)
        assertEquals(30, state.minVideoDurationSec)
        assertEquals(5, state.minFileSizeMb)
        assertTrue("Download/Temp" in state.blacklistedFolders)
        assertTrue(state.whitelistEnabled)
        assertTrue("Movies" in state.whitelistedFolders)
        assertEquals(75, state.horizontalSwipeSensitivity)
        assertEquals(2.5f, state.holdForSpeedMultiplier, 0.01f)
        assertEquals(40, state.doubleTapSeekAreaWidthPercent)
    }

    @Test
    fun `biometric auth custom vault pin sets and verifies cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BiometricAuth.setCustomVaultPin(context, "4829")
        assertTrue(BiometricAuth.hasCustomVaultPin(context))
        assertTrue(BiometricAuth.verifyCustomVaultPin(context, "4829"))
        assertTrue(!BiometricAuth.verifyCustomVaultPin(context, "0000"))
    }

    @Test
    fun `markAllVideosAsSeen clears folder counts until newer videos arrive`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        val oldVideo = VideoItem(
            id = 501L,
            uri = Uri.parse("content://media/external/video/media/501"),
            title = "Old Clip",
            displayName = "old.mp4",
            durationMs = 10_000L,
            sizeBytes = 5_000_000L,
            width = 1920,
            height = 1080,
            folderName = "Movies",
            relativePath = "Movies",
            dateAddedSec = 1_700_000_000L,
            dateModifiedSec = 1_700_000_000L
        )
        val preBaselineUnlisted = oldVideo.copy(
            id = 502L,
            displayName = "pre_baseline.mp4",
            dateAddedSec = 1_700_000_500L,
            dateModifiedSec = 1_700_000_500L
        )

        val baselineSec = 1_700_001_000L
        store.markAllVideosAsSeen(listOf(oldVideo.id), baselineSec)
        val updatedState = store.settingsFlow.first()
        assertEquals(baselineSec, updatedState.seenBaselineEpochSec)
        assertTrue(oldVideo.id in updatedState.watchedVideoIds)

        assertFalse(oldVideo.isConsideredNew(updatedState.watchedVideoIds, updatedState.seenBaselineEpochSec))
        assertFalse(preBaselineUnlisted.isConsideredNew(updatedState.watchedVideoIds, updatedState.seenBaselineEpochSec))

        val brandNewVideo = oldVideo.copy(
            id = 503L,
            displayName = "brand_new.mp4",
            dateAddedSec = 1_700_002_000L,
            dateModifiedSec = 1_700_002_000L
        )
        assertTrue(brandNewVideo.isConsideredNew(updatedState.watchedVideoIds, updatedState.seenBaselineEpochSec))

        val treeWithOldOnly = DirectoryTreeBuilder.buildTree(listOf(oldVideo, preBaselineUnlisted))
        assertEquals(0, treeWithOldOnly.unwatchedVideoCount(updatedState.watchedVideoIds, updatedState.seenBaselineEpochSec))

        val treeWithNewVideo = DirectoryTreeBuilder.buildTree(listOf(oldVideo, preBaselineUnlisted, brandNewVideo))
        assertEquals(1, treeWithNewVideo.unwatchedVideoCount(updatedState.watchedVideoIds, updatedState.seenBaselineEpochSec))
    }

    @Test
    fun `video orientation resolves portrait rotation 90 and 270 and all 8 modes immediately`() {
        val rotatedPortrait90 = VideoItem(
            id = 601L,
            uri = Uri.parse("content://media/external/video/media/601"),
            title = "Phone Portrait 90",
            displayName = "portrait90.mp4",
            durationMs = 8_000L,
            sizeBytes = 4_000_000L,
            width = 1920,
            height = 1080,
            folderName = "Camera",
            dateAddedSec = 1_700_000_000L,
            rotationDegrees = 90
        )
        assertEquals(1080 to 1920, rotatedPortrait90.effectiveVisualDimensions())
        assertTrue(rotatedPortrait90.isPortraitVideo)
        assertEquals("1080×1920", rotatedPortrait90.formattedResolution)
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveTargetActivityOrientation(
                VideoOrientationMode.VIDEO,
                rotatedPortrait90.width,
                rotatedPortrait90.height,
                rotatedPortrait90.rotationDegrees
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveTargetActivityOrientation(VideoOrientationMode.VIDEO, 1920, 1080, 270)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveTargetActivityOrientation(VideoOrientationMode.VIDEO, 1080, 1920, 0)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolveTargetActivityOrientation(VideoOrientationMode.VIDEO, 1920, 1080, 0)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveTargetActivityOrientation(VideoOrientationMode.FREE, 1920, 1080, 0)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveTargetActivityOrientation(VideoOrientationMode.PORTRAIT, 1920, 1080, 0)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            resolveTargetActivityOrientation(VideoOrientationMode.REVERSE_PORTRAIT, 1920, 1080, 0)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            resolveTargetActivityOrientation(VideoOrientationMode.SENSOR_PORTRAIT, 1920, 1080, 0)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveTargetActivityOrientation(VideoOrientationMode.LANDSCAPE, 1080, 1920, 0)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolveTargetActivityOrientation(VideoOrientationMode.REVERSE_LANDSCAPE, 1080, 1920, 0)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolveTargetActivityOrientation(VideoOrientationMode.SENSOR_LANDSCAPE, 1080, 1920, 0)
        )
    }

    @Test
    fun `resume playback mode persists Ask Resume StartOver and preserves legacy mapping`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        assertEquals(ResumePlaybackMode.ASK, ResumePlaybackMode.fromStored("ASK", legacyDefaultResume = true))
        assertEquals(ResumePlaybackMode.RESUME, ResumePlaybackMode.fromStored(null, legacyDefaultResume = true))
        assertEquals(ResumePlaybackMode.START_OVER, ResumePlaybackMode.fromStored(null, legacyDefaultResume = false))

        store.setResumePlaybackMode(ResumePlaybackMode.ASK)
        assertEquals(ResumePlaybackMode.ASK, store.settingsFlow.first().resumePlaybackMode)

        store.setResumePlaybackMode(ResumePlaybackMode.START_OVER)
        val startOverState = store.settingsFlow.first()
        assertEquals(ResumePlaybackMode.START_OVER, startOverState.resumePlaybackMode)
        assertFalse(startOverState.defaultResumePlayback)

        store.setResumePlaybackMode(ResumePlaybackMode.RESUME)
        val resumeState = store.settingsFlow.first()
        assertEquals(ResumePlaybackMode.RESUME, resumeState.resumePlaybackMode)
        assertTrue(resumeState.defaultResumePlayback)
    }

    @Test
    fun `storage volume discovery, resolution, and filtering separate internal, SD card, and USB OTG videos`() {
        val mounted = listOf(
            StorageVolumeOption(
                id = PRIMARY_STORAGE_VOLUME_ID,
                label = "Internal Storage",
                rootPath = "/storage/emulated/0",
                kind = StorageVolumeKind.INTERNAL
            ),
            StorageVolumeOption(
                id = "abcd-1234",
                label = "SD Card",
                rootPath = "/storage/ABCD-1234",
                kind = StorageVolumeKind.SD_CARD
            ),
            StorageVolumeOption(
                id = "usb-99",
                label = "USB OTG",
                rootPath = "/storage/USB-99",
                kind = StorageVolumeKind.USB_OTG
            )
        )

        assertEquals(
            PRIMARY_STORAGE_VOLUME_ID,
            resolveStorageVolumeId("/storage/emulated/0/Movies/film.mp4", null, mounted)
        )
        assertEquals(
            "abcd-1234",
            resolveStorageVolumeId("/storage/ABCD-1234/DCIM/trip.mp4", null, mounted)
        )
        assertEquals(
            "usb-99",
            resolveStorageVolumeId(null, "usb-99", mounted)
        )

        val internalVideo = VideoItem(
            id = 1L,
            uri = Uri.parse("content://media/external/video/media/1"),
            title = "Internal Movie",
            displayName = "internal.mp4",
            durationMs = 10_000L,
            sizeBytes = 5_000_000L,
            width = 1920,
            height = 1080,
            folderName = "Movies",
            dateAddedSec = 100L,
            storageVolumeId = PRIMARY_STORAGE_VOLUME_ID
        )
        val sdVideo = internalVideo.copy(
            id = 2L,
            title = "SD Trip",
            displayName = "sd.mp4",
            storageVolumeId = "abcd-1234"
        )
        val usbVideo = internalVideo.copy(
            id = 3L,
            title = "USB Clip",
            displayName = "usb.mp4",
            storageVolumeId = "usb-99"
        )
        val all = listOf(internalVideo, sdVideo, usbVideo)

        assertEquals(
            listOf(2L),
            filterVideosByStorageVolume(all, "abcd-1234", mounted).map { it.id }
        )
        assertEquals(
            listOf(3L),
            filterVideosByStorageVolume(all, "usb-99", mounted).map { it.id }
        )
        assertEquals(
            listOf(1L),
            filterVideosByStorageVolume(all, PRIMARY_STORAGE_VOLUME_ID, mounted).map { it.id }
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val discovered = discoverMountedStorageVolumes(context)
        assertTrue(discovered.any { it.id == PRIMARY_STORAGE_VOLUME_ID })
    }

    @Test
    fun `hardware video decoder count executes cleanly without exceptions`() {
        val hwDecoders = countHardwareVideoDecoders()
        assertTrue(hwDecoders >= 0)
    }

    @Test
    fun `github release updater parses latest apk asset and compares semantic versions`() {
        assertEquals(
            "https://api.github.com/repos/khanistiak/Aura-Player/releases/latest",
            GITHUB_LATEST_RELEASE_API
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AppUpdateManager(context)
        assertEquals("2.6.2", manager.getInstalledVersionName(context))

        assertTrue(AppUpdateManager.isNewerVersion("2.7.0", "2.6.2"))
        assertTrue(AppUpdateManager.isNewerVersion("v2.6.3", "2.6.2"))
        assertFalse(AppUpdateManager.isNewerVersion("v2.6.2", "2.6.2"))
        assertFalse(AppUpdateManager.isNewerVersion("v2.6.2-rc1", "2.6.2"))
        assertFalse(AppUpdateManager.isNewerVersion("v2.6", "2.6.0"))
        assertFalse(AppUpdateManager.isNewerVersion("2.6.1", "2.6.2"))

        assertTrue(AppUpdateManager.shouldPromptRelease("v2.7.0", "2.6.2", ""))
        assertFalse(AppUpdateManager.shouldPromptRelease("v2.7.0", "2.6.2", "v2.7.0"))
        assertTrue(AppUpdateManager.shouldPromptRelease("v2.7.1", "2.6.2", "v2.7.0"))
        assertFalse(AppUpdateManager.shouldPromptRelease("v2.6.2", "2.6.2", ""))

        val sampleJson = """
            {
              "tag_name": "v2.7.0",
              "name": "Aura Player v2.7.0",
              "published_at": "2026-11-16T18:00:00Z",
              "body": "[critical]\n- Added Liquid Glass update bottom sheet\n- Improved 4K HEVC playback",
              "html_url": "https://github.com/khanistiak/Aura-Player/releases/tag/v2.7.0",
              "assets": [
                {
                  "name": "checksums.txt",
                  "size": 128,
                  "browser_download_url": "https://github.com/khanistiak/Aura-Player/releases/download/v2.7.0/checksums.txt"
                },
                {
                  "name": "AuraPlayer-v2.7.0-release.apk",
                  "size": 19293798,
                  "browser_download_url": "https://github.com/khanistiak/Aura-Player/releases/download/v2.7.0/AuraPlayer-v2.7.0-release.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = AppUpdateManager.parseReleaseJson(sampleJson)
        assertTrue(parsed != null)
        assertEquals("2.7.0", parsed!!.version)
        assertEquals("November 2026", parsed.publishedMonthYear)
        assertTrue(parsed.isCritical)
        assertEquals("AuraPlayer-v2.7.0-release.apk", parsed.apkFileName)
        assertEquals(19293798L, parsed.apkSizeBytes)
        assertEquals("18.4 MB", parsed.formattedSize)
        assertEquals(2, parsed.changelogLines.size)

        val targetApk = manager.getTargetApkFile()
        assertEquals("AuraPlayer-update.apk", targetApk.name)
    }

    @Test
    fun `default settings enable skip intro button and autoplay next episode`() {
        val defaults = AppSettingsState()
        assertTrue(defaults.showSkipIntroButton)
        assertTrue(defaults.autoPlayNextEpisode)
    }

    @Test
    fun `thumbnail cache manager preserves portrait and landscape aspect ratios and uses v2 cache key`() {
        assertEquals(
            540 to 960,
            ThumbnailCacheManager.computeTargetDimensions(1080, 1920, 0)
        )
        assertEquals(
            540 to 960,
            ThumbnailCacheManager.computeTargetDimensions(1920, 1080, 90)
        )
        assertEquals(
            640 to 360,
            ThumbnailCacheManager.computeTargetDimensions(3840, 2160, 0)
        )
        val key = VideoThumbnailKey(
            uri = Uri.parse("content://media/external/video/media/42"),
            filePath = "/storage/emulated/0/Camera/VID_20261117.mp4",
            durationMs = 15_000L,
            dateModifiedSec = 1700000000L,
            width = 1080,
            height = 1920
        )
        assertTrue(key.stableDiskKey.endsWith("_v2"))
    }
}


