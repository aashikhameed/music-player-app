package com.aashik.music.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aashik.music.viewmodel.MusicViewModel

@Composable
fun SongLoadingBar(viewModel: MusicViewModel) {
    val loaded by viewModel.loadedCount.collectAsState()
    val total by viewModel.totalCount.collectAsState()

    if (total > 0 && loaded < total) {
        val progress = loaded.toFloat() / total

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Loading $loaded / $total songs (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
