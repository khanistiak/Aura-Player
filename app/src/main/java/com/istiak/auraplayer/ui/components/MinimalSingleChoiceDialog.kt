package com.istiak.auraplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

/**
 * Minimal, theme-matched single-choice selection dialog (`MinimalSingleChoiceDialog`):
 * - Clean single-surface `RoundedCornerShape(24.dp)` container (`#F8FAFF` Light / `#111827` Dark)
 *   with a delicate `1.dp` hairline border (`outlineVariant.copy(alpha = 0.35f)`).
 * - Compact `18.sp` SemiBold title with zero two-tone header band.
 * - Normal-height (`minHeight = 46.dp`) rows with `4.dp` vertical spacing: soft Aura Blue
 *   tint & `1.dp` border for the selected row, transparent background for unselected rows.
 * - Minimal Aura Blue `TextButton` Cancel action aligned to the bottom-end.
 */
@Composable
fun MinimalSingleChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    dialogTestTag: String = "minimal_single_choice_dialog"
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val auraBlue = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val containerColor = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFF)
    val hairlineBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val selectedRowFill = if (isDark) {
        Color(0xFF60A5FA).copy(alpha = 0.16f)
    } else {
        Color(0xFF2563EB).copy(alpha = 0.10f)
    }
    val selectedRowBorder = Color(0xFF2563EB).copy(alpha = 0.45f)
    val unselectedRadioColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface
    val cardShape = RoundedCornerShape(24.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .shadow(16.dp, cardShape)
                .clip(cardShape)
                .background(containerColor)
                .border(1.dp, hairlineBorderColor, cardShape)
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp)
                .testTag(dialogTestTag)
        ) {
            Text(
                text = title,
                color = labelColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    val rowShape = RoundedCornerShape(12.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clip(rowShape)
                            .background(if (isSelected) selectedRowFill else Color.Transparent)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, selectedRowBorder, rowShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("minimal_dialog_option_$option")
                    ) {
                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides Dp.Unspecified
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(option) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = auraBlue,
                                    unselectedColor = unselectedRadioColor
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            color = labelColor,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("minimal_dialog_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        color = auraBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
