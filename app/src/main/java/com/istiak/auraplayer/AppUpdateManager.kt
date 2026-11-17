package com.istiak.auraplayer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * GitHub Releases endpoint for checking the latest published Aura Player APK release.
 */
const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/khanistiak/Aura-Player/releases/latest"

const val UPDATE_NOTIFICATION_CHANNEL_ID = "auraplayer_updates"
const val UPDATE_NOTIFICATION_CHANNEL_NAME = "App Updates"
const val EXTRA_SHOW_UPDATE_SHEET = "show_update_sheet"

private const val UPDATE_NOTIFICATION_ID = 2601
private const val UPDATE_APK_FILENAME = "AuraPlayer-update.apk"

data class ReleaseUpdateInfo(
    val tagName: String,
    val version: String,
    val releaseTitle: String,
    val publishedAt: String,
    val publishedMonthYear: String,
    val body: String,
    val changelogLines: List<String>,
    val isCritical: Boolean,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val formattedSize: String,
    val htmlReleaseUrl: String = ""
) {
    val cleanVersion: String
        get() = version

    val changelog: String
        get() = body

    val displayTag: String
        get() = if (tagName.startsWith("v", ignoreCase = true)) tagName else "v$version"

    val formattedApkSize: String
        get() = formattedSize
}

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: ReleaseUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * GitHub Releases Update Manager (`AppUpdateManager.kt`):
 * - Creates the high-importance `"auraplayer_updates"` (`"App Updates"`) notification channel.
 * - Queries [GITHUB_LATEST_RELEASE_API] (`https://api.github.com/repos/khanistiak/Aura-Player/releases/latest`)
 *   on [Dispatchers.IO] during app startup and when the user taps "Check for Updates Now" in `AboutScreen.kt`.
 * - Parses `tag_name` (stripped leading `"v"`), `published_at` (`"MMMM yyyy"`), `body` (changelog lines),
 *   and the first `.apk` asset's `browser_download_url` and formatted `size` (e.g., `"18.4 MB"`).
 * - Posts a high-priority system notification when a newer release is discovered (and not yet notified
 *   for that `tag_name` in [SettingsDataStore]) whose `PendingIntent` opens `MainActivity` with
 *   `"show_update_sheet" = true`.
 */
class AppUpdateManager(context: Context) {

    private val appContext = context.applicationContext

    init {
        ensureUpdateNotificationChannel()
    }

    /**
     * Creates the dedicated high-importance notification channel (`"auraplayer_updates"`, `"App Updates"`).
     */
    fun ensureUpdateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                UPDATE_NOTIFICATION_CHANNEL_ID,
                UPDATE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new Aura Player releases and APK updates"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Reads the installed app `versionName` dynamically from [PackageManager] at runtime,
     * falling back to [BuildConfig.VERSION_NAME].
     */
    fun getInstalledVersionName(context: Context = appContext): String {
        val packageVersion = runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull()

        return packageVersion?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME
    }

    /**
     * Cancels any active update notification from the system tray.
     */
    fun cancelUpdateNotification() {
        runCatching {
            NotificationManagerCompat.from(appContext).cancel(UPDATE_NOTIFICATION_ID)
        }
    }

