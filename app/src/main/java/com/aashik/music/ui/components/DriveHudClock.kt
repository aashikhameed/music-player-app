package com.aashik.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.utils.getCurrentTimeWithAmPm
import kotlinx.coroutines.delay

private val ClockBadgeShape = RoundedCornerShape(12.dp)

/**
 * Isolated leaf composable for the Drive HUD clock.
 * Updates at 15s intervals without triggering recompositions of the outer HUD.
 */
@Composable
fun DriveHudClock(
    isDark: Boolean,
    isNightDimmer: Boolean = false,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getCurrentTimeWithAmPm()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            currentTime = getCurrentTimeWithAmPm()
        }
    }

    val badgeBg = when {
        isNightDimmer -> Color(0xFF131922)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isNightDimmer -> Color(0xFFCCD6E0)
        isDark -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val timeParts = currentTime.split(" ")
    val timeDigits = timeParts.getOrElse(0) { currentTime }
    val amPmLabel = timeParts.getOrElse(1) { "" }

    Row(
        modifier = modifier
            .background(color = badgeBg, shape = ClockBadgeShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.2.sp
                    )
                ) { append(timeDigits) }
                if (amPmLabel.isNotEmpty()) {
                    append("\u202F")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 8.5.sp,
                            baselineShift = BaselineShift.Superscript,
                            color = textColor.copy(alpha = 0.75f)
                        )
                    ) { append(amPmLabel) }
                }
            },
            color = textColor
        )
    }
}
