package com.istiak.auraplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val SubPanelSkyAccent = Color(0xFF38BDF8)
private val SubPanelDividerColor = Color.White.copy(alpha = 0.16f)
private val SubPanelTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.75f),
    offset = Offset(0f, 1.5f),
    blurRadius = 3.5f
)

/**
 * Level 2 — `← Subtitles Customization` Sub-Panel inside the translucent right-side player drawer
 * (`width = 320.dp`, `Color(0xCC0B101B)` backdrop so the live subtitle remains visible underneath):
 * - **Layout Section:**
 *   - `Alignment` dropdown (`Center`, `Left`, `Right`)
 *   - `Bottom margins` slider (`0` to `150`, default `8`, 1:1 synced with direct vertical subtitle drag)
 *   - `Background` checkbox + color/opacity swatch (unchecked & transparent by default)
 *   - `Fit subtitles into video size` checkbox (checked by default)
 * - **Text Section:**
 *   - `Font` dropdown (`Default`, `Sans-serif`, `Serif`, `Monospace`)
 *   - `Size` slider (`12` to `48`, default `24`, live value readout)
 *   - `Scale` slider (`50%` to `200%`, default `100%`, live value readout)
 *   - `Color` clickable color swatch (default `#FFFFFF`) + `Bold` checkbox
 *   - `Background Color` checkbox + color swatch
 *   - `Border` checkbox (checked by default) + color swatch (`#000000`) + opacity/thickness slider (`0%` to `100%`, default `80%`)
 * - **Advanced Section (Expandable):**
 *   - `Drop shadow` toggle (enabled by default), `Use embedded ASS/SSA styles` toggle, and `Reset to default` button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitlesCustomizationSubPanel(
    styleConfig: SubtitleStyleConfig,
    onUpdateStyleConfig: (SubtitleStyleConfig) -> Unit,
    onResetToDefault: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var alignmentMenuExpanded by remember { mutableStateOf(false) }
    var fontMenuExpanded by remember { mutableStateOf(false) }
    var showBgColorPicker by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showTextBgColorPicker by remember { mutableStateOf(false) }
    var showBorderColorPicker by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("subtitles_customization_panel")
    ) {
        // Header: ← Subtitles Customization
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("subtitles_customization_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Subtitle Menu",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Subtitles Customization",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(shadow = SubPanelTextShadow)
            )
        }

        HorizontalDivider(thickness = 0.6.dp, color = SubPanelDividerColor)

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ==================== LAYOUT SECTION ====================
            CustomizationSectionHeader(title = "Layout")

            // Alignment Dropdown (Center, Left, Right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Alignment",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    style = TextStyle(shadow = SubPanelTextShadow)
                )
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.7.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .clickable { alignmentMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("subtitle_alignment_dropdown")
                    ) {
                        Text(
                            text = styleConfig.alignment.label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = alignmentMenuExpanded,
                        onDismissRequest = { alignmentMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF111827))
                    ) {
                        SubtitleAlignment.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (styleConfig.alignment == option) SubPanelSkyAccent else Color.White,
                                        fontWeight = if (styleConfig.alignment == option) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    alignmentMenuExpanded = false
                                    onUpdateStyleConfig(styleConfig.copy(alignment = option))
                                }
                            )
                        }
                    }
                }
            }

            // Bottom margins slider (0 to 150, default 8, 1:1 synced with direct vertical subtitle drag)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bottom margins",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        style = TextStyle(shadow = SubPanelTextShadow)
                    )
                    Text(
                        text = "${styleConfig.bottomMargin}",
                        color = SubPanelSkyAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = styleConfig.bottomMargin.toFloat(),
                    onValueChange = {
                        onUpdateStyleConfig(styleConfig.copy(bottomMargin = it.roundToInt().coerceIn(0, 150)))
                    },
                    valueRange = 0f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = SubPanelSkyAccent,
                        activeTrackColor = SubPanelSkyAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.testTag("subtitle_bottom_margin_slider")
                )
            }

            // Background checkbox + color/opacity swatch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onUpdateStyleConfig(
                                styleConfig.copy(backgroundEnabled = !styleConfig.backgroundEnabled)
                            )
                        }
                        .padding(vertical = 4.dp)
                        .testTag("subtitle_background_checkbox")
                ) {
                    CustomizationSquareCheckbox(checked = styleConfig.backgroundEnabled)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Background",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        style = TextStyle(shadow = SubPanelTextShadow)
                    )
                }
                ColorSwatchButton(
                    colorArgb = styleConfig.backgroundColorArgb,
                    onClick = { showBgColorPicker = !showBgColorPicker }
                )
            }
            AnimatedVisibility(visible = showBgColorPicker) {
                ColorPaletteRow(
                    colors = SubtitleStyleConfig.PresetBackgroundColors,
                    selectedColorArgb = styleConfig.backgroundColorArgb,
                    onSelectColor = { argb ->
                        onUpdateStyleConfig(
                            styleConfig.copy(
                                backgroundEnabled = true,
                                backgroundColorArgb = argb
                            )
                        )
                    }
                )
            }

            // Fit subtitles into video size checkbox (checked by default)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        onUpdateStyleConfig(
                            styleConfig.copy(fitIntoVideoSize = !styleConfig.fitIntoVideoSize)
                        )
                    }
                    .padding(vertical = 4.dp)
                    .testTag("subtitle_fit_video_checkbox")
            ) {
                CustomizationSquareCheckbox(checked = styleConfig.fitIntoVideoSize)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Fit subtitles into video size",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    style = TextStyle(shadow = SubPanelTextShadow)
                )
            }

            HorizontalDivider(thickness = 0.6.dp, color = SubPanelDividerColor)

            // ==================== TEXT SECTION ====================
            CustomizationSectionHeader(title = "Text")

            // Font dropdown (Default, Sans-serif, Serif, Monospace)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Font",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    style = TextStyle(shadow = SubPanelTextShadow)
                )
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.7.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .clickable { fontMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("subtitle_font_dropdown")
                    ) {
                        Text(
                            text = styleConfig.fontFamily.label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = fontMenuExpanded,
                        onDismissRequest = { fontMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF111827))
                    ) {
                        SubtitleFontFamilyOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (styleConfig.fontFamily == option) SubPanelSkyAccent else Color.White,
                                        fontWeight = if (styleConfig.fontFamily == option) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    fontMenuExpanded = false
                                    onUpdateStyleConfig(styleConfig.copy(fontFamily = option))
                                }
                            )
                        }
                    }
                }
            }

            // Size slider (12 to 48, default 24)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Size",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        style = TextStyle(shadow = SubPanelTextShadow)
                    )
                    Text(
                        text = "${styleConfig.fontSizeSp}",
                        color = SubPanelSkyAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = styleConfig.fontSizeSp.toFloat(),
                    onValueChange = {
                        onUpdateStyleConfig(styleConfig.copy(fontSizeSp = it.roundToInt().coerceIn(12, 48)))
                    },
                    valueRange = 12f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = SubPanelSkyAccent,
                        activeTrackColor = SubPanelSkyAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.testTag("subtitle_size_slider")
                )
            }

            // Scale slider (50% to 200%, default 100%)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Scale",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        style = TextStyle(shadow = SubPanelTextShadow)
                    )
                    Text(
                        text = "${styleConfig.scalePercent}%",
                        color = SubPanelSkyAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = styleConfig.scalePercent.toFloat(),
                    onValueChange = {
                        onUpdateStyleConfig(styleConfig.copy(scalePercent = it.roundToInt().coerceIn(50, 200)))
                    },
                    valueRange = 50f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = SubPanelSkyAccent,
                        activeTrackColor = SubPanelSkyAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.testTag("subtitle_scale_slider")
                )
            }

            // Color clickable swatch (default #FFFFFF) + Bold checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Color",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        style = TextStyle(shadow = SubPanelTextShadow)
                    )
                    ColorSwatchButton(
                        colorArgb = styleConfig.textColorArgb,
                        onClick = { showTextColorPicker = !showTextColorPicker }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onUpdateStyleConfig(styleConfig.copy(isBold = !styleConfig.isBold))
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("subtitle_bold_checkbox")
                ) {
                    CustomizationSquareCheckbox(checked = styleConfig.isBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bold",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = SubPanelTextShadow)
                    )
                }
            }
            AnimatedVisibility(visible = showTextColorPicker) {
                ColorPaletteRow(
                    colors = SubtitleStyleConfig.PresetTextColors,
                    selectedColorArgb = styleConfig.textColorArgb,
                    onSelectColor = { argb ->
                        onUpdateStyleConfig(styleConfig.copy(textColorArgb = argb))
                    }
                )
            }

            // Background Color checkbox + color swatch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onUpdateStyleConfig(
                                styleConfig.copy(textBackgroundEnabled = !styleConfig.textBackgroundEnabled)
                            )
                        }
                        .padding(vertical = 4.dp)
                        .testTag("subtitle_text_bg_checkbox")
                ) {
                    CustomizationSquareCheckbox(checked = styleConfig.textBackgroundEnabled)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Background Color",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        style = TextStyle(shadow = SubPanelTextShadow)
                    )
                }
                ColorSwatchButton(
                    colorArgb = styleConfig.textBackgroundColorArgb,
                    onClick = { showTextBgColorPicker = !showTextBgColorPicker }
                )
            }
            AnimatedVisibility(visible = showTextBgColorPicker) {
                ColorPaletteRow(
                    colors = SubtitleStyleConfig.PresetBackgroundColors,
                    selectedColorArgb = styleConfig.textBackgroundColorArgb,
                    onSelectColor = { argb ->
                        onUpdateStyleConfig(
                            styleConfig.copy(
                                textBackgroundEnabled = true,
                                textBackgroundColorArgb = argb
                            )
                        )
                    }
                )
            }

            // Border checkbox (checked by default) + color swatch (#000000) + opacity/thickness slider (0%..100%, default 80%)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onUpdateStyleConfig(
                                    styleConfig.copy(borderEnabled = !styleConfig.borderEnabled)
                                )
                            }
                            .padding(vertical = 4.dp)
                            .testTag("subtitle_border_checkbox")
                    ) {
                        CustomizationSquareCheckbox(checked = styleConfig.borderEnabled)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Border",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            style = TextStyle(shadow = SubPanelTextShadow)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${styleConfig.borderStrengthPercent}%",
                            color = SubPanelSkyAccent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        ColorSwatchButton(
                            colorArgb = styleConfig.borderColorArgb,
                            onClick = { showBorderColorPicker = !showBorderColorPicker }
                        )
                    }
                }
                AnimatedVisibility(visible = showBorderColorPicker) {
                    ColorPaletteRow(
                        colors = SubtitleStyleConfig.PresetBorderColors,
                        selectedColorArgb = styleConfig.borderColorArgb,
                        onSelectColor = { argb ->
                            onUpdateStyleConfig(
                                styleConfig.copy(
                                    borderEnabled = true,
                                    borderColorArgb = argb
                                )
                            )
                        }
                    )
                }
                Slider(
                    value = styleConfig.borderStrengthPercent.toFloat(),
                    onValueChange = {
                        onUpdateStyleConfig(
                            styleConfig.copy(
                                borderEnabled = true,
                                borderStrengthPercent = it.roundToInt().coerceIn(0, 100)
                            )
                        )
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = SubPanelSkyAccent,
                        activeTrackColor = SubPanelSkyAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.testTag("subtitle_border_strength_slider")
                )
            }

            HorizontalDivider(thickness = 0.6.dp, color = SubPanelDividerColor)

            // ==================== ADVANCED SECTION (EXPANDABLE) ====================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { advancedExpanded = !advancedExpanded }
                    .padding(vertical = 6.dp)
                    .testTag("subtitle_advanced_header")
            ) {
                CustomizationSectionHeader(title = "Advanced")
                Icon(
                    imageVector = if (advancedExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (advancedExpanded) "Collapse Advanced" else "Expand Advanced",
                    tint = SubPanelSkyAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = advancedExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Drop shadow toggle (enabled by default)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Drop shadow",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            style = TextStyle(shadow = SubPanelTextShadow)
                        )
                        Switch(
                            checked = styleConfig.dropShadowEnabled,
                            onCheckedChange = {
                                onUpdateStyleConfig(styleConfig.copy(dropShadowEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SubPanelSkyAccent
                            )
                        )
                    }

                    // Use embedded ASS/SSA styles toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Use embedded ASS/SSA styles",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            style = TextStyle(shadow = SubPanelTextShadow),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = styleConfig.useEmbeddedAssStyles,
                            onCheckedChange = {
                                onUpdateStyleConfig(styleConfig.copy(useEmbeddedAssStyles = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SubPanelSkyAccent
                            )
                        )
                    }

                    // Reset to default button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.7.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onResetToDefault)
                            .padding(vertical = 8.dp)
                            .testTag("subtitle_reset_default_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = SubPanelSkyAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset to default",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationSectionHeader(title: String) {
    Text(
        text = title,
        color = SubPanelSkyAccent,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp
    )
}

@Composable
private fun CustomizationSquareCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(18.dp)
            .clip(shape)
            .background(if (checked) SubPanelSkyAccent else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (checked) SubPanelSkyAccent else Color.White.copy(alpha = 0.65f),
                shape = shape
            )
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color(0xFF06101E),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ColorSwatchButton(
    colorArgb: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(colorArgb))
            .border(1.2.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPaletteRow(
    colors: List<Int>,
    selectedColorArgb: Int,
    onSelectColor: (Int) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        colors.forEach { colorArgb ->
            val isSelected = selectedColorArgb == colorArgb
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(colorArgb))
                    .border(
                        width = if (isSelected) 2.4.dp else 1.dp,
                        color = if (isSelected) SubPanelSkyAccent else Color.White.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(colorArgb) }
            )
        }
    }
}
