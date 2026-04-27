package com.paul.simplesprint.features.display_results.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paul.simplesprint.ui.theme.InterExtraBoldTabularTypography

data class DisplayLapRow(
    val deviceName: String,
    val lapTimeLabel: String,
)

@Composable
internal fun DisplayResultsCard(rows: List<DisplayLapRow>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val displayCardBackground = Color(0xFFFFCC00)
        val displayTimeColor = Color(0xFF000000)
        val displayDeviceColor = Color(0xFF000000)
        val density = LocalDensity.current
        val layout = displayLayoutSpecForCount(rows.size)
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "WAITING FOR LAP RESULTS",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                )
            }
            return@BoxWithConstraints
        }

        val count = rows.size.coerceAtLeast(1)
        val visibleCards = displayHorizontalVisibleCardSlots(count)
        val availableHeight = maxHeight.takeIf { it > 0.dp } ?: layout.rowHeight
        val cardHeight = availableHeight.coerceAtLeast(layout.minRowHeight)
        val cardWidth = ((maxWidth - (layout.rowSpacing * (visibleCards - 1))) / visibleCards)
            .coerceAtLeast(layout.minRowHeight)
        val rowContentWidth = (cardWidth - (layout.horizontalPadding * 2)).coerceAtLeast(1.dp)
        val clampedTimeFont = clampDisplayTimeFont(layout.timeFont, cardHeight, rowContentWidth, density)
        val clampedDeviceFont = clampDisplayLabelFont(layout.deviceFont, cardHeight, density)

        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(layout.rowSpacing),
        ) {
            items(rows) { row ->
                Column(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(displayCardBackground)
                        .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = row.deviceName,
                        style = MaterialTheme.typography.bodySmall.merge(
                            TextStyle(
                                fontSize = clampedDeviceFont,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                            ),
                        ),
                        color = displayDeviceColor,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = row.lapTimeLabel,
                        style = MaterialTheme.typography.displayLarge.merge(
                            InterExtraBoldTabularTypography.merge(
                                TextStyle(
                                    fontSize = clampedTimeFont,
                                ),
                            ),
                        ),
                        color = displayTimeColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

internal data class DisplayLayoutSpec(
    val rowHeight: Dp,
    val minRowHeight: Dp,
    val rowSpacing: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val timeFont: TextUnit,
    val deviceFont: TextUnit,
)

internal fun displayLayoutSpecForCount(count: Int): DisplayLayoutSpec {
    return when {
        count <= 1 -> DisplayLayoutSpec(
            rowHeight = 420.dp,
            minRowHeight = 300.dp,
            rowSpacing = 24.dp,
            horizontalPadding = 26.dp,
            verticalPadding = 22.dp,
            timeFont = 168.sp,
            deviceFont = 26.sp,
        )
        count == 2 -> DisplayLayoutSpec(
            rowHeight = 330.dp,
            minRowHeight = 230.dp,
            rowSpacing = 18.dp,
            horizontalPadding = 22.dp,
            verticalPadding = 18.dp,
            timeFont = 138.sp,
            deviceFont = 22.sp,
        )
        count in 3..4 -> DisplayLayoutSpec(
            rowHeight = 245.dp,
            minRowHeight = 170.dp,
            rowSpacing = 12.dp,
            horizontalPadding = 18.dp,
            verticalPadding = 14.dp,
            timeFont = 104.sp,
            deviceFont = 18.sp,
        )
        else -> DisplayLayoutSpec(
            rowHeight = 182.dp,
            minRowHeight = 130.dp,
            rowSpacing = 8.dp,
            horizontalPadding = 14.dp,
            verticalPadding = 10.dp,
            timeFont = 72.sp,
            deviceFont = 15.sp,
        )
    }
}

internal fun displayHorizontalVisibleCardSlots(count: Int): Int = when {
    count <= 1 -> 1
    count == 2 -> 2
    else -> 3
}

internal fun clampDisplayTimeFont(
    base: TextUnit,
    rowHeight: Dp,
    rowContentWidth: Dp,
    density: androidx.compose.ui.unit.Density,
): TextUnit {
    val maxByHeight = with(density) { (rowHeight * 0.74f).toSp() }
    val maxChars = 8f // "MM:SS.cc"
    val widthFactor = 0.62f // Approximate monospace glyph width in ems.
    val maxByWidth = with(density) { (rowContentWidth / (maxChars * widthFactor)).toSp() }
    return minOf(base.value, maxByHeight.value, maxByWidth.value).sp
}

internal fun clampDisplayLabelFont(base: TextUnit, rowHeight: Dp, density: androidx.compose.ui.unit.Density): TextUnit {
    val maxByHeight = with(density) { (rowHeight * 0.16f).toSp() }
    val minReadable = 12.sp
    val clamped = minOf(base.value, maxByHeight.value).sp
    return maxOf(clamped.value, minReadable.value).sp
}
