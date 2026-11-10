package com.aashik.music.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SpatialAudio
import com.aashik.music.ui.designsystem.FilterChip
import com.aashik.music.ui.designsystem.FilterChipDefaults
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.IconButton
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.viewmodel.MusicViewModel

private val EqChipShape = RoundedCornerShape(8.dp)

@Composable
fun BalanceFaderCard(viewModel: MusicViewModel) {
    val balance by viewModel.balance.collectAsState()
    val fader by viewModel.fader.collectAsState()

    DspCard(title = "4-Way Balance & Fader Acoustic Grid", icon = Icons.Rounded.SpatialAudio) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Preset Focus Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = (balance == 0f && fader == 0f),
                    onClick = { viewModel.setAcousticFocus(0f, 0f) },
                    label = { Text("Center", fontSize = 10.5.sp) },
                    shape = EqChipShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(30.dp)
                )

                FilterChip(
                    selected = (balance == -0.35f && fader == 0.25f),
                    onClick = { viewModel.setAcousticFocus(-0.35f, 0.25f) },
                    label = { Text("Driver LHD", fontSize = 10.5.sp) },
                    shape = EqChipShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(30.dp)
                )

                FilterChip(
                    selected = (balance == 0.35f && fader == 0.25f),
                    onClick = { viewModel.setAcousticFocus(0.35f, 0.25f) },
                    label = { Text("Driver RHD", fontSize = 10.5.sp) },
                    shape = EqChipShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(30.dp)
                )

                FilterChip(
                    selected = (balance == 0f && fader == 0.5f),
                    onClick = { viewModel.setAcousticFocus(0f, 0.5f) },
                    label = { Text("Front", fontSize = 10.5.sp) },
                    shape = EqChipShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(30.dp)
                )

                FilterChip(
                    selected = (balance == 0f && fader == -0.5f),
                    onClick = { viewModel.setAcousticFocus(0f, -0.5f) },
                    label = { Text("Rear", fontSize = 10.5.sp) },
                    shape = EqChipShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(30.dp)
                )

                IconButton(
                    onClick = { viewModel.resetBalanceFader() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset Balance and Fader",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2D Touch Grid Canvas & Sliders
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 480.dp

                if (isCompact) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Interactive 2D Soundstage Touchpad
                        AcousticTouchPad(
                            balance = balance,
                            fader = fader,
                            onUpdate = { newBal, newFad ->
                                viewModel.setBalance(newBal)
                                viewModel.setFader(newFad)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )

                        // Fine Adjustment Sliders
                        AcousticFineSliders(
                            balance = balance,
                            fader = fader,
                            onBalanceChange = { viewModel.setBalance(it) },
                            onFaderChange = { viewModel.setFader(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AcousticTouchPad(
                            balance = balance,
                            fader = fader,
                            onUpdate = { newBal, newFad ->
                                viewModel.setBalance(newBal)
                                viewModel.setFader(newFad)
                            },
                            modifier = Modifier.size(width = 180.dp, height = 130.dp)
                        )

                        AcousticFineSliders(
                            balance = balance,
                            fader = fader,
                            onBalanceChange = { viewModel.setBalance(it) },
                            onFaderChange = { viewModel.setFader(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