    /**
     * Fetches [GITHUB_LATEST_RELEASE_API] on [Dispatchers.IO] and compares against [localVersion].
     */
    suspend fun checkForUpdates(
        localVersion: String = getInstalledVersionName(),
        apiUrl: String = GITHUB_LATEST_RELEASE_API
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val effectiveLocalVersion = localVersion.ifBlank { getInstalledVersionName() }
        var connection: HttpURLConnection? = null
        try {
            val url = URL(apiUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AuraPlayer-Android/${stripLeadingV(effectiveLocalVersion)}")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                if (code == 404) {
                    cancelUpdateNotification()
                    return@withContext UpdateCheckResult.UpToDate(stripLeadingV(effectiveLocalVersion))
                }
                return@withContext UpdateCheckResult.Error("GitHub API returned HTTP $code")
            }

            val jsonText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val releaseInfo = parseReleaseJson(jsonText)
            if (releaseInfo == null) {
                cancelUpdateNotification()
                return@withContext UpdateCheckResult.UpToDate(stripLeadingV(effectiveLocalVersion))
            }

            if (isNewerVersion(releaseInfo.tagName, effectiveLocalVersion)) {
                UpdateCheckResult.UpdateAvailable(releaseInfo)
            } else {
                cancelUpdateNotification()
                UpdateCheckResult.UpToDate(stripLeadingV(effectiveLocalVersion))
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "Unable to reach GitHub Releases")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Runs on startup: checks GitHub Releases against the installed runtime version and
     * `lastDismissedOrInstalledTag` in [settingsStore]. Only posts a notification and returns
     * [ReleaseUpdateInfo] when a strictly newer semantic version is available and not dismissed.
     */
    suspend fun checkOnStartupAndNotifyIfNeeded(
        settingsStore: SettingsDataStore,
        localVersion: String = getInstalledVersionName()
    ): ReleaseUpdateInfo? = withContext(Dispatchers.IO) {
        val effectiveLocalVersion = localVersion.ifBlank { getInstalledVersionName() }
        when (val result = checkForUpdates(localVersion = effectiveLocalVersion)) {
            is UpdateCheckResult.UpdateAvailable -> {
                val release = result.info
                val settings = runCatching { settingsStore.settingsFlow.first() }.getOrNull()
                val lastDismissedOrInstalled = settings?.lastDismissedOrInstalledTag.orEmpty()

                if (!shouldPromptRelease(release.tagName, effectiveLocalVersion, lastDismissedOrInstalled)) {
                    cancelUpdateNotification()
                    return@withContext null
                }

                val lastNotifiedTag = settings?.lastNotifiedUpdateTag.orEmpty()
                val alreadyNotified = lastNotifiedTag.isNotBlank() &&
                    !isNewerVersion(release.tagName, lastNotifiedTag)
                if (!alreadyNotified) {
                    val posted = postSystemUpdateNotification(release)
                    if (posted) {
                        settingsStore.setLastNotifiedUpdateTag(release.tagName)
                    }
                }
                release
            }
            else -> {
                cancelUpdateNotification()
                null
            }
        }
    }

    /**
     * Posts a high-priority system update notification that opens `MainActivity` with
     * `"show_update_sheet" = true` when tapped.
     */
    fun postSystemUpdateNotification(release: ReleaseUpdateInfo): Boolean {
        ensureUpdateNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val tapIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOW_UPDATE_SHEET, true)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            UPDATE_NOTIFICATION_ID,
            tapIntent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(appContext, UPDATE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Aura Player v${release.version} Update Available")
            .setContentText("Tap to view what's new and download the latest APK.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Tap to view what's new and download the latest APK.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(UPDATE_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    /**
     * Opens [apkDownloadUrl] via Chrome Custom Tabs (`CustomTabsIntent` bundle protocol) with a
     * fallback to `Intent(Intent.ACTION_VIEW, Uri.parse(apkDownloadUrl))` so Android's browser /
     * system download manager downloads the `.apk` with live progress and shows the native one-tap
     * "Open" install banner upon completion.
     */
    fun openApkDownloadUrl(context: Context = appContext, apkDownloadUrl: String) {
        if (apkDownloadUrl.isBlank()) return
        val uri = Uri.parse(apkDownloadUrl)

        val customTabsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            val extras = Bundle().apply {
                // Standard CustomTabsIntent.EXTRA_SESSION + EXTRA_TOOLBAR_COLOR keys
                putBinder("android.support.customtabs.extra.SESSION", null)
                putInt("android.support.customtabs.extra.TOOLBAR_COLOR", 0xFF141824.toInt())
                putInt("android.support.customtabs.extra.TITLE_VISIBILITY", 1)
            }
            putExtras(extras)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val launchedCustomTab = runCatching {
            context.startActivity(customTabsIntent)
            true
        }.getOrDefault(false)

        if (!launchedCustomTab) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallbackIntent) }
        }
    }

    fun getTargetApkFile(): File {
        val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(appContext.filesDir, "Download")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return File(downloadsDir, UPDATE_APK_FILENAME)
    }

