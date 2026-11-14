package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ExportedActivityItem
import com.example.model.TestStatus
import com.example.ui.theme.LocalAuditStatusColors

@Composable
fun ActivityItemCard(
    item: ExportedActivityItem,
    index: Int,
    loadIcon: suspend (String) -> ImageBitmap?,
    onTestClick: () -> Unit,
    onCopyComponent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalAuditStatusColors.current
    val appIcon by produceState<ImageBitmap?>(initialValue = null, key1 = item.packageName) {
        value = loadIcon(item.packageName)
    }

    val borderColor = when (item.testStatus) {
        TestStatus.Launched -> statusColors.launched.copy(alpha = 0.7f)
        is TestStatus.Denied -> statusColors.denied.copy(alpha = 0.7f)
        is TestStatus.RequiresParameters -> statusColors.warning.copy(alpha = 0.7f)
        TestStatus.NotTested -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("activity_item_card_$index"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header: App Icon + App Name + System/User + Enabled/Disabled + Test Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon!!,
                            contentDescription = item.appName,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Android,
                            contentDescription = item.appName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiniTag(
                            text = stringResource(
                                if (item.isSystemApp) R.string.badge_system else R.string.badge_user
                            ),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MiniTag(
                            text = stringResource(
                                if (item.isEnabled) R.string.badge_enabled else R.string.badge_disabled
                            ),
                            containerColor = if (item.isEnabled) {
                                statusColors.launchedContainer.copy(alpha = 0.55f)
                            } else {
                                statusColors.warningContainer
                            },
                            contentColor = if (item.isEnabled) {
                                statusColors.onLaunchedContainer
                            } else {
                                statusColors.onWarningContainer
                            }
                        )
                    }
                }

                StatusPill(testStatus = item.testStatus)
            }

            Text(
                text = item.shortActivityName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Package & Activity Monospace Metadata Block
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetadataLine(
                        label = stringResource(R.string.detail_package_label),
                        value = item.packageName
                    )
                    MetadataLine(
                        label = stringResource(R.string.detail_activity_label),
                        value = item.activityName
                    )
                }
            }

            // Permission Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.hasNoPermission) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColors.warningContainer,
                        border = BorderStroke(1.dp, statusColors.warning.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = stringResource(R.string.permission_none),
                                tint = statusColors.warning,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.permission_none),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = statusColors.onWarningContainer
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = stringResource(R.string.detail_permission_label),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = item.permission ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Result description & exception details
            val statusSubtitle = when (item.testStatus) {
                TestStatus.NotTested -> null
                TestStatus.Launched -> stringResource(R.string.status_launched_desc)
                is TestStatus.Denied -> "${stringResource(R.string.status_denied_desc)}: ${item.testStatus.reason}"
                is TestStatus.RequiresParameters -> "${stringResource(R.string.status_requires_params_desc)}: ${item.testStatus.reason}"
            }
            AnimatedVisibility(visible = statusSubtitle != null) {
                if (statusSubtitle != null) {
                    val containerColor = when (item.testStatus) {
                        TestStatus.Launched -> statusColors.launchedContainer.copy(alpha = 0.5f)
                        is TestStatus.Denied -> statusColors.deniedContainer.copy(alpha = 0.65f)
                        is TestStatus.RequiresParameters -> statusColors.warningContainer.copy(alpha = 0.65f)
                        TestStatus.NotTested -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val textColor = when (item.testStatus) {
                        TestStatus.Launched -> statusColors.onLaunchedContainer
                        is TestStatus.Denied -> statusColors.onDeniedContainer
                        is TestStatus.RequiresParameters -> statusColors.onWarningContainer
                        TestStatus.NotTested -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = containerColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Action Row: Copy Component + Single-Tap TEST Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onCopyComponent,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("copy_component_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.btn_copy_component_cd),
                        modifier = Modifier.size(18.dp)
                    )
                }

                val isTested = item.testStatus !is TestStatus.NotTested
                Button(
                    onClick = onTestClick,
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (item.testStatus) {
                            TestStatus.Launched -> statusColors.launchedContainer
                            is TestStatus.Denied -> statusColors.deniedContainer
                            is TestStatus.RequiresParameters -> statusColors.warningContainer
                            TestStatus.NotTested -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = when (item.testStatus) {
                            TestStatus.Launched -> statusColors.onLaunchedContainer
                            is TestStatus.Denied -> statusColors.onDeniedContainer
                            is TestStatus.RequiresParameters -> statusColors.onWarningContainer
                            TestStatus.NotTested -> MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("test_single_button_$index")
                ) {
                    Icon(
                        imageVector = if (isTested) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (isTested) R.string.btn_retest_single else R.string.btn_test_single
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataLine(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MiniTag(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusPill(testStatus: TestStatus) {
    val statusColors = LocalAuditStatusColors.current

    val label: String
    val container: Color
    val content: Color
    val icon: ImageVector?

    when (testStatus) {
        TestStatus.NotTested -> {
            label = stringResource(R.string.status_not_tested_desc)
            container = statusColors.neutralContainer
            content = statusColors.onNeutralContainer
            icon = null
        }
        TestStatus.Launched -> {
            label = stringResource(R.string.status_launched)
            container = statusColors.launchedContainer
            content = statusColors.onLaunchedContainer
            icon = Icons.Rounded.CheckCircle
        }
        is TestStatus.Denied -> {
            label = stringResource(R.string.status_denied)
            container = statusColors.deniedContainer
            content = statusColors.onDeniedContainer
            icon = Icons.Rounded.Block
        }
        is TestStatus.RequiresParameters -> {
            label = stringResource(R.string.status_requires_params)
            container = statusColors.warningContainer
            content = statusColors.onWarningContainer
            icon = Icons.Rounded.Info
        }
    }

    Surface(
        shape = CircleShape,
        color = container,
        border = if (testStatus !is TestStatus.NotTested) {
            BorderStroke(1.dp, content.copy(alpha = 0.4f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = content,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = content
            )
        }
    }
}
