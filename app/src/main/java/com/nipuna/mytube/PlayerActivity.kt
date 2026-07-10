package com.nipuna.mytube

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvDuration: TextView

    private var isPlaying = false
    private var userSeeking = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        webView = findViewById(R.id.webView)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvDuration = findViewById(R.id.tvDuration)

        val videoId = intent.getStringExtra("VIDEO_ID") ?: ""

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(PlayerBridge(), "Android")

        val html = getPlayerHtml(videoId)
        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)

        btnPlayPause.setOnClickListener {
            if (isPlaying) {
                webView.evaluateJavascript("pauseVideo();", null)
            } else {
                webView.evaluateJavascript("playVideo();", null)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) tvCurrentTime.text = formatTime(progress.toDouble())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) { userSeeking = true }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                userSeeking = false
                sb?.let { webView.evaluateJavascript("seekTo(${it.progress});", null) }
            }
        })
    }

    private fun getPlayerHtml(videoId: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head><style>body,html{margin:0;padding:0;background:#000;}</style></head>
            <body>
            <div id="player"></div>
            <script>
            var tag = document.createElement('script');
            tag.src = "https://www.youtube.com/iframe_api";
            var firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

            var player;
            function onYouTubeIframeAPIReady() {
              player = new YT.Player('player', {
                height: '100%',
                width: '100%',
                videoId: '$videoId',
                playerVars: { 'controls': 0, 'autoplay': 1, 'rel': 0, 'playsinline': 1 },
                events: {
                  'onReady': onPlayerReady,
                  'onStateChange': onPlayerStateChange
                }
              });
            }

            function onPlayerReady(event) {
              Android.onReady(event.target.getDuration());
              setInterval(function() {
                if (player && player.getCurrentTime) {
                  Android.onProgress(player.getCurrentTime(), player.getDuration());
                }
              }, 500);
            }

            function onPlayerStateChange(event) {
              Android.onStateChange(event.data);
            }

            function playVideo() { player.playVideo(); }
            function pauseVideo() { player.pauseVideo(); }
            function seekTo(seconds) { player.seekTo(seconds, true); }
            </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun formatTime(seconds: Double): String {
        val totalSeconds = seconds.toInt()
        val minutes = totalSeconds / 60
        val secs = totalSeconds % 60
        return String.format("%d:%02d", minutes, secs)
    }

    inner class PlayerBridge {
        @JavascriptInterface
        fun onReady(durationSeconds: Double) {
            runOnUiThread {
                seekBar.max = durationSeconds.toInt()
                tvDuration.text = formatTime(durationSeconds)
            }
        }

        @JavascriptInterface
        fun onProgress(current: Double, total: Double) {
            runOnUiThread {
                if (!userSeeking) {
                    seekBar.progress = current.toInt()
                    tvCurrentTime.text = formatTime(current)
                }
            }
        }

        @JavascriptInterface
        fun onStateChange(state: Int) {
            runOnUiThread {
                isPlaying = state == 1
                btnPlayPause.setImageResource(
                    if (isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                )
            }
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
