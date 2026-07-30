package com.marta.habittracker.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marta.habittracker.R

@Composable
fun CustomBadgeBox(modifier: Modifier = Modifier, painter: Painter, notificationNumber: Int) {
    BadgedBox(badge = {
        Badge(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            content = { Text("$notificationNumber") })
    }) {
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painter,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = stringResource(R.string.cd_toolbar_actions)
        )
    }
}