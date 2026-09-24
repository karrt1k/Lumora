package com.example.ui.components

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-performance smooth marquee for video titles in player top bar.
 * Uses Compose basicMarquee so long filenames smoothly slide across and reveal the entire name.
 */
@Composable
fun AutoScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
    maxLines: Int = 1,
    startDelayMillis: Int = 1000,
    pauseMillis: Int = 1000,
    speedMillis: Int = 3000,
    endGap: Dp = 32.dp
) {
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    animationMode = MarqueeAnimationMode.Immediately,
                    repeatDelayMillis = pauseMillis,
                    initialDelayMillis = startDelayMillis,
                    spacing = MarqueeSpacing(endGap),
                    velocity = 36.dp
                )
        )
    }
}
