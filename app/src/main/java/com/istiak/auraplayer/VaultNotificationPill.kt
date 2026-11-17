package com.istiak.auraplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class VaultToastType {
    LOCK,
    RESTORE,
    RESCAN,
    INFO
}

data class VaultToastEvent(
    val message: String,
    val type: VaultToastType = VaultToastType.LOCK,
    val id: Long = System.nanoTime()
)

/**
 * Compact 1-second floating Capsule Toast Pill for Private Vault lock/restore and MediaStore rescan actions:
 * - Centered horizontally above the bottom navigation bar (`padding(bottom = 96.dp)`).
 * - Dark obsidian-glass surface (`Color(0xFF141824).copy(alpha = 0.90f)`) with `1.dp` white specular border.
 * - Small leading icon in `#60A5FA` and concise single-line white text.
 * - Auto-dismisses strictly after `1000ms` with a fast `150ms` fade/scale animation.
 */
@Composable
fun VaultNotificationPill(
    event: VaultToastEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(event) {
        if (event != null) {
            delay(1000L)
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 96.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = event != null,
            enter = fadeIn(animationSpec = tween(durationMillis = 150)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 150)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = 150))
        ) {
            event?.let { toast ->
                val pillShape = RoundedCornerShape(999.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .shadow(
                            elevation = 12.dp,
                            shape = pillShape,
                            ambientColor = Color.Black.copy(alpha = 0.35f),
                            spotColor = Color.Black.copy(alpha = 0.40f)
                        )
                        .clip(pillShape)
                        .background(Color(0xFF141824).copy(alpha = 0.90f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.22f),
                            shape = pillShape
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("vault_notification_pill")
                ) {
                    Icon(
                        imageVector = when (toast.type) {
                            VaultToastType.LOCK -> Icons.Filled.Lock
                            VaultToastType.RESTORE -> Icons.Filled.Restore
                            VaultToastType.RESCAN -> Icons.Filled.Refresh
                            VaultToastType.INFO -> Icons.Filled.Info
                        },
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = toast.message,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
