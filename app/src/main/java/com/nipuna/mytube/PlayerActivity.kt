package com.nipuna.mytube

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerWebView: WebView
    private lateinit var playerProgressBar: ProgressBar
    private lateinit var playerTitleTextView: TextView
    private lateinit var backButton: ImageButton

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerWebView = findViewById(R.id.playerWebView)
        playerProgressBar = findViewById(R.id.playerProgressBar)
        playerTitleTextView = findViewById(R.id.playerTitleTextView)
        backButton = findViewById(R.id.backButton)

        val videoId = intent.getStringExtra("VIDEO_ID")
        val title = intent.getStringExtra("VIDEO_TITLE") ?: "Now Playing"
        playerTitleTextView.text = title

        backButton.setOnClickListener { finish() }

        playerWebView.settings.javaScriptEnabled = true
        playerWebView.settings.mediaPlaybackRequiresUserGesture = false
        playerWebView.settings.domStorageEnabled = true

        playerWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                playerProgressBar.visibility = View.GONE
            }
        }

        playerWebView.webChromeClient = WebChromeClient()

        if (!videoId.isNullOrBlank()) {
            val embedHtml = """
                <html>
                <body style="margin:0;padding:0;background:#000;">
                <iframe width="100%" height="100%"
                    src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1"
                    frameborder="0"
                    allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
                    allowfullscreen>
                </iframe>
                </body>
                </html>
            """.trimIndent()
            playerWebView.loadDataWithBaseURL(
                "https://www.youtube.com",
                embedHtml,
                "text/html",
                "utf-8",
                null
            )
        } else {
            playerProgressBar.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        playerWebView.destroy()
        super.onDestroy()
    }
}
