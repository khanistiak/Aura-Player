package com.istiak.auraplayer

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

private val PfCobaltActive = Color(0xFF2563EB)
private val PfDangerRed = Color(0xFFDC2626)
private val PfIconBgLight = Color(0xFFEEF2FF)
private val PfSlateHeaderLight = Color(0xFF434A6E)
private val PfDividerLight = Color(0xFFE8ECF8)

/**
 * Secure / Private Folder Settings Sub-Screen (`PrivateFolderSettingsScreen.kt`):
 * - Frozen `ScrollAwareFixedHeader`: `← Secure Folder Settings`
 * - Section `| Security`: Enable Biometric Unlock, Reset PIN (4-box modal),
 *   Reset Security Question, Forgot PIN, and Hide Secure Folder Settings.
 * - Section `| Danger Zone`: Destroy Secure Folder with confirmation dialog.
 * - Section `| Help`: Secure Folder access tip.
 */
@Composable
fun PrivateFolderSettingsScreen(
    settings: AppSettingsState,
    onUpdateBiometricUnlock: (Boolean) -> Unit,
    onUpdateHideSecureFolderSettings: (Boolean) -> Unit,
    onSaveSecurityQuestion: (String, String) -> Unit,
    onDestroySecureFolder: () -> Unit,
    onBack: () -> Unit,
    onShowToast: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalExtendedPalette.current

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showSecurityQuestionDialog by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var showDestroyConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 4.dp,
        bottomPadding = navBottomDp + 24.dp,
        horizontalPadding = 12.dp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("private_folder_settings_screen")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = scrollLayout.backdropState,
                    scrollableState = listState,
                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                )
        ) {
            // Section | Security
            item(key = "sec_security") {
                PfFrostedSection(title = "| Security") {
                    PfSwitchRow(
                        icon = Icons.Filled.Fingerprint,
                        title = "Enable Biometric Unlock",
                        subtitle = "Use your fingerprint or face to unlock",
                        checked = settings.biometricVaultLockEnabled,
                        onCheckedChange = onUpdateBiometricUnlock,
                        testTag = "pf_switch_biometric"
                    )

                    PfActionRow(
                        icon = Icons.Filled.Lock,
                        title = "Reset PIN",
                        subtitle = if (BiometricAuth.hasCustomVaultPin(context)) {
                            "Update your 4-digit PIN"
                        } else {
                            "Create a 4-digit PIN"
                        },
                        onClick = { showPinSetupDialog = true },
                        testTag = "pf_reset_pin_row"
                    )

                    PfActionRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Reset Security Question",
                        subtitle = if (settings.vaultSecurityQuestion.isNotBlank()) {
                            settings.vaultSecurityQuestion
                        } else {
                            "Security Answer"
                        },
                        onClick = { showSecurityQuestionDialog = true },
                        testTag = "pf_security_question_row"
                    )

                    PfActionRow(
                        icon = Icons.Filled.LockReset,
                        title = "Forgot PIN",
                        subtitle = "Reset PIN using your security question",
                        onClick = {
                            if (settings.vaultSecurityQuestion.isBlank() || settings.vaultSecurityAnswer.isBlank()) {
                                onShowToast("Set a security question first")
                                showSecurityQuestionDialog = true
                            } else {
                                showForgotPinDialog = true
                            }
                        },
                        testTag = "pf_forgot_pin_row"
                    )

                    PfSwitchRow(
                        icon = Icons.Filled.VisibilityOff,
                        title = "Hide Secure Folder Settings",
                        subtitle = "Remove this entry from Settings",
                        checked = settings.hideSecureFolderSettings,
                        onCheckedChange = { hidden ->
                            onUpdateHideSecureFolderSettings(hidden)
                            if (hidden) {
                                onShowToast("Hidden from Settings — restore via Vault ⋮ menu")
                            }
                        },
                        showDivider = false,
                        testTag = "pf_switch_hide_settings"
                    )
                }
            }

            // Section | Danger Zone
            item(key = "sec_danger_zone") {
                PfFrostedSection(title = "| Danger Zone", isDangerHeader = true) {
                    val isDark = palette.isDark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { showDestroyConfirmDialog = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("pf_destroy_secure_folder_row")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PfDangerRed.copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null,
                                tint = PfDangerRed,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Destroy Secure Folder",
                                color = PfDangerRed,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Permanently delete all secured videos and reset PIN",
                                color = palette.secondaryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Section | Help
            item(key = "sec_help") {
                PfFrostedSection(title = "| Help") {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        PfIconBadge(icon = Icons.Filled.Info)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Secure Folder",
                                color = palette.primaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the discreet Vault icon in the top header or long-press the app title at the top of the Home screen to open the Secure Folder.",
                                color = palette.secondaryText,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("pf_settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "Private Folder",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showPinSetupDialog) {
        VaultPinSetupDialog(
            title = "Set Vault PIN",
            subtitle = "Create a 4-digit PIN to unlock your Private Folder",
            onDismiss = { showPinSetupDialog = false },
            onConfirmPin = { newPin ->
                BiometricAuth.setCustomVaultPin(context, newPin)
                showPinSetupDialog = false
                onShowToast("Vault PIN updated")
            }
        )
    }

    if (showSecurityQuestionDialog) {
        SecurityQuestionSetupDialog(
            initialQuestion = settings.vaultSecurityQuestion,
            initialAnswer = settings.vaultSecurityAnswer,
            onDismiss = { showSecurityQuestionDialog = false },
            onSave = { q, a ->
                onSaveSecurityQuestion(q, a)
                showSecurityQuestionDialog = false
                onShowToast("Security question saved")
            }
        )
    }

    if (showForgotPinDialog) {
        ForgotPinVerifyDialog(
            question = settings.vaultSecurityQuestion,
            expectedAnswer = settings.vaultSecurityAnswer,
            onDismiss = { showForgotPinDialog = false },
            onVerified = {
                showForgotPinDialog = false
                showPinSetupDialog = true
            }
        )
    }

    if (showDestroyConfirmDialog) {
        DestroySecureFolderConfirmDialog(
            onDismiss = { showDestroyConfirmDialog = false },
            onConfirmDestroy = {
                showDestroyConfirmDialog = false
                context.getSharedPreferences("aura_vault_security", Context.MODE_PRIVATE)
                    .edit()
                    .remove("vault_fallback_pin")
                    .apply()
                onDestroySecureFolder()
            }
        )
    }
}

@Composable
private fun SecurityQuestionSetupDialog(
    initialQuestion: String,
    initialAnswer: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val presets = listOf(
        "What was your childhood nickname?",
        "What city were you born in?",
        "What is your favorite movie?",
        "What was your first pet's name?"
    )
    var questionText by remember {
        mutableStateOf(initialQuestion.ifBlank { presets.first() })
    }
    var answerText by remember { mutableStateOf(initialAnswer) }
    var errorText by remember { mutableStateOf<String?>(null) }

    PfFrostedModalCard(onDismiss = onDismiss) {
        Text(
            text = "Reset Security Question",
            color = palette.primaryText,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Used to recover your Secure Folder if you forget your PIN.",
            color = palette.secondaryText,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = questionText,
            onValueChange = {
                questionText = it
                errorText = null
            },
            label = { Text("Security Question") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PfCobaltActive,
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD6D9F8),
                focusedTextColor = palette.primaryText,
                unfocusedTextColor = palette.primaryText
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = answerText,
            onValueChange = {
                answerText = it
                errorText = null
            },
            label = { Text("Security Answer") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PfCobaltActive,
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD6D9F8),
                focusedTextColor = palette.primaryText,
                unfocusedTextColor = palette.primaryText
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = errorText!!, color = PfDangerRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.secondaryText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (questionText.isBlank() || answerText.isBlank()) {
                        errorText = "Enter both a question and an answer"
                    } else {
                        onSave(questionText.trim(), answerText.trim())
                    }
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PfCobaltActive,
                    contentColor = Color.White
                )
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ForgotPinVerifyDialog(
    question: String,
    expectedAnswer: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var enteredAnswer by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    PfFrostedModalCard(onDismiss = onDismiss) {
        Text(
            text = "Forgot PIN",
            color = palette.primaryText,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = question,
            color = if (isDark) Color(0xFFBFDBFE) else PfCobaltActive,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = enteredAnswer,
            onValueChange = {
                enteredAnswer = it
                errorText = null
            },
            label = { Text("Your Answer") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PfCobaltActive,
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD6D9F8),
                focusedTextColor = palette.primaryText,
                unfocusedTextColor = palette.primaryText
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = errorText!!, color = PfDangerRed, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.secondaryText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (enteredAnswer.trim().equals(expectedAnswer.trim(), ignoreCase = true)) {
                        onVerified()
                    } else {
                        errorText = "Incorrect security answer"
                    }
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PfCobaltActive,
                    contentColor = Color.White
                )
            ) {
                Text("Verify & Reset PIN", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DestroySecureFolderConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmDestroy: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    PfFrostedModalCard(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PfDangerRed.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = PfDangerRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Destroy Secure Folder?",
                color = PfDangerRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "This will permanently delete every video inside AuraPlayer/.private/, erase vault.kv and cached thumbnails, and reset your Vault PIN and security question. This action cannot be undone.",
            color = palette.secondaryText,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.secondaryText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onConfirmDestroy,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PfDangerRed,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_destroy_secure_folder")
            ) {
                Text("Destroy", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PfFrostedModalCard(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val cardShape = RoundedCornerShape(24.dp)
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF171E35), Color(0xFF101526)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF4F6FF), Color(0xFFECEEFF)))
    }
    val borderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color(0xFF60A5FA).copy(alpha = 0.20f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color(0xFFD6D9F8).copy(alpha = 0.85f)))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .shadow(20.dp, cardShape)
                .clip(cardShape)
                .background(bgBrush)
                .border(1.dp, borderBrush, cardShape)
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun PfFrostedSection(
    title: String,
    isDangerHeader: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val headerColor = when {
        isDangerHeader -> PfDangerRed
        isDark -> Color(0xFFB7C5E8)
        else -> PfSlateHeaderLight
    }
    val cardShape = RoundedCornerShape(16.dp)
    val cardBg = if (isDark) Color(0xFF161B2E).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.84f)
    val borderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color(0xFF60A5FA).copy(alpha = 0.14f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), Color(0xFFD6D9F8).copy(alpha = 0.72f)))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = headerColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = cardShape,
                    ambientColor = Color(0xFF434A6E).copy(alpha = if (isDark) 0.28f else 0.06f),
                    spotColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.28f else 0.08f)
                )
                .clip(cardShape)
                .background(cardBg)
                .border(1.dp, borderBrush, cardShape)
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun PfIconBadge(icon: ImageVector) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isDark) PfCobaltActive.copy(alpha = 0.18f) else PfIconBgLight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color(0xFF60A5FA) else PfCobaltActive,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PfSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else PfDividerLight

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(testTag)
        ) {
            PfIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.scale(0.86f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PfCobaltActive,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun PfActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else PfDividerLight

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(testTag)
        ) {
            PfIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
