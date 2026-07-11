package com.nipuna.mytube

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nipuna.mytube.model.toUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerWebView: WebView
    private lateinit var playerProgressBar: ProgressBar
    private lateinit var playerTitleTextView: TextView
    private lateinit var backButton: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var captionsButton: ImageButton
    private lateinit var qualityButton: ImageButton
    private lateinit var shareButton: ImageButton
    private lateinit var playerSeekBar: SeekBar
    private lateinit var currentTimeTextView: TextView
    private lateinit var durationTextView: TextView
    private lateinit var suggestedRecyclerView: RecyclerView

    private lateinit var suggestedAdapter: VideoAdapter

    private var videoId: String = ""
    private var videoTitle: String = ""
    private var isPlaying = true
    private var captionsOn = false
    private var isUserSeeking = false
    private var videoDurationSeconds = 0

    private val playerOrigin = "https://mytube.app"

    private val apiKey: String
        get() = BuildConfig.YOUTUBE_API_KEY

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerWebView = findViewById(R.id.playerWebView)
        playerProgressBar = findViewById(R.id.playerProgressBar)
        playerTitleTextView = findViewById(R.id.playerTitleTextView)
        backButton = findViewById(R.id.backButton)
        playPauseButton = findViewById(R.id.playPauseButton)
        captionsButton = findViewById(R.id.captionsButton)
        qualityButton = findViewById(R.id.qualityButton)
        shareButton = findViewById(R.id.shareButton)
        playerSeekBar = findViewById(R.id.playerSeekBar)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        durationTextView = findViewById(R.id.durationTextView)
        suggestedRecyclerView = findViewById(R.id.suggestedRecyclerView)

        videoId = intent.getStringExtra("VIDEO_ID").orEmpty()
        videoTitle = intent.getStringExtra("VIDEO_TITLE") ?: "Now Playing"
        playerTitleTextView.text = videoTitle

        backButton.setOnClickListener { finish() }

        setupWebView()
        setupControls()
        setupSuggestedVideos()

        if (videoId.isNotBlank()) {
            loadPlayer(videoId)
            loadSuggestedVideos(videoTitle)
        } else {
            playerProgressBar.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        playerWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        playerWebView.addJavascriptInterface(PlayerBridge(), "Android")

        playerWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                playerProgressBar.visibility = View.GONE
            }
        }
        playerWebView.webChromeClient = WebChromeClient()
    }

    private fun loadPlayer(id: String) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                html,body{margin:0;padding:0;background:#000;width:100%;height:100%;overflow:hidden;}
                #player{width:100%;height:100%;}
              </style>
            </head>
            <body>
              <div id="player"></div>
              <script src="https://www.youtube.com/iframe_api"></script>
              <script>
                var player;
                function onYouTubeIframeAPIReady() {
                  player = new YT.Player('player', {
                    videoId: '$id',
                    playerVars: {
                      'autoplay': 1,
                      'playsinline': 1,
                      'controls': 0,
                      'rel': 0,
                      'modestbranding': 1,
                      'iv_load_policy': 3,
                      'origin': '$playerOrigin'
                    },
                    events: {
                      'onReady': onPlayerReady,
                      'onStateChange': onPlayerStateChange
                    }
                  });
                }
                function onPlayerReady(event) {
                  Android.onReady(String(event.target.getDuration()));
                  setInterval(function() {
                    try {
                      if (player && player.getCurrentTime) {
                        Android.onTimeUpdate(String(player.getCurrentTime()), String(player.getDuration()));
                      }
                    } catch (e) {}
                  }, 500);
                }
                function onPlayerStateChange(event) {
                  Android.onStateChange(String(event.data));
                }
                function nativePlay() { if (player) player.playVideo(); }
                function nativePause() { if (player) player.pauseVideo(); }
                function nativeSeekTo(seconds) { if (player) player.seekTo(seconds, true); }
                function nativeSetQuality(level) { if (player) player.setPlaybackQuality(level); }
                function nativeToggleCaptions(enable) {
                  if (!player) return;
                  if (enable) {
                    player.loadModule('captions');
                    player.setOption('captions', 'reload', true);
                    player.setOption('captions', 'track', {});
                  } else {
                    player.unloadModule('captions');
                  }
                }
              </script>
            </body>
            </html>
        """.trimIndent()

        playerWebView.loadDataWithBaseURL(playerOrigin, html, "text/html", "utf-8", null)
    }

    private fun setupControls() {
        playPauseButton.setOnClickListener {
            if (isPlaying) {
                playerWebView.evaluateJavascript("nativePause();", null)
            } else {
                playerWebView.evaluateJavascript("nativePlay();", null)
            }
        }

        captionsButton.setOnClickListener {
            captionsOn = !captionsOn
            playerWebView.evaluateJavascript("nativeToggleCaptions($captionsOn);", null)
            captionsButton.alpha = if (captionsOn) 1.0f else 0.5f
        }
        captionsButton.alpha = 0.5f

        qualityButton.setOnClickListener { showQualityMenu() }

        shareButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, "https://youtu.be/$videoId")
            startActivity(Intent.createChooser(shareIntent, "Share video via"))
        }

        playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentTimeTextView.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                val seekToSeconds = seekBar?.progress ?: 0
                playerWebView.evaluateJavascript("nativeSeekTo($seekToSeconds);", null)
            }
        })
    }

    private fun showQualityMenu() {
        val popup = PopupMenu(this, qualityButton)
        val qualities = listOf(
            "Auto" to "default",
            "1080p" to "hd1080",
            "720p" to "hd720",
            "480p" to "large",
            "360p" to "medium"
        )
        qualities.forEachIndexed { index, (label, _) ->
            popup.menu.add(0, index, index, label)
        }
        popup.setOnMenuItemClickListener { item ->
            val (_, value) = qualities[item.itemId]
            playerWebView.evaluateJavascript("nativeSetQuality('$value');", null)
            true
        }
        popup.show()
    }

    private fun setupSuggestedVideos() {
        suggestedAdapter = VideoAdapter { newVideoId, newTitle ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("VIDEO_ID", newVideoId)
            intent.putExtra("VIDEO_TITLE", newTitle)
            startActivity(intent)
        }
        suggestedRecyclerView.layoutManager = LinearLayoutManager(this)
        suggestedRecyclerView.adapter = suggestedAdapter
    }

    private fun loadSuggestedVideos(title: String) {
        if (apiKey.isBlank()) return

        val keywords = title.split(" ", "|", "-", "(", ")")
            .filter { it.trim().length > 2 }
            .take(4)
            .joinToString(" ")

        val query = keywords.ifBlank { title }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.searchVideos(query = query, apiKey = apiKey)
                }
                if (response.isSuccessful) {
                    val items = response.body()?.items.orEmpty()
                        .mapNotNull { it.toUiModel() }
                        .filter { it.videoId != videoId }
                    suggestedAdapter.submitList(items)
                }
            } catch (e: Exception) {
                // silently ignore - suggestions are non-critical
            }
        }
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    inner class PlayerBridge {
        @JavascriptInterface
        fun onReady(durationStr: String) {
            val duration = durationStr.toDoubleOrNull()?.toInt() ?: 0
            videoDurationSeconds = duration
            runOnUiThread {
                playerSeekBar.max = duration
                durationTextView.text = formatTime(duration)
            }
        }

        @JavascriptInterface
        fun onTimeUpdate(currentStr: String, durationStr: String) {
            val current = currentStr.toDoubleOrNull()?.toInt() ?: 0
            val duration = durationStr.toDoubleOrNull()?.toInt() ?: videoDurationSeconds
            runOnUiThread {
                if (!isUserSeeking) {
                    if (playerSeekBar.max != duration && duration > 0) {
                        playerSeekBar.max = duration
                        durationTextView.text = formatTime(duration)
                    }
                    playerSeekBar.progress = current
                    currentTimeTextView.text = formatTime(current)
                }
            }
        }

        @JavascriptInterface
        fun onStateChange(stateStr: String) {
            val state = stateStr.toIntOrNull() ?: return
            runOnUiThread {
                when (state) {
                    1 -> {
                        isPlaying = true
                        playPauseButton.setImageResource(R.drawable.ic_control_pause)
                    }
                    2, 0 -> {
                        isPlaying = false
                        playPauseButton.setImageResource(R.drawable.ic_control_play)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        playerWebView.destroy()
        super.onDestroy()
    }
}
