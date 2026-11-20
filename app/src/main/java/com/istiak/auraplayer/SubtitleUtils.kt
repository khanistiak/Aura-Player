package com.istiak.auraplayer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Prepared local external subtitle ready to be passed via `Uri.fromFile(localFile)` and explicit
 * Media3 `MimeTypes` to `MediaItem.SubtitleConfiguration.Builder`.
 */
data class PreparedExternalSubtitle(
    val localFile: File,
    val uri: Uri,
    val mimeType: String,
    val label: String
)

/**
 * Utilities for picking, extracting (`.zip`), normalizing (BOM stripping + clean UTF-8 conversion),
 * and staging external `.srt`, `.ass`, `.ssa`, and `.vtt` subtitle files in
 * `context.filesDir/subtitles/` so Media3/ExoPlayer loads them reliably.
 */
object SubtitleUtils {

    private const val SUBTITLES_DIR_NAME = "subtitles"
    private const val MAX_SUBTITLE_BYTES = 16 * 1024 * 1024 // 16 MB safety guard

    val SUPPORTED_SUBTITLE_EXTENSIONS: Set<String> = setOf("srt", "ass", "ssa", "vtt")

    /**
     * MIME types passed to `ActivityResultContracts.OpenDocument()` so `.srt`, `.ass`, `.ssa`,
     * `.vtt`, and `.zip` files can all be selected, with a wildcard fallback for providers that
     * report generic or unknown MIME types.
     */
    val PICKER_MIME_TYPES: Array<String> = arrayOf(
        "application/x-subrip",
        "text/vtt",
        "text/x-ssa",
        "text/x-ass",
        "text/plain",
        "application/zip",
        "application/x-zip-compressed",
        "application/octet-stream",
        "*/*"
    )

    fun subtitlesDirectory(context: Context): File {
        return File(context.filesDir, SUBTITLES_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Resolves an explicit Media3 MIME type (`APPLICATION_SUBRIP`, `TEXT_SSA`, or `TEXT_VTT`) from
     * the filename extension, falling back to content sniffing if the extension is missing/unknown.
     */
    fun mimeTypeForFilename(fileName: String, contentPreview: String? = null): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "vtt" -> MimeTypes.TEXT_VTT
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            else -> detectMimeTypeFromContent(contentPreview) ?: MimeTypes.APPLICATION_SUBRIP
        }
    }

    fun detectMimeTypeFromContent(content: String?): String? {
        if (content.isNullOrBlank()) return null
        val trimmed = content.trimStart('\uFEFF').trimStart()
        return when {
            trimmed.startsWith("WEBVTT", ignoreCase = true) -> MimeTypes.TEXT_VTT
            trimmed.contains("[Script Info]", ignoreCase = true) ||
                trimmed.contains("Dialogue:", ignoreCase = false) -> MimeTypes.TEXT_SSA
            trimmed.contains("-->") -> MimeTypes.APPLICATION_SUBRIP
            else -> null
        }
    }

    fun extensionForMimeType(mimeType: String): String = when (mimeType) {
        MimeTypes.TEXT_SSA -> "ass"
        MimeTypes.TEXT_VTT -> "vtt"
        else -> "srt"
    }

    /**
     * Builds a `MediaItem.SubtitleConfiguration` using `Uri.fromFile(localFile)` and explicit
     * Media3 `MimeTypes` (`APPLICATION_SUBRIP`, `TEXT_SSA`, or `TEXT_VTT`).
     */
    fun buildSubtitleConfiguration(
        uri: Uri,
        mimeType: String,
        label: String,
        id: String? = null,
        language: String = "en",
        isDefault: Boolean = true
    ): MediaItem.SubtitleConfiguration {
        return MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage(language)
            .setLabel(label)
            .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
            .setSelectionFlags(if (isDefault) C.SELECTION_FLAG_DEFAULT else 0)
            .apply {
                if (!id.isNullOrBlank()) setId(id)
            }
            .build()
    }

