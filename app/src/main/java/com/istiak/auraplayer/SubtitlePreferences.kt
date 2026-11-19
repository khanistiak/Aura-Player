package com.istiak.auraplayer

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.subtitleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aura_player_subtitle_preferences"
)

enum class SubtitleAlignment(val label: String, val storedKey: String) {
    CENTER("Center", "center"),
    LEFT("Left", "left"),
    RIGHT("Right", "right");

    fun toTextAlign(): TextAlign = when (this) {
        CENTER -> TextAlign.Center
        LEFT -> TextAlign.Start
        RIGHT -> TextAlign.End
    }

    companion object {
        fun fromStored(value: String?): SubtitleAlignment {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.storedKey.equals(value, ignoreCase = true)
            } ?: CENTER
        }
    }
}

enum class SubtitleFontFamilyOption(val label: String, val storedKey: String) {
    DEFAULT("Default", "default"),
    SANS_SERIF("Sans-serif", "sans_serif"),
    SERIF("Serif", "serif"),
    MONOSPACE("Monospace", "monospace");

    fun toComposeFontFamily(): FontFamily = when (this) {
        DEFAULT -> FontFamily.Default
        SANS_SERIF -> FontFamily.SansSerif
        SERIF -> FontFamily.Serif
        MONOSPACE -> FontFamily.Monospace
    }

    fun toAndroidTypeface(isBold: Boolean): Typeface {
        val base = when (this) {
            DEFAULT -> Typeface.DEFAULT
            SANS_SERIF -> Typeface.SANS_SERIF
            SERIF -> Typeface.SERIF
            MONOSPACE -> Typeface.MONOSPACE
        }
        return Typeface.create(base, if (isBold) Typeface.BOLD else Typeface.NORMAL)
    }

    companion object {
        fun fromStored(value: String?): SubtitleFontFamilyOption {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.storedKey.equals(value, ignoreCase = true)
            } ?: DEFAULT
        }
    }
}

/**
 * Cinema-grade Subtitle Style & Layout Configuration persisted in DataStore (`SubtitlePreferences`).
 * - Default pure white text (`#FFFFFF`), transparent background (`backgroundEnabled = false`),
 *   dark drop shadow + black border outline (`borderEnabled = true`, `borderStrengthPercent = 80`),
 *   and `subtitleBottomMargin` standardized on the `0..150` integer scale (default `8`).
 */
@Immutable
data class SubtitleStyleConfig(
    val alignment: SubtitleAlignment = SubtitleAlignment.CENTER,
    val bottomMargin: Int = 8, // 0..150 unified scale (default 8)
    val backgroundEnabled: Boolean = false,
    val backgroundColorArgb: Int = AndroidColor.argb(140, 0, 0, 0),
    val fitIntoVideoSize: Boolean = true,
    val fontFamily: SubtitleFontFamilyOption = SubtitleFontFamilyOption.DEFAULT,
    val fontSizeSp: Int = 24, // 12..48, default 24
    val scalePercent: Int = 100, // 50%..200%, default 100%
    val textColorArgb: Int = AndroidColor.WHITE,
    val isBold: Boolean = false,
    val textBackgroundEnabled: Boolean = false,
    val textBackgroundColorArgb: Int = AndroidColor.argb(150, 0, 0, 0),
    val borderEnabled: Boolean = true,
    val borderColorArgb: Int = AndroidColor.BLACK,
    val borderStrengthPercent: Int = 80, // 0%..100%, default 80%
    val dropShadowEnabled: Boolean = true,
    val useEmbeddedAssStyles: Boolean = false
) {
    val effectiveTextSizeSp: Float
        get() = (fontSizeSp.coerceIn(12, 48) * (scalePercent.coerceIn(50, 200) / 100f))
            .coerceIn(10f, 84f)

    val hasBackground: Boolean
        get() = backgroundEnabled || textBackgroundEnabled

    val bottomMarginFraction: Float
        get() = (bottomMargin.coerceIn(0, 150) / 300f).coerceIn(0f, 0.50f)

    companion object {
        val Default = SubtitleStyleConfig()

        val PresetTextColors = listOf(
            AndroidColor.WHITE,
            AndroidColor.parseColor("#FACC15"), // Warm Cinema Yellow
            AndroidColor.parseColor("#38BDF8"), // Sky Cyan
            AndroidColor.parseColor("#4ADE80"), // Mint Green
            AndroidColor.parseColor("#FB7185"), // Coral Rose
            AndroidColor.parseColor("#C084FC")  // Soft Lavender
        )

        val PresetBackgroundColors = listOf(
            AndroidColor.argb(140, 0, 0, 0),
            AndroidColor.argb(180, 15, 23, 42),
            AndroidColor.argb(165, 30, 41, 59),
            AndroidColor.argb(160, 20, 83, 45),
            AndroidColor.argb(160, 30, 58, 138)
        )

        val PresetBorderColors = listOf(
            AndroidColor.BLACK,
            AndroidColor.parseColor("#0F172A"),
            AndroidColor.parseColor("#1E3A8A"),
            AndroidColor.parseColor("#334155"),
            AndroidColor.WHITE
        )
    }
}

class SubtitlePreferences(private val context: Context) {

