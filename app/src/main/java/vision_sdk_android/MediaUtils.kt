package vision_sdk_android

import android.content.Context
import android.media.MediaPlayer
import com.example.barcodescannernew.R


class MediaUtils(private val context: Context) {
    private val player: MediaPlayer = MediaPlayer.create(context, R.raw.beep_sound)

    fun getMediaPlayer(): MediaPlayer {
        return player
    }
}