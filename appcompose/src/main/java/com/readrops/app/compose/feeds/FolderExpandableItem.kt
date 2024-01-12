package com.readrops.app.compose.feeds

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.readrops.app.compose.R
import com.readrops.app.compose.util.theme.MediumSpacer
import com.readrops.app.compose.util.theme.spacing
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder

@Composable
fun FolderExpandableItem(
    folder: Folder,
    feeds: List<Feed>,
    onFeedClick: (Feed) -> Unit,
    onFeedLongClick: (Feed) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "folder item arrow rotation"
    )

    Column(
        modifier = Modifier
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing,
                )
            )
    ) {
        Column(
            modifier = Modifier
                .clickable { isExpanded = isExpanded.not() }
                .padding(
                    horizontal = MaterialTheme.spacing.shortSpacing,
                    vertical = MaterialTheme.spacing.veryShortSpacing,
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder_grey),
                        contentDescription = folder.name
                    )

                    MediumSpacer()

                    Text(
                        text = folder.name!!,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(rotationState)
                    )
                }
            }
        }

        Column {
            if (isExpanded) {
                for (feed in feeds) {
                    FeedItem(
                        feed = feed,
                        onClick = { onFeedClick(feed) },
                        onLongClick = { onFeedLongClick(feed) },
                    )
                }
            }
        }
    }
}