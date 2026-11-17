package com.istiak.auraplayer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.delay

/**
 * Redesigned Frosted Pastel / Obsidian Liquid Glass 4-Digit Vault PIN Setup Modal (`VaultPinSetupDialog.kt`):
 * - 26.dp rounded corners with soft icy-blue to lavender tint (`#F4F6FF` in Light Mode) and
 *   luminous 1.dp specular border.
 * - Centered circular periwinkle shield-lock emblem (`#E3E6FA` fill, `#2563EB` icon).
 * - 4 individual rounded square digit boxes (`52.dp x 56.dp`, `14.dp` corners) with `#2563EB`
 *   active border glow, masked dot (`●`) or digit preview toggle, and auto-focused numeric input.
 * - Full-width pill-shaped `#2563EB` Save PIN button and subtle Cancel text button.
 */
@Composable
fun VaultPinSetupDialog(
    title: String = "Set Vault PIN",
    subtitle: String = "Create a 4-digit PIN to unlock your Private Folder",
    confirmLabel: String = "Save PIN",
    onDismiss: () -> Unit,
    onConfirmPin: (String) -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    var pinValue by remember { mutableStateOf("") }
    var showDigits by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(80L)
        runCatching {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val cardShape = RoundedCornerShape(26.dp)
    val cardBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF171E35),
                Color(0xFF101526)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF4F6FF),
                Color(0xFFECEEFF)
            )
        )
    }
    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                Color(0xFF60A5FA).copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFD6D9F8).copy(alpha = 0.85f)
            )
        )
    }

    val emblemBg = if (isDark) Color(0xFF283454) else Color(0xFFE3E6FA)
    val cobaltAccent = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val saveButtonBg = Color(0xFF2563EB)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 24.dp,
                    shape = cardShape,
                    ambientColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.35f else 0.14f),
                    spotColor = Color(0xFF1E293B).copy(alpha = if (isDark) 0.65f else 0.18f)
                )
                .clip(cardShape)
                .background(cardBrush)
                .border(1.dp, borderBrush, cardShape)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .testTag("vault_pin_setup_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top centered circular periwinkle shield-lock emblem
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(emblemBg)
                        .border(
                            width = 1.dp,
                            color = cobaltAccent.copy(alpha = if (isDark) 0.45f else 0.22f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = cobaltAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    color = palette.secondaryText,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 4 Individual Rounded Square Digit Boxes backed by an auto-focused BasicTextField
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hidden input field capturing numeric keyboard input cleanly
                    BasicTextField(
                        value = pinValue,
                        onValueChange = { incoming ->
                            val filtered = incoming.filter { it.isDigit() }.take(4)
                            pinValue = filtered
                            errorMessage = null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (pinValue.length == 4) {
                                    onConfirmPin(pinValue)
                                } else {
                                    errorMessage = "Please enter all 4 digits"
                                }
                            }
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color.Transparent),
                        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0.01f)
                            .focusRequester(focusRequester)
                            .testTag("vault_pin_input")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                    ) {
                        repeat(4) { index ->
                            val digitChar = pinValue.getOrNull(index)
                            val isFilled = digitChar != null
                            val isActiveSlot = pinValue.length == index || (pinValue.length == 4 && index == 3)

                            val boxShape = RoundedCornerShape(14.dp)
                            val boxBg by animateColorAsState(
                                targetValue = when {
                                    isActiveSlot -> if (isDark) Color(0xFF1F2A48) else Color(0xFFE3E6FA).copy(alpha = 0.72f)
                                    isFilled -> if (isDark) Color(0xFF1B2440) else Color.White.copy(alpha = 0.96f)
                                    else -> if (isDark) Color(0xFF141B30) else Color.White.copy(alpha = 0.80f)
                                },
                                animationSpec = tween(160),
                                label = "pin_box_bg_$index"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = when {
                                    isActiveSlot -> Color(0xFF2563EB)
                                    isFilled -> cobaltAccent.copy(alpha = 0.65f)
                                    else -> if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFFD6D9F8)
                                },
                                animationSpec = tween(160),
                                label = "pin_box_border_$index"
                            )

                            Box(
                                modifier = Modifier
                                    .size(width = 52.dp, height = 56.dp)
                                    .then(
                                        if (isActiveSlot) {
                                            Modifier.shadow(
                                                elevation = 8.dp,
                                                shape = boxShape,
                                                ambientColor = Color(0xFF2563EB).copy(alpha = 0.35f),
                                                spotColor = Color(0xFF2563EB).copy(alpha = 0.40f)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clip(boxShape)
                                    .background(boxBg)
                                    .border(
                                        width = if (isActiveSlot) 2.dp else 1.2.dp,
                                        color = borderColor,
                                        shape = boxShape
                                    )
                                    .testTag("vault_pin_box_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFilled) {
                                    Text(
                                        text = if (showDigits) digitChar.toString() else "●",
                                        color = if (isDark) Color.White else Color(0xFF1C1B1F),
                                        fontSize = if (showDigits) 22.sp else 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (isActiveSlot) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(22.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFF2563EB))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subtle show/hide digits toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { showDigits = !showDigits }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (showDigits) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showDigits) "Hide PIN digits" else "Show PIN digits",
                        tint = palette.secondaryText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showDigits) "Hide digits" else "Show digits",
                        color = palette.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Full-width pill-shaped #2563EB Save PIN button
                Button(
                    onClick = {
                        if (pinValue.length == 4) {
                            onConfirmPin(pinValue)
                        } else {
                            errorMessage = "Please enter a 4-digit PIN"
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    enabled = pinValue.length == 4,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = saveButtonBg,
                        contentColor = Color.White,
                        disabledContainerColor = saveButtonBg.copy(alpha = 0.40f),
                        disabledContentColor = Color.White.copy(alpha = 0.72f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_vault_pin_button")
                ) {
                    Text(
                        text = confirmLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_vault_pin_button")
                ) {
                    Text(
                        text = "Cancel",
                        color = palette.secondaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
