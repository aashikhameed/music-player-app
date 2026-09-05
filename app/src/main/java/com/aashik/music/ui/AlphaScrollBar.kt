package com.aashik.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AlphabetChars = ('A'..'Z').toList()
private val AlphabetStrings = ('A'..'Z').map { it.toString() }

@Composable
fun AlphaScrollBar(
    modifier: Modifier = Modifier,
    onLetterClick: (Char) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlphabetChars.forEachIndexed { index, letter ->
            val letterStr = AlphabetStrings[index]
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onLetterClick(letter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letterStr,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
