package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.math.NumberFormatter
import com.example.ui.theme.OutfitFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BigNumberViewerDialog(
    expressionLabel: String,
    plainDigits: String,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val digitCount = remember(plainDigits) { plainDigits.count { it.isDigit() } }
    val formattedDigitCount = remember(digitCount) {
        NumberFormatter.formatGroupedNumberString(digitCount.toString())
    }

    val formattedParagraphs = remember(plainDigits) {
        buildCleanNumberParagraphs(plainDigits)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("big_number_viewer_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header matching HistorySheet style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.big_number_full_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.big_number_digits_count, digitCount)
                            .replace(digitCount.toString(), formattedDigitCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

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

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Main number card matching calculator display aesthetic
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 96.dp, max = 420.dp)
            ) {
                val numberScrollState = rememberScrollState()
                val fontSizeSp = when {
                    plainDigits.length <= 36 -> 24.sp
                    plainDigits.length <= 96 -> 21.sp
                    else -> 19.sp
                }
                val lineHeightSp = when {
                    plainDigits.length <= 36 -> 34.sp
                    plainDigits.length <= 96 -> 30.sp
                    else -> 28.sp
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    val cleanExprLabel = expressionLabel.trim()
                    if (cleanExprLabel.isNotEmpty() && cleanExprLabel != plainDigits) {
                        SelectionContainer {
                            Text(
                                text = "$cleanExprLabel =",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }

                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(numberScrollState)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            formattedParagraphs.forEach { chunk ->
                                Text(
                                    text = chunk,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = fontSizeSp,
                                        lineHeight = lineHeightSp,
                                        letterSpacing = 0.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onCopy(plainDigits)
                    onDismiss()
                },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("copy_big_number_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.big_number_copy_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun buildCleanNumberParagraphs(plainDigits: String): List<String> {
    if (plainDigits.isEmpty()) return emptyList()
    // Format integer part with 3-digit spaces; decimal part remains unspaced (e.g. 5.215232564...)
    val displayString = NumberFormatter.formatGroupedNumberString(plainDigits).replace("\u2009", " ")
    if (displayString.length <= 1800) {
        return listOf(displayString)
    }
    val chunks = mutableListOf<String>()
    var idx = 0
    val maxChunk = 1500
    while (idx < displayString.length) {
        val rawEnd = (idx + maxChunk).coerceAtMost(displayString.length)
        if (rawEnd >= displayString.length) {
            chunks.add(displayString.substring(idx).trim())
            break
        }
        val lastSpace = displayString.lastIndexOf(' ', rawEnd)
        if (lastSpace > idx + 300) {
            chunks.add(displayString.substring(idx, lastSpace).trimEnd())
            idx = lastSpace + 1
        } else {
            chunks.add(displayString.substring(idx, rawEnd))
            idx = rawEnd
        }
    }
    return chunks
}
