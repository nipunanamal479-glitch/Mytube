package com.nipuna.mytube

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nipuna.mytube.model.toUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateTextView: TextView

    private lateinit var adapter: VideoAdapter
    private var lastQuery: String = ""
    private var isShowingTrending = true

    private val apiKey: String
        get() = BuildConfig.YOUTUBE_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
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

            searchEditText = findViewById(R.id.searchEditText)
            searchButton = findViewById(R.id.searchButton)
            recyclerView = findViewById(R.id.recyclerView)
            swipeRefresh = findViewById(R.id.swipeRefresh)
            loadingProgressBar = findViewById(R.id.loadingProgressBar)
            emptyStateLayout = findViewById(R.id.emptyStateLayout)
            emptyStateTextView = findViewById(R.id.emptyStateTextView)

            setupRecyclerView()
            setupSearch()
            setupSwipeRefresh()

            // App open unama trending videos auto load wenawa
            loadTrendingVideos()

        } catch (e: Exception) {
            showCrashScreen(e.stackTraceToString())
        }
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter { videoId, title ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("VIDEO_ID", videoId)
            intent.putExtra("VIDEO_TITLE", title)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchButton.setOnClickListener {
            performSearch()
        }

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.accent_teal)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_dark)
        swipeRefresh.setOnRefreshListener {
            if (isShowingTrending) {
                loadTrendingVideos()
            } else if (lastQuery.isNotBlank()) {
                performSearch(lastQuery)
            } else {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadTrendingVideos() {
        isShowingTrending = true
        emptyStateLayout.visibility = View.GONE
        loadingProgressBar.visibility = View.VISIBLE

        if (apiKey.isBlank()) {
            loadingProgressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            showEmptyState("API key eka missing. build.gradle check karanna.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getTrendingVideos(apiKey = apiKey)
                }

                loadingProgressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val items = response.body()?.items.orEmpty().map { it.toUiModel() }
                    if (items.isEmpty()) {
                        showEmptyState("Trending videos load wenne nae. Search karala try karanna.")
                    } else {
                        emptyStateLayout.visibility = View.GONE
                        adapter.submitList(items)
                    }
                } else {
                    showEmptyState("Error: HTTP ${response.code()}. API key/quota check karanna.")
                }
            } catch (e: Exception) {
                loadingProgressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                showEmptyState("Network error: ${e.message}")
            }
        }
    }

    private fun performSearch(overrideQuery: String? = null) {
        val query = overrideQuery ?: searchEditText.text.toString().trim()
        if (query.isBlank()) {
            Toast.makeText(this, "Search karanna videos type karanna", Toast.LENGTH_SHORT).show()
            return
        }

        isShowingTrending = false
        lastQuery = query
        emptyStateLayout.visibility = View.GONE
        loadingProgressBar.visibility = View.VISIBLE

        if (apiKey.isBlank()) {
            loadingProgressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            showEmptyState("API key eka missing. build.gradle check karanna.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.searchVideos(query = query, apiKey = apiKey)
                }

                loadingProgressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val items = response.body()?.items.orEmpty().mapNotNull { it.toUiModel() }
                    if (items.isEmpty()) {
                        showEmptyState("Results hambune nae. Wena keyword ekak try karanna.")
                    } else {
                        adapter.submitList(items)
                    }
                } else {
                    showEmptyState("Error: HTTP ${response.code()}. API key/quota check karanna.")
                }
            } catch (e: Exception) {
                loadingProgressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                showEmptyState("Network error: ${e.message}")
            }
        }
    }

    private fun showEmptyState(message: String) {
        adapter.clear()
        emptyStateTextView.text = message
        emptyStateLayout.visibility = View.VISIBLE
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
}
