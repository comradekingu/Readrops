package com.readrops.app.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.readrops.app.util.components.FeedIcon
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.spacing

@Composable
fun NotificationItem(
    feedName: String,
    iconUrl: String?,
    folderName: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier.clickable { onCheckChange(!checked) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.mediumSpacing,
                    vertical = MaterialTheme.spacing.shortSpacing
                )
        ) {
            FeedIcon(
                iconUrl = iconUrl,
                name = feedName
            )

            MediumSpacer()

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f, fill = true)
            ) {
                Text(
                    text = feedName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (folderName != null) {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }


            MediumSpacer()

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckChange,
            )
        }
    }
}