package com.istiak.auraplayer

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.liquidGlassDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aura_player_liquid_glass"
)

enum class HighlightStyle(
    val label: String,
    val storedKey: String
) {
    COMPONENT_DEFAULT("Component default", "component_default"),
    SPECULAR_RIM("Specular rim", "specular_rim"),
    TOP_SHEEN("Top sheen", "top_sheen"),
    MINIMAL("Minimal", "minimal");

    companion object {
        fun fromStored(value: String?): HighlightStyle {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.storedKey.equals(value, ignoreCase = true)
            } ?: COMPONENT_DEFAULT
        }
    }
}

@Immutable
data class LiquidGlassConfig(
    val enabled: Boolean = true,
    val headerLiquidGlassEnabled: Boolean = true,
    val hdrModeEnabled: Boolean = true,
    val frostPercent: Float = 0.20f,
    val backdropOpacity: Float = 1.00f,
    val refractionEdgeWidth: Float = 1.00f,
    val refractionStrength: Float = 1.00f,
    val depthEffect: Boolean = false,
    val chromaticAberration: Boolean = true,
    val vibrancyEnabled: Boolean = true,
    val saturation: Float = 1.00f,
    val brightness: Float = 0.00f,
    val contrast: Float = 1.00f,
    val highlightStyle: HighlightStyle = HighlightStyle.COMPONENT_DEFAULT,
    val highlightStrength: Float = 1.00f,
    val highlightWidth: Float = 1.00f,
    val highlightSoftness: Float = 1.00f
) {
    /**
     * Maps `frostPercent` (`0f..2f`, default `0.20f`) dynamically to blur radius (`0.dp..56.dp`,
     * so `20%` yields `5.6.dp` crystal-clear optical frost).
     */
    fun effectiveBlurDp(maxBlurDp: Dp = 28.dp): Dp {
        if (!enabled) return 0.dp
        val clampedFrost = frostPercent.coerceIn(0f, 2f)
        return (maxBlurDp.value * clampedFrost).dp
    }

    fun effectiveHighlightStrokeDp(): Dp {
        val factor = highlightWidth.coerceIn(0f, 2f)
        return (0.5f + 1.0f * factor).coerceIn(0.4f, 2.5f).dp
    }

    companion object {
        val Default = LiquidGlassConfig()
    }
}

val LocalLiquidGlassConfig = compositionLocalOf { LiquidGlassConfig.Default }

class LiquidGlassPreferences(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("lg_enabled")
        val HEADER_LIQUID_GLASS_ENABLED = booleanPreferencesKey("lg_header_enabled")
        val HDR_MODE_ENABLED = booleanPreferencesKey("lg_hdr_mode_enabled")
        val FROST_PERCENT = floatPreferencesKey("lg_frost_percent")
        val BACKDROP_OPACITY = floatPreferencesKey("lg_backdrop_opacity")
        val REFRACTION_EDGE_WIDTH = floatPreferencesKey("lg_refraction_edge_width")
        val REFRACTION_STRENGTH = floatPreferencesKey("lg_refraction_strength")
        val DEPTH_EFFECT = booleanPreferencesKey("lg_depth_effect")
        val CHROMATIC_ABERRATION = booleanPreferencesKey("lg_chromatic_aberration")
        val VIBRANCY_ENABLED = booleanPreferencesKey("lg_vibrancy_enabled")
        val SATURATION = floatPreferencesKey("lg_saturation")
        val BRIGHTNESS = floatPreferencesKey("lg_brightness")
        val CONTRAST = floatPreferencesKey("lg_contrast")
        val HIGHLIGHT_STYLE = stringPreferencesKey("lg_highlight_style")
        val HIGHLIGHT_STRENGTH = floatPreferencesKey("lg_highlight_strength")
        val HIGHLIGHT_WIDTH = floatPreferencesKey("lg_highlight_width")
        val HIGHLIGHT_SOFTNESS = floatPreferencesKey("lg_highlight_softness")
    }

    val configFlow: Flow<LiquidGlassConfig> = context.liquidGlassDataStore.data.map { prefs ->
        LiquidGlassConfig(
            enabled = prefs[Keys.ENABLED] ?: true,
            headerLiquidGlassEnabled = prefs[Keys.HEADER_LIQUID_GLASS_ENABLED] ?: true,
            hdrModeEnabled = prefs[Keys.HDR_MODE_ENABLED] ?: true,
            frostPercent = (prefs[Keys.FROST_PERCENT] ?: 0.20f).coerceIn(0f, 2f),
            backdropOpacity = (prefs[Keys.BACKDROP_OPACITY] ?: 1.00f).coerceIn(0f, 1f),
            refractionEdgeWidth = (prefs[Keys.REFRACTION_EDGE_WIDTH] ?: 1.00f).coerceIn(0f, 2f),
            refractionStrength = (prefs[Keys.REFRACTION_STRENGTH] ?: 1.00f).coerceIn(0f, 2f),
            depthEffect = prefs[Keys.DEPTH_EFFECT] ?: false,
            chromaticAberration = prefs[Keys.CHROMATIC_ABERRATION] ?: true,
            vibrancyEnabled = prefs[Keys.VIBRANCY_ENABLED] ?: true,
            saturation = (prefs[Keys.SATURATION] ?: 1.00f).coerceIn(0f, 2f),
            brightness = (prefs[Keys.BRIGHTNESS] ?: 0.00f).coerceIn(-0.50f, 0.50f),
            contrast = (prefs[Keys.CONTRAST] ?: 1.00f).coerceIn(0.50f, 1.50f),
            highlightStyle = HighlightStyle.fromStored(prefs[Keys.HIGHLIGHT_STYLE]),
            highlightStrength = (prefs[Keys.HIGHLIGHT_STRENGTH] ?: 1.00f).coerceIn(0f, 2f),
            highlightWidth = (prefs[Keys.HIGHLIGHT_WIDTH] ?: 1.00f).coerceIn(0f, 2f),
            highlightSoftness = (prefs[Keys.HIGHLIGHT_SOFTNESS] ?: 1.00f).coerceIn(0f, 2f)
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.liquidGlassDataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setHeaderLiquidGlassEnabled(enabled: Boolean) {
        context.liquidGlassDataStore.edit { it[Keys.HEADER_LIQUID_GLASS_ENABLED] = enabled }
    }

    suspend fun setHdrModeEnabled(enabled: Boolean) {
        context.liquidGlassDataStore.edit { it[Keys.HDR_MODE_ENABLED] = enabled }
    }

    suspend fun setFrostPercent(percent: Float) {
        context.liquidGlassDataStore.edit { it[Keys.FROST_PERCENT] = percent.coerceIn(0f, 2f) }
    }

    suspend fun setBackdropOpacity(opacity: Float) {
        context.liquidGlassDataStore.edit { it[Keys.BACKDROP_OPACITY] = opacity.coerceIn(0f, 1f) }
    }

    suspend fun setRefractionEdgeWidth(width: Float) {
        context.liquidGlassDataStore.edit { it[Keys.REFRACTION_EDGE_WIDTH] = width.coerceIn(0f, 2f) }
    }

    suspend fun setRefractionStrength(strength: Float) {
        context.liquidGlassDataStore.edit { it[Keys.REFRACTION_STRENGTH] = strength.coerceIn(0f, 2f) }
    }

    suspend fun setDepthEffect(enabled: Boolean) {
        context.liquidGlassDataStore.edit { it[Keys.DEPTH_EFFECT] = enabled }
    }

    suspend fun setChromaticAberration(enabled: Boolean) {
        context.liquidGlassDataStore.edit { it[Keys.CHROMATIC_ABERRATION] = enabled }
    }

    suspend fun setVibrancyEnabled(enabled: Boolean) {
        context.liquidGlassDataStore.edit { it[Keys.VIBRANCY_ENABLED] = enabled }
    }

    suspend fun setSaturation(saturation: Float) {
        context.liquidGlassDataStore.edit { it[Keys.SATURATION] = saturation.coerceIn(0f, 2f) }
    }

    suspend fun setBrightness(brightness: Float) {
        context.liquidGlassDataStore.edit { it[Keys.BRIGHTNESS] = brightness.coerceIn(-0.50f, 0.50f) }
    }

    suspend fun setContrast(contrast: Float) {
        context.liquidGlassDataStore.edit { it[Keys.CONTRAST] = contrast.coerceIn(0.50f, 1.50f) }
    }

    suspend fun setHighlightStyle(style: HighlightStyle) {
        context.liquidGlassDataStore.edit { it[Keys.HIGHLIGHT_STYLE] = style.storedKey }
    }

    suspend fun setHighlightStrength(strength: Float) {
        context.liquidGlassDataStore.edit { it[Keys.HIGHLIGHT_STRENGTH] = strength.coerceIn(0f, 2f) }
    }

    suspend fun setHighlightWidth(width: Float) {
        context.liquidGlassDataStore.edit { it[Keys.HIGHLIGHT_WIDTH] = width.coerceIn(0f, 2f) }
    }

    suspend fun setHighlightSoftness(softness: Float) {
        context.liquidGlassDataStore.edit { it[Keys.HIGHLIGHT_SOFTNESS] = softness.coerceIn(0f, 2f) }
    }

    suspend fun resetToDefaults() {
        context.liquidGlassDataStore.edit { prefs ->
            prefs[Keys.ENABLED] = true
            prefs[Keys.HEADER_LIQUID_GLASS_ENABLED] = true
            prefs[Keys.HDR_MODE_ENABLED] = true
            prefs[Keys.FROST_PERCENT] = 0.20f
            prefs[Keys.BACKDROP_OPACITY] = 1.00f
            prefs[Keys.REFRACTION_EDGE_WIDTH] = 1.00f
            prefs[Keys.REFRACTION_STRENGTH] = 1.00f
            prefs[Keys.DEPTH_EFFECT] = false
            prefs[Keys.CHROMATIC_ABERRATION] = true
            prefs[Keys.VIBRANCY_ENABLED] = true
            prefs[Keys.SATURATION] = 1.00f
            prefs[Keys.BRIGHTNESS] = 0.00f
            prefs[Keys.CONTRAST] = 1.00f
            prefs[Keys.HIGHLIGHT_STYLE] = HighlightStyle.COMPONENT_DEFAULT.storedKey
            prefs[Keys.HIGHLIGHT_STRENGTH] = 1.00f
            prefs[Keys.HIGHLIGHT_WIDTH] = 1.00f
            prefs[Keys.HIGHLIGHT_SOFTNESS] = 1.00f
        }
    }
}
