package com.aashik.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Slider
import com.aashik.music.ui.designsystem.SliderDefaults
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun AcousticFineSliders(
    balance: Float,
    fader: Float,
    onBalanceChange: (Float) -> Unit,
    onFaderChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Balance Slider
        val balPercent = (balance * 100).roundToInt()
        val balText = when {
            balPercent < 0 -> "Left ${-balPercent}%"
            balPercent > 0 -> "Right $balPercent%"
            else -> "Center"
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Balance (L ↔ R)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = balText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = balance,
            onValueChange = onBalanceChange,
            valueRange = -1.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth().height(26.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Fader Slider
        val fadPercent = (fader * 100).roundToInt()
        val fadText = when {
            fadPercent > 0 -> "Front +$fadPercent%"
            fadPercent < 0 -> "Rear ${fadPercent}%"
            else -> "Center"
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fader (Rear ↔ Front)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = fadText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = fader,
            onValueChange = onFaderChange,
            valueRange = -1.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth().height(26.dp)
        )
    }
}

