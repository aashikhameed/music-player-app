package com.aashik.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GridBorderShape = RoundedCornerShape(12.dp)

@Composable
fun AcousticTouchPad(
    balance: Float,
    fader: Float,
    onUpdate: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridBgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .background(gridBgColor, shape = GridBorderShape)
            .border(1.dp, gridLineColor, GridBorderShape)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = ((offset.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
                    val normY = -((offset.y / size.height) * 2f - 1f).coerceIn(-1f, 1f)
                    onUpdate(normX, normY)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val normX = ((change.position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
                    val normY = -((change.position.y / size.height) * 2f - 1f).coerceIn(-1f, 1f)
                    onUpdate(normX, normY)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Crosshairs
            drawLine(
                color = gridLineColor,
                start = Offset(cx, 0f),
                end = Offset(cx, h),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridLineColor,
                start = Offset(0f, cy),
                end = Offset(w, cy),
                strokeWidth = 1.dp.toPx()
            )

            // 4 Speaker Corner Indicators
            val speakerColor = primaryColor.copy(alpha = 0.35f)
            val spRad = 4.dp.toPx()
            drawCircle(speakerColor, spRad, Offset(16.dp.toPx(), 16.dp.toPx()))
            drawCircle(speakerColor, spRad, Offset(w - 16.dp.toPx(), 16.dp.toPx()))
            drawCircle(speakerColor, spRad, Offset(16.dp.toPx(), h - 16.dp.toPx()))
            drawCircle(speakerColor, spRad, Offset(w - 16.dp.toPx(), h - 16.dp.toPx()))

            // Focal Puck Position (balance maps to x, fader maps to -y)
            val puckX = cx + balance * (cx - 18.dp.toPx())
            val puckY = cy - fader * (cy - 18.dp.toPx())

            // Outer acoustic halo
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = 16.dp.toPx(),
                center = Offset(puckX, puckY)
            )
            // Inner focal puck
            drawCircle(
                color = primaryColor,
                radius = 8.dp.toPx(),
                center = Offset(puckX, puckY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(puckX, puckY)
            )
        }

        // Cabin Axis Labels
        Text(
            text = "FRONT",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
        )
        Text(
            text = "REAR",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
        )
        Text(
            text = "L",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
        )
        Text(
            text = "R",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
        )
    }
}

