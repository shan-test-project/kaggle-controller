package com.kagglecontroller.core.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kagglecontroller.core.common.ApiState
import com.kagglecontroller.core.ui.theme.KC
import com.kagglecontroller.data.local.AppMode
import com.kagglecontroller.data.local.SettingsStore
import com.kagglecontroller.domain.model.RunStatus

fun openInKaggle(context: Context, url: String) {
    val fullMode = SettingsStore(context).settings.value.appMode == AppMode.FULL
    try {
        if (fullMode) {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(android.graphics.Color.rgb(14, 21, 38))
                .build()
                .launchUrl(context, Uri.parse(url))
        } else {
            val external = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context !is Activity) external.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(external)
        }
    } catch (e: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context !is Activity) fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        } catch (_: Exception) {
            // No browser installed.
        }
    }
}

@Composable
fun OpenInKaggleButton(url: String, modifier: Modifier = Modifier, label: String = "Open in Kaggle") {
    val ctx = LocalContext.current
    OutlinedButton(onClick = { openInKaggle(ctx, url) }, modifier = modifier) {
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(label)
    }
}

/** Status is always icon + text, never colour alone (spec 52). */
@Composable
fun StatusPill(status: RunStatus, modifier: Modifier = Modifier) {
    val (icon, color) = when (status) {
        RunStatus.COMPLETE -> Icons.Outlined.CheckCircle to KC.Success
        RunStatus.ERROR -> Icons.Outlined.ErrorOutline to KC.Danger
        RunStatus.RUNNING -> Icons.Outlined.PlayCircleOutline to KC.Cyan
        RunStatus.QUEUED, RunStatus.NEW_SCRIPT -> Icons.Outlined.HourglassTop to KC.Warning
        RunStatus.CANCEL_REQUESTED, RunStatus.CANCEL_ACKNOWLEDGED -> Icons.Outlined.Block to KC.TextDim
        RunStatus.UNKNOWN -> Icons.Outlined.Speed to KC.TextDim
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(status.label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SkeletonList(rows: Int = 6, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val alpha = pulse.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) {
            Box(
                Modifier.fillMaxWidth().height(76.dp)
                    .alpha(alpha.value)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actions != null) {
            Spacer(Modifier.height(20.dp))
            actions()
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, actions: @Composable (() -> Unit)? = null) =
    MessageState(Icons.Outlined.Inbox, title, body, modifier, actions)

/**
 * Renders the correct UI for every ApiState (spec 54): loading skeleton, offline, auth error,
 * permission error, rate limit, unsupported (with Open in Kaggle), generic error, success.
 */
@Composable
fun <T> StateHost(
    state: ApiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onReauth: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    Box(modifier) {
        when (state) {
            ApiState.Idle, ApiState.Loading -> SkeletonList()
            is ApiState.Success -> content(state.data)
            ApiState.Offline -> MessageState(Icons.Outlined.CloudOff, "You're offline",
                "Cached and local items are still available. Reconnect to refresh from Kaggle.") { Button(onClick = onRetry) { Text("Try again") } }
            ApiState.Unauthorized -> MessageState(Icons.Outlined.Lock, "Sign in again",
                "Kaggle rejected your token. It may have expired or been revoked.") {
                Button(onClick = { (onReauth ?: onRetry)() }) { Text(if (onReauth != null) "Enter a new token" else "Try again") }
            }
            is ApiState.Forbidden -> MessageState(Icons.Outlined.Lock, "Permission needed", state.message) {
                Button(onClick = onRetry) { Text("Try again") }
            }
            is ApiState.RateLimited -> MessageState(Icons.Outlined.Schedule, "Kaggle asked us to slow down",
                state.retryAfterSeconds?.let { "Try again in about $it seconds." } ?: "Try again in a minute.") {
                Button(onClick = onRetry) { Text("Try again") }
            }
            is ApiState.Unsupported -> MessageState(Icons.Outlined.Block, "Not available through the API", state.reason) {
                state.webUrl?.let { OpenInKaggleButton(it) }
            }
            is ApiState.Error -> MessageState(Icons.Outlined.ErrorOutline, "Couldn't load this", state.message) {
                Button(onClick = onRetry) { Text("Try again") }
            }
        }
    }
}
