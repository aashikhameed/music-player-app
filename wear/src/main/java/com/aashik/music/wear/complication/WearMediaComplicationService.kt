package com.aashik.music.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.aashik.music.wear.player.WearMusicPlayer
import com.aashik.music.wear.presentation.WearMainActivity

class WearMediaComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return if (type == ComplicationType.SHORT_TEXT) {
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("Music").build(),
                PlainComplicationText.Builder("Now Playing").build()
            ).build()
        } else {
            null
        }
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        if (request.complicationType != ComplicationType.SHORT_TEXT) {
            listener.onComplicationData(null)
            return
        }

        val intent = Intent(this, WearMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val state = WearMusicPlayer.state.value
        val title = state.currentSong?.title ?: "Music"
        val status = if (state.isPlaying) "Playing" else "Paused"

        val data = ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(title.take(7)).build(),
            PlainComplicationText.Builder(status).build()
        )
            .setTapAction(pendingIntent)
            .build()

        listener.onComplicationData(data)
    }
}
