package com.nipuna.mytube

import android.os.Bundle
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView

class PlayerActivity : YouTubeBaseActivity(), YouTubePlayer.OnInitializedListener {

    private val API_KEY = BuildConfig.YOUTUBE_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val playerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        playerView.initialize(API_KEY, this)
    }

    override fun onInitializationSuccess(
        provider: YouTubePlayer.Provider?,
        player: YouTubePlayer?,
        wasRestored: Boolean
    ) {
        val videoId = intent.getStringExtra("VIDEO_ID")
        if (!wasRestored && videoId != null) {
            player?.cueVideo(videoId)
        }
    }

    override fun onInitializationFailure(
        provider: YouTubePlayer.Provider?,
        result: YouTubeInitializationResult?
    ) {
    }
}
