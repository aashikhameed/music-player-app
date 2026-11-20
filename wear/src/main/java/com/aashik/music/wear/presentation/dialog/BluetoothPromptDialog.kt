package com.aashik.music.wear.presentation.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSecondaryAmber

@Composable
fun BluetoothPromptDialog(
    onConnectClicked: () -> Unit,
    onDismissClicked: () -> Unit
) {
    Alert(
        title = {
            Text(
                text = "Bluetooth Required",
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                color = WearSecondaryAmber
            )
        },
        positiveButton = {
            Button(
                onClick = onConnectClicked,
                colors = ButtonDefaults.primaryButtonColors(
                    backgroundColor = WearPrimaryCyan,
                    contentColor = Color.Black
                ),
                modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Connect Bluetooth"
                )
            }
        },
        negativeButton = {
            Button(
                onClick = onDismissClicked,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss"
                )
            }
        }
    ) {
        Text(
            text = "Connect Bluetooth headphones to play music.",
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
