package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.math.NumberFormatter

private data class DigitDisplayRow(
    val indexLabel: String,
    val content: String
)

@Composable
fun BigNumberViewerDialog(
    expressionLabel: String,
    plainDigits: String,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val digitCount = remember(plainDigits) { plainDigits.count { it.isDigit() } }
    val formattedDigitCount = remember(digitCount) {
        NumberFormatter.formatGroupedNumberString(digitCount.toString())
    }
    var useGroupedView by remember { mutableStateOf(true) }

    val rows = remember(plainDigits, useGroupedView) {
        buildDigitRows(plainDigits, grouped = useGroupedView)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 540.dp)
                .heightIn(max = 590.dp)
                .testTag("big_number_viewer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Header: Title + Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.big_number_full_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("big_number_close_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compact expression summary card (prevents huge expressions from overflowing header)
                val cleanExprLabel = expressionLabel.trim()
                if (cleanExprLabel.isNotEmpty() && cleanExprLabel != plainDigits) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$cleanExprLabel =",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata pill + View mode segmented toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.big_number_digits_count, digitCount)
                                .replace(digitCount.toString(), formattedDigitCount),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (plainDigits.length > 24) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FormatModeTab(
                                    label = stringResource(R.string.big_number_mode_grouped),
                                    selected = useGroupedView,
                                    onClick = { useGroupedView = true }
                                )
                                FormatModeTab(
                                    label = stringResource(R.string.big_number_mode_raw),
                                    selected = !useGroupedView,
                                    onClick = { useGroupedView = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Smooth lazy-scrolled digit container (handles thousands of digits effortlessly)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 120.dp, max = 330.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = rows,
                            key = { idx, _ -> idx }
                        ) { idx, row ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (row.indexLabel.isNotEmpty()) {
                                        Text(
                                            text = row.indexLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                            maxLines = 1,
                                            modifier = Modifier
                                                .widthIn(min = 42.dp)
                                                .padding(end = 8.dp)
                                        )
                                    }
                                    SelectionContainer(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.content,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = if (useGroupedView) 15.sp else 14.sp,
                                                lineHeight = 22.sp,
                                                letterSpacing = if (useGroupedView) 0.4.sp else 0.sp,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (rows.size > 4 && (idx + 1) % 5 == 0 && idx < rows.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer action button
                Button(
                    onClick = {
                        onCopy(plainDigits)
                        onDismiss()
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_big_number_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.action_copy))
                }
            }
        }
    }
}

@Composable
private fun FormatModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(160),
        label = "format_mode_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(160),
        label = "format_mode_text"
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            maxLines = 1
        )
    }
}

private fun buildDigitRows(plainDigits: String, grouped: Boolean): List<DigitDisplayRow> {
    if (plainDigits.isEmpty()) return emptyList()

    if (!grouped) {
        // Raw continuous blocks (26 chars per row so monospace fits cleanly without ragged wrapping)
        val chunkSize = 26
        val showIndex = plainDigits.length > chunkSize * 2
        return plainDigits.chunked(chunkSize).mapIndexed { idx, chunk ->
            DigitDisplayRow(
                indexLabel = if (showIndex) "${idx * chunkSize + 1}" else "",
                content = chunk
            )
        }
    }

    // Standard 3-digit grouped formatting for moderately sized numbers
    if (plainDigits.length <= 45) {
        return listOf(
            DigitDisplayRow(
                indexLabel = "",
                content = NumberFormatter.formatGroupedNumberString(plainDigits)
            )
        )
    }

    // For very large numbers, split into neat rows of 3 x 5-char blocks (15 chars per line)
    val groupSize = 5
    val groupsPerLine = 3
    val lineChars = groupSize * groupsPerLine
    val totalLines = (plainDigits.length + lineChars - 1) / lineChars
    val showIndex = totalLines > 2

    val rows = ArrayList<DigitDisplayRow>(totalLines)
    var pos = 0
    while (pos < plainDigits.length) {
        val end = (pos + lineChars).coerceAtMost(plainDigits.length)
        val slice = plainDigits.substring(pos, end)
        val spaced = slice.chunked(groupSize).joinToString(" ")
        rows.add(
            DigitDisplayRow(
                indexLabel = if (showIndex) "${pos + 1}" else "",
                content = spaced
            )
        )
        pos = end
    }
    return rows
}
