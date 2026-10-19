package com.marta.habittracker.presentation.components

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.utils.toHex

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    displayName: String,
    avatarColor: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 2.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (borderColor != null) {
                    Modifier.border(borderWidth, borderColor, CircleShape)
                } else {
                    Modifier
                },
            )
            .padding(if (borderColor != null) borderWidth else 0.dp)
            .clip(CircleShape)
            .background(avatarColor.toComposeColor()),
        contentAlignment = Alignment.Center,
    ) {
        val url = avatarUrl?.takeIf { it.isNotBlank() }
        if (url != null) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        } else {
            Text(
                text = userAvatarInitials(displayName),
                style = initialsTextStyle(size),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
fun userAvatarInitials(displayName: String): String {
    val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
    val fallback = stringResource(R.string.user_avatar_initials_fallback)
    return when {
        parts.isEmpty() -> fallback
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

fun String.toComposeColor(): Color =
    runCatching { Color(parseColor(this)) }.getOrDefault(HabitPrimary)

@Composable
private fun initialsTextStyle(size: Dp) = when {
    size >= 72.dp -> MaterialTheme.typography.headlineSmall
    size >= 48.dp -> MaterialTheme.typography.titleSmall
    else -> MaterialTheme.typography.labelMedium
}

@Preview(showBackground = true, name = "ProfileAvatar initials")
@Composable
private fun ProfileAvatarInitialsPreview() {
    ProfileAvatar(
        avatarUrl = null,
        displayName = "John Doe",
        avatarColor = HabitPrimary.toHex(),
        size = 80.dp,
    )
}