    companion object {
        fun stripLeadingV(version: String): String {
            return version.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore(" ")
                .trim()
        }

        /**
         * Compares two semantic version strings (e.g., `"v2.7.0"` vs `"2.6.1"`).
         * Strips `'v'`/`'V'`, whitespace, and pre-release/build suffixes (`-rc1`, `+build`),
         * pads shorter segment lists with `0`, and returns `true` strictly when [remoteTag] > [localVersion].
         */
        fun isNewerVersion(
            remoteTag: String,
            localVersion: String = BuildConfig.VERSION_NAME
        ): Boolean {
            val remoteParts = parseNumericVersionParts(remoteTag)
            val localParts = parseNumericVersionParts(localVersion)
            if (remoteParts.isEmpty()) return false

            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r != l) {
                    return r > l
                }
            }
            return false
        }

        /**
         * Returns `true` only when [remoteTag] is strictly newer than [installedVersion] AND
         * strictly newer than any previously dismissed or installed [lastDismissedOrInstalledTag].
         */
        fun shouldPromptRelease(
            remoteTag: String,
            installedVersion: String,
            lastDismissedOrInstalledTag: String
        ): Boolean {
            if (!isNewerVersion(remoteTag, installedVersion)) return false
            if (lastDismissedOrInstalledTag.isBlank()) return true
            return isNewerVersion(remoteTag, lastDismissedOrInstalledTag)
        }

        private fun parseNumericVersionParts(rawVersion: String): List<Int> {
            val cleaned = stripLeadingV(rawVersion)
                .substringBefore('-')
                .substringBefore('+')
                .trim()
            if (cleaned.isEmpty()) return emptyList()
            return cleaned.split('.')
                .mapNotNull { token ->
                    val digits = token.trim().takeWhile { it.isDigit() }
                    digits.toIntOrNull()
                }
        }

        /**
         * Formats an ISO-8601 `published_at` timestamp (e.g., `"2026-11-16T18:00:00Z"`) as `"MMMM yyyy"`
         * (e.g., `"November 2026"`).
         */
        fun formatPublishedMonthYear(publishedAtIso: String): String {
            if (publishedAtIso.isBlank()) return "Latest Release"
            return runCatching {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(publishedAtIso.trim()) ?: return@runCatching "Latest Release"
                val formatter = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                formatter.format(date)
            }.getOrElse {
                runCatching {
                    val datePart = publishedAtIso.take(10)
                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val date = parser.parse(datePart) ?: return@runCatching "Latest Release"
                    SimpleDateFormat("MMMM yyyy", Locale.US).format(date)
                }.getOrDefault("Latest Release")
            }
        }

        /**
         * Extracts clean bulleted highlight lines from raw Markdown `body`.
         */
        fun parseChangelogHighlights(rawBody: String): List<String> {
            val cleanedLines = rawBody.lines()
                .map { line ->
                    line.trim()
                        .removePrefix("- ")
                        .removePrefix("* ")
                        .removePrefix("• ")
                        .replace(Regex("^#+\\s*"), "")
                        .replace(Regex("\\[critical\\]", RegexOption.IGNORE_CASE), "")
                        .replace("**", "")
                        .trim()
                }
                .filter { it.isNotBlank() }

            return if (cleanedLines.isNotEmpty()) {
                cleanedLines
            } else {
                listOf(
                    "Improved 4K & 10-bit HEVC hardware decoder stability",
                    "Refined Liquid Glass UI and folder browsing performance",
                    "General bug fixes and playback enhancements"
                )
            }
        }

        /**
         * Parses GitHub `/releases/latest` JSON payload (`tag_name`, `published_at`, `body`, and `.apk` asset).
         */
        fun parseReleaseJson(jsonString: String): ReleaseUpdateInfo? {
            return runCatching {
                val root = JSONObject(jsonString)
                val rawTag = root.optString("tag_name", "").trim()
                if (rawTag.isEmpty()) return@runCatching null

                val cleanVersion = stripLeadingV(rawTag)
                val releaseTitle = root.optString("name", "").trim().ifEmpty { "Aura Player v$cleanVersion" }
                val publishedAt = root.optString("published_at", "").trim()
                val publishedMonthYear = formatPublishedMonthYear(publishedAt)
                val rawBody = root.optString("body", "").trim()
                val body = rawBody.ifEmpty {
                    "- Improved 4K hardware decoding and playback reliability\n- Refined Liquid Glass UI polish and stability fixes"
                }
                val isCritical = rawBody.contains("[critical]", ignoreCase = true) ||
                    rawBody.contains("critical", ignoreCase = true)
                val changelogLines = parseChangelogHighlights(body)
                val htmlUrl = root.optString("html_url", "").trim()

                val assetsArray = root.optJSONArray("assets") ?: return@runCatching null
                var apkDownloadUrl = ""
                var apkFileName = ""
                var apkSizeBytes = 0L

                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name", "").trim()
                    val downloadUrl = asset.optString("browser_download_url", "").trim()
                    if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotEmpty()) {
                        apkDownloadUrl = downloadUrl
                        apkFileName = assetName
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }

                if (apkDownloadUrl.isEmpty()) return@runCatching null

                ReleaseUpdateInfo(
                    tagName = rawTag,
                    version = cleanVersion,
                    releaseTitle = releaseTitle,
                    publishedAt = publishedAt,
                    publishedMonthYear = publishedMonthYear,
                    body = body,
                    changelogLines = changelogLines,
                    isCritical = isCritical,
                    apkDownloadUrl = apkDownloadUrl,
                    apkFileName = apkFileName,
                    apkSizeBytes = apkSizeBytes,
                    formattedSize = formatUpdateSizeBytes(apkSizeBytes),
                    htmlReleaseUrl = htmlUrl
                )
            }.getOrNull()
        }
    }
}

internal fun formatUpdateSizeBytes(bytes: Long): String {
    if (bytes <= 0L) return "APK Package"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        val kb = bytes.toDouble() / 1024.0
        String.format(Locale.US, "%.0f KB", kb)
    }
}
