package com.nipuna.mytube

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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

    private val playerOrigin = "https://www.youtube.com"

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

        playerWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        playerWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                playerProgressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                playerProgressBar.visibility = View.GONE
            }
        }

        playerWebView.webChromeClient = WebChromeClient()

        if (!videoId.isNullOrBlank()) {
            val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&origin=$playerOrigin"
            val headers = mapOf("Referer" to playerOrigin)
            playerWebView.loadUrl(embedUrl, headers)
        } else {
            playerProgressBar.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        playerWebView.destroy()
        super.onDestroy()
    }
}
