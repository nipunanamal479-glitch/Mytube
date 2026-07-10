package com.nipuna.mytube

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyStateLayout: FrameLayout

    private val homeUrl = "https://m.youtube.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Global crash catcher - error eka screen ekema penna
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                try {
                    showCrashScreen(throwable.stackTraceToString())
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            webView = findViewById(R.id.webView)
            progressBar = findViewById(R.id.progressBar)
            swipeRefresh = findViewById(R.id.swipeRefresh)
            emptyStateLayout = findViewById(R.id.emptyStateLayout)

            setupWebView()
            setupSwipeRefresh()

            if (savedInstanceState != null) {
                webView.restoreState(savedInstanceState)
            } else {
                webView.loadUrl(homeUrl)
            }
        } catch (e: Exception) {
            showCrashScreen(e.stackTraceToString())
        }
    }

    private fun showCrashScreen(errorText: String) {
        val scrollView = ScrollView(this)
        val textView = TextView(this)
        textView.text = "CRASH ERROR:\n\n$errorText"
        textView.setTextColor(Color.WHITE)
        textView.setBackgroundColor(Color.BLACK)
        textView.textSize = 12f
        textView.setPadding(24, 24, 24, 24)
        textView.setTextIsSelectable(true)
        scrollView.addView(textView)
        setContentView(scrollView)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                emptyStateLayout.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    emptyStateLayout.visibility = View.VISIBLE
                }
            }
        }

        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.accent_teal)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_dark)
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) {
            webView.saveState(outState)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }
}
