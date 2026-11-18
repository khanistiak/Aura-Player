package com.istiak.auraplayer

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.istiak.auraplayer.ui.theme.AuraPlayerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FileDialogsRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleVideo(id: Long, title: String, displayName: String, relPath: String = "DCIM/Camera") = VideoItem(
        id = id,
        uri = Uri.parse("content://media/external/video/media/$id"),
        title = title,
        displayName = displayName,
        durationMs = 10_000L,
        sizeBytes = 5_000_000L,
        width = 1920,
        height = 1080,
        folderName = relPath.substringAfterLast('/'),
        relativePath = relPath,
        dateAddedSec = 1_700_000_000L
    )

    @Test
    fun `delete confirmation dialog requires explicit confirmation before deleting`() {
        val v1 = sampleVideo(1L, "Sunset Clip", "sunset_trip.mp4")
        var confirmed = false
        var dismissed = false

        composeTestRule.setContent {
            AuraPlayerTheme {
                DeleteConfirmationDialog(
                    videos = listOf(v1),
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("delete_confirmation_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete video?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_confirm_cancel_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
        assertFalse(confirmed)

        composeTestRule.onNodeWithTag("delete_confirm_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(confirmed)
    }

    @Test
    fun `delete confirmation dialog shows plural title and count for multi-select`() {
        val videos = listOf(
            sampleVideo(1L, "One", "one.mp4"),
            sampleVideo(2L, "Two", "two.mp4"),
            sampleVideo(3L, "Three", "three.mp4")
        )

        composeTestRule.setContent {
            AuraPlayerTheme {
                DeleteConfirmationDialog(
                    videos = videos,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Delete 3 videos?").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 selected videos will be permanently removed from your device.")
            .assertIsDisplayed()
    }

    @Test
    fun `move to vault confirmation dialog displays single and multi-select copy`() {
        val v1 = sampleVideo(1L, "Private Trip", "private_trip.mp4")
        var confirmed = false

        composeTestRule.setContent {
            AuraPlayerTheme {
                MoveToVaultConfirmationDialog(
                    videos = listOf(v1),
                    onConfirm = { confirmed = true },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("move_to_vault_confirmation_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Move to Private Vault?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Hide \"Private Trip\" in your PIN/biometric-protected Private Vault? It will be removed from public folders and gallery."
        ).assertIsDisplayed()

        composeTestRule.onNodeWithTag("vault_confirm_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(confirmed)
    }

    @Test
    fun `rename file dialog preserves extension, validates invalid chars, and confirms valid name`() {
        val v1 = sampleVideo(10L, "Holiday", "holiday_reel.mkv")
        var renamedTo: String? = null

        composeTestRule.setContent {
            AuraPlayerTheme {
                RenameFileDialog(
                    video = v1,
                    onDismiss = {},
                    onConfirmRename = { renamedTo = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("rename_file_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rename video").assertIsDisplayed()
        composeTestRule.onNodeWithText(".mkv").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rename_file_confirm_button").assertIsEnabled()

        // Invalid character disables Rename
        composeTestRule.onNodeWithTag("rename_file_input").performTextClearance()
        composeTestRule.onNodeWithTag("rename_file_input").performTextInput("bad/name")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("rename_file_confirm_button").assertIsNotEnabled()

        // Clear button resets and disables Rename
        composeTestRule.onNodeWithTag("rename_clear_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("rename_file_confirm_button").assertIsNotEnabled()

        // Valid name enables Rename and delivers trimmed name
        composeTestRule.onNodeWithTag("rename_file_input").performTextInput("  winter_reel  ")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("rename_file_confirm_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("rename_file_confirm_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("winter_reel", renamedTo)
    }

    @Test
    fun `select folder dialog displays mode header, breadcrumbs, and Move Here action`() {
        var selectedDestination: String? = null

        composeTestRule.setContent {
            AuraPlayerTheme {
                SelectFolderDialog(
                    actionLabel = "Move",
                    itemCount = 3,
                    allDirectories = listOf("DCIM", "DCIM/Camera", "Movies", "Movies/4K"),
                    sourceRelativePaths = setOf("DCIM/Camera"),
                    onCreateFolder = {},
                    onSelectFolder = { selectedDestination = it },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("select_folder_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Move 3 items to...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Internal Storage").assertIsDisplayed()
        composeTestRule.onNodeWithText("Move Here").assertIsDisplayed()
        composeTestRule.onNodeWithTag("folder_picker_confirm_button").assertIsNotEnabled()

        // Drill into Movies -> 4K and confirm
        composeTestRule.onNodeWithTag("folder_picker_item_Movies").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("folder_picker_confirm_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("folder_picker_item_4K").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("folder_picker_confirm_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Movies/4K", selectedDestination)
    }

    @Test
    fun `add to playlist dialog shows single Create New Playlist button, filters list, and confirms selection`() {
        val playlists = listOf(
            PlaylistItem(id = PlaylistRepository.FAVORITES_ID, name = "Favorites", videoIds = listOf(1L, 2L)),
            PlaylistItem(id = "action_4k", name = "Action 4K", videoIds = listOf(3L))
        )
        var addedToIds: Set<String> = emptySet()

        composeTestRule.setContent {
            AuraPlayerTheme {
                AddToPlaylistSheet(
                    videoCount = 2,
                    playlists = playlists,
                    onCreatePlaylist = {},
                    onAddVideosToPlaylists = { addedToIds = it },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_to_playlist_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add to Playlist").assertIsDisplayed()
        composeTestRule.onNodeWithText("Adding 2 videos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create New Playlist").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_add_to_playlist_button").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("search_playlists_input").performTextInput("Action")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_item_action_4k").assertIsDisplayed()

        composeTestRule.onNodeWithTag("clear_playlist_search_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_item_${PlaylistRepository.FAVORITES_ID}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_add_to_playlist_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("confirm_add_to_playlist_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(setOf(PlaylistRepository.FAVORITES_ID), addedToIds)
    }
}
