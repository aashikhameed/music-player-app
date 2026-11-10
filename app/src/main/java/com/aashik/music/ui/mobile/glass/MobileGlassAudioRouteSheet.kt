package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Airplay
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AudioOutputDevice(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val batteryPct: Int? = null,
    val isConnected: Boolean = false
)

/**
 * Mobile AirPlay Audio Output Selector Sheet (Modal Glass Sheet).
 * Displays output devices: AirPods Max, iPhone Speaker, MacBook Pro with battery levels.
 */
@Composable
fun MobileGlassAudioRouteSheet(
    devices: List<AudioOutputDevice>,
    selectedDeviceId: String,
    onSelectDevice: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .shadow(
                elevation = 24.dp,
                shape = sheetShape,
                ambientColor = Color.Black.copy(alpha = 0.50f),
                spotColor = Color.Black.copy(alpha = 0.65f)
            )
            .mobileLiquidGlass(
                shape = sheetShape,
                tintColor = Color(0xFF16181C).copy(alpha = 0.90f),
                borderColor = Color.White.copy(alpha = 0.16f)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Airplay,
                        contentDescription = "AirPlay",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Audio Output",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Done",
                    color = Color(0xFF00E5FF),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                )
            }

            // Device List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                devices.forEach { device ->
                    val isSelected = device.id == selectedDeviceId
                    val rowShape = RoundedCornerShape(14.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .mobileLiquidGlass(
                                shape = rowShape,
                                tintColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
                                borderColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectDevice(device.id) }
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = device.icon,
                                    contentDescription = device.name,
                                    tint = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = device.name,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (device.batteryPct != null) {
                                    Text(
                                        text = "${device.batteryPct}%",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