    /**
     * Copies (or extracts from `.zip`) the user-selected external subtitle `Uri` into
     * `context.filesDir/subtitles/` as clean UTF-8 (BOM stripped, UTF-16/legacy encodings converted
     * to UTF-8) and returns a `PreparedExternalSubtitle` with `Uri.fromFile(localFile)` and explicit
     * Media3 `MimeTypes`.
     */
    suspend fun prepareExternalSubtitle(
        context: Context,
        sourceUri: Uri
    ): Result<PreparedExternalSubtitle> = withContext(Dispatchers.IO) {
        runCatching {
            val resolvedName = resolveDisplayName(context, sourceUri)
            val resolvedMime = runCatching { context.contentResolver.getType(sourceUri) }.getOrNull()
            val rawInputBytes = readBytesFromUri(context, sourceUri)
            require(rawInputBytes.isNotEmpty()) { "Selected subtitle file is empty" }

            val isZip = isZipSource(resolvedName, resolvedMime, rawInputBytes)
            val (entryName, subtitleBytes) = if (isZip) {
                extractFirstSubtitleEntryFromZip(rawInputBytes)
            } else {
                resolvedName to rawInputBytes
            }

            val cleanUtf8Text = decodeToCleanUtf8(subtitleBytes)
            require(cleanUtf8Text.isNotBlank()) { "Subtitle file contains no readable text" }

            val ext = entryName.substringAfterLast('.', "").lowercase(Locale.US)
            val explicitMime = when (ext) {
                "ass", "ssa" -> MimeTypes.TEXT_SSA
                "vtt" -> MimeTypes.TEXT_VTT
                "srt" -> MimeTypes.APPLICATION_SUBRIP
                else -> detectMimeTypeFromContent(cleanUtf8Text)
                    ?: throw IllegalArgumentException(
                        "Unsupported subtitle format. Pick a .srt, .ass, .ssa, .vtt, or .zip file."
                    )
            }

            val finalExt = if (ext in SUPPORTED_SUBTITLE_EXTENSIONS) ext else extensionForMimeType(explicitMime)
            val targetDir = subtitlesDirectory(context)
            val targetFileName = buildSafeSubtitleFileName(entryName, finalExt)
            val localFile = File(targetDir, targetFileName)

            // Write strictly as clean UTF-8 without BOM
            localFile.writeBytes(cleanUtf8Text.toByteArray(Charsets.UTF_8))

            PreparedExternalSubtitle(
                localFile = localFile,
                uri = Uri.fromFile(localFile),
                mimeType = explicitMime,
                label = entryName.substringAfterLast('/').substringAfterLast('\\').ifBlank { targetFileName }
            )
        }
    }