    private object Keys {
        val ALIGNMENT = stringPreferencesKey("sub_alignment")
        val BOTTOM_MARGIN = intPreferencesKey("sub_bottom_margin_0_150")
        val BACKGROUND_ENABLED = booleanPreferencesKey("sub_bg_enabled")
        val BACKGROUND_COLOR = intPreferencesKey("sub_bg_color_argb")
        val FIT_INTO_VIDEO = booleanPreferencesKey("sub_fit_into_video")
        val FONT_FAMILY = stringPreferencesKey("sub_font_family")
        val FONT_SIZE_SP = intPreferencesKey("sub_font_size_sp")
        val SCALE_PERCENT = intPreferencesKey("sub_scale_percent")
        val TEXT_COLOR = intPreferencesKey("sub_text_color_argb")
        val IS_BOLD = booleanPreferencesKey("sub_is_bold")
        val TEXT_BG_ENABLED = booleanPreferencesKey("sub_text_bg_enabled")
        val TEXT_BG_COLOR = intPreferencesKey("sub_text_bg_color_argb")
        val BORDER_ENABLED = booleanPreferencesKey("sub_border_enabled")
        val BORDER_COLOR = intPreferencesKey("sub_border_color_argb")
        val BORDER_STRENGTH = intPreferencesKey("sub_border_strength_percent")
        val DROP_SHADOW_ENABLED = booleanPreferencesKey("sub_drop_shadow_enabled")
        val USE_EMBEDDED_ASS = booleanPreferencesKey("sub_use_embedded_ass")
    }

    val styleConfigFlow: Flow<SubtitleStyleConfig> = context.subtitleDataStore.data.map { prefs ->
        SubtitleStyleConfig(
            alignment = SubtitleAlignment.fromStored(prefs[Keys.ALIGNMENT]),
            bottomMargin = (prefs[Keys.BOTTOM_MARGIN] ?: 8).coerceIn(0, 150),
            backgroundEnabled = prefs[Keys.BACKGROUND_ENABLED] ?: false,
            backgroundColorArgb = prefs[Keys.BACKGROUND_COLOR] ?: AndroidColor.argb(140, 0, 0, 0),
            fitIntoVideoSize = prefs[Keys.FIT_INTO_VIDEO] ?: true,
            fontFamily = SubtitleFontFamilyOption.fromStored(prefs[Keys.FONT_FAMILY]),
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 24).coerceIn(12, 48),
            scalePercent = (prefs[Keys.SCALE_PERCENT] ?: 100).coerceIn(50, 200),
            textColorArgb = prefs[Keys.TEXT_COLOR] ?: AndroidColor.WHITE,
            isBold = prefs[Keys.IS_BOLD] ?: false,
            textBackgroundEnabled = prefs[Keys.TEXT_BG_ENABLED] ?: false,
            textBackgroundColorArgb = prefs[Keys.TEXT_BG_COLOR] ?: AndroidColor.argb(150, 0, 0, 0),
            borderEnabled = prefs[Keys.BORDER_ENABLED] ?: true,
            borderColorArgb = prefs[Keys.BORDER_COLOR] ?: AndroidColor.BLACK,
            borderStrengthPercent = (prefs[Keys.BORDER_STRENGTH] ?: 80).coerceIn(0, 100),
            dropShadowEnabled = prefs[Keys.DROP_SHADOW_ENABLED] ?: true,
            useEmbeddedAssStyles = prefs[Keys.USE_EMBEDDED_ASS] ?: false
        )
    }

    suspend fun saveStyleConfig(config: SubtitleStyleConfig) {
        context.subtitleDataStore.edit { prefs ->
            prefs[Keys.ALIGNMENT] = config.alignment.storedKey
            prefs[Keys.BOTTOM_MARGIN] = config.bottomMargin.coerceIn(0, 150)
            prefs[Keys.BACKGROUND_ENABLED] = config.backgroundEnabled
            prefs[Keys.BACKGROUND_COLOR] = config.backgroundColorArgb
            prefs[Keys.FIT_INTO_VIDEO] = config.fitIntoVideoSize
            prefs[Keys.FONT_FAMILY] = config.fontFamily.storedKey
            prefs[Keys.FONT_SIZE_SP] = config.fontSizeSp.coerceIn(12, 48)
            prefs[Keys.SCALE_PERCENT] = config.scalePercent.coerceIn(50, 200)
            prefs[Keys.TEXT_COLOR] = config.textColorArgb
            prefs[Keys.IS_BOLD] = config.isBold
            prefs[Keys.TEXT_BG_ENABLED] = config.textBackgroundEnabled
            prefs[Keys.TEXT_BG_COLOR] = config.textBackgroundColorArgb
            prefs[Keys.BORDER_ENABLED] = config.borderEnabled
            prefs[Keys.BORDER_COLOR] = config.borderColorArgb
            prefs[Keys.BORDER_STRENGTH] = config.borderStrengthPercent.coerceIn(0, 100)
            prefs[Keys.DROP_SHADOW_ENABLED] = config.dropShadowEnabled
            prefs[Keys.USE_EMBEDDED_ASS] = config.useEmbeddedAssStyles
        }
    }

    suspend fun saveBottomMargin(bottomMargin: Int) {
        context.subtitleDataStore.edit { prefs ->
            prefs[Keys.BOTTOM_MARGIN] = bottomMargin.coerceIn(0, 150)
        }
    }

    suspend fun resetToDefault() {
        saveStyleConfig(SubtitleStyleConfig.Default)
    }
}