    /**
     * Extracts the first `.srt`, `.ass`, `.ssa`, or `.vtt` entry from raw ZIP bytes using
     * `ZipInputStream`, skipping directories and macOS `__MACOSX` / `._` metadata entries.
     */
    internal fun extractFirstSubtitleEntryFromZip(zipBytes: ByteArray): Pair<String, ByteArray> {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val rawEntryName = entry.name
                val baseName = rawEntryName.substringAfterLast('/').substringAfterLast('\\')
                val isMetadataEntry = rawEntryName.startsWith("__MACOSX/", ignoreCase = true) ||
                    rawEntryName.contains("/__MACOSX/", ignoreCase = true) ||
                    baseName.startsWith("._")

                if (!entry.isDirectory && !isMetadataEntry) {
                    val ext = baseName.substringAfterLast('.', "").lowercase(Locale.US)
                    if (ext in SUPPORTED_SUBTITLE_EXTENSIONS) {
                        val extracted = readBoundedBytes(zis)
                        zis.closeEntry()
                        if (extracted.isNotEmpty()) {
                            return baseName to extracted
                        }
                    }
                }
                zis.closeEntry()
            }
        }
        throw IllegalArgumentException("No .srt, .ass, .ssa, or .vtt subtitle found inside ZIP archive")
    }

    /**
     * Strips UTF-8 / UTF-16 / UTF-32 BOMs if present, converts UTF-16 / legacy Windows-1252 bytes
     * into clean UTF-8 text without any leading U+FEFF character.
     */
    internal fun decodeToCleanUtf8(rawBytes: ByteArray): String {
        if (rawBytes.isEmpty()) return ""

        val decoded = when {
            // UTF-32 BE BOM: 00 00 FE FF
            rawBytes.size >= 4 &&
                rawBytes[0] == 0x00.toByte() &&
                rawBytes[1] == 0x00.toByte() &&
                rawBytes[2] == 0xFE.toByte() &&
                rawBytes[3] == 0xFF.toByte() -> {
                String(rawBytes, 4, rawBytes.size - 4, Charset.forName("UTF-32BE"))
            }
            // UTF-32 LE BOM: FF FE 00 00
            rawBytes.size >= 4 &&
                rawBytes[0] == 0xFF.toByte() &&
                rawBytes[1] == 0xFE.toByte() &&
                rawBytes[2] == 0x00.toByte() &&
                rawBytes[3] == 0x00.toByte() -> {
                String(rawBytes, 4, rawBytes.size - 4, Charset.forName("UTF-32LE"))
            }
            // UTF-8 BOM: EF BB BF
            rawBytes.size >= 3 &&
                rawBytes[0] == 0xEF.toByte() &&
                rawBytes[1] == 0xBB.toByte() &&
                rawBytes[2] == 0xBF.toByte() -> {
                decodeUtf8OrFallback(rawBytes, 3, rawBytes.size - 3)
            }
            // UTF-16 LE BOM: FF FE
            rawBytes.size >= 2 &&
                rawBytes[0] == 0xFF.toByte() &&
                rawBytes[1] == 0xFE.toByte() -> {
                String(rawBytes, 2, rawBytes.size - 2, Charsets.UTF_16LE)
            }
            // UTF-16 BE BOM: FE FF
            rawBytes.size >= 2 &&
                rawBytes[0] == 0xFE.toByte() &&
                rawBytes[1] == 0xFF.toByte() -> {
                String(rawBytes, 2, rawBytes.size - 2, Charsets.UTF_16BE)
            }
            else -> {
                decodeUtf8OrFallback(rawBytes, 0, rawBytes.size)
            }
        }

        // Ensure no stray leading BOM character remains
        return decoded.trimStart('\uFEFF')
    }

    private fun decodeUtf8OrFallback(bytes: ByteArray, offset: Int, length: Int): String {
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, offset, length))
                .toString()
        } catch (_: Exception) {
            val fallbackCharset = runCatching { Charset.forName("windows-1252") }
                .getOrDefault(Charsets.ISO_8859_1)
            String(bytes, offset, length, fallbackCharset)
        }
    }

    private fun isZipSource(fileName: String, mimeType: String?, rawBytes: ByteArray): Boolean {
        if (fileName.endsWith(".zip", ignoreCase = true)) return true
        if (mimeType != null && (
                mimeType.equals("application/zip", ignoreCase = true) ||
                    mimeType.equals("application/x-zip-compressed", ignoreCase = true)
                )
        ) {
            return true
        }
        // Check ZIP local file header (PK\x03\x04) or empty archive EOCD (PK\x05\x06)
        return rawBytes.size >= 4 &&
            rawBytes[0] == 0x50.toByte() &&
            rawBytes[1] == 0x4B.toByte() &&
            ((rawBytes[2] == 0x03.toByte() && rawBytes[3] == 0x04.toByte()) ||
                (rawBytes[2] == 0x05.toByte() && rawBytes[3] == 0x06.toByte()))
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val fileName = uri.path?.let { File(it).name }
            if (!fileName.isNullOrBlank()) return fileName
        }

        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) return displayName
                    }
                }
            }
        }

        val segment = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }

        return segment ?: "external_subtitle.srt"
    }

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val file = uri.path?.let { File(it) }
            if (file != null && file.exists()) {
                return file.inputStream().use { readBoundedBytes(it) }
            }
        }
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open selected subtitle file")
        return input.use { readBoundedBytes(it) }
    }

    private fun readBoundedBytes(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > MAX_SUBTITLE_BYTES) {
                throw IllegalArgumentException("Subtitle file exceeds 16 MB limit")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun buildSafeSubtitleFileName(rawName: String, extension: String): String {
        val justName = rawName.substringAfterLast('/').substringAfterLast('\\')
        val stem = justName.substringBeforeLast('.', justName)
            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .trim('_')
            .ifBlank { "subtitle_${System.currentTimeMillis()}" }
            .take(96)
        return "$stem.$extension"
    }
}
