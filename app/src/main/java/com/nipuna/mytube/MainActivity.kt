package com.nipuna.mytube

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
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
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nipuna.mytube.model.toUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateTextView: TextView
    private lateinit var suggestionsCard: CardView
    private lateinit var suggestionsRecyclerView: RecyclerView

    private lateinit var adapter: VideoAdapter
    private lateinit var suggestionAdapter: SuggestionAdapter

    private var lastQuery: String = ""
    private var isShowingTrending = true

    private val suggestHandler = Handler(Looper.getMainLooper())
    private var suggestRunnable: Runnable? = null

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
            suggestionsCard = findViewById(R.id.suggestionsCard)
            suggestionsRecyclerView = findViewById(R.id.suggestionsRecyclerView)

            setupRecyclerView()
            setupSuggestions()
            setupSearch()
            setupSwipeRefresh()

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

    private fun setupSuggestions() {
        suggestionAdapter = SuggestionAdapter { suggestion ->
            searchEditText.setText(suggestion)
            searchEditText.setSelection(suggestion.length)
            suggestionsCard.visibility = View.GONE
            performSearch(suggestion)
        }
        suggestionsRecyclerView.layoutManager = LinearLayoutManager(this)
        suggestionsRecyclerView.adapter = suggestionAdapter
    }

    private fun setupSearch() {
        searchButton.setOnClickListener {
            suggestionsCard.visibility = View.GONE
            performSearch()
        }

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                suggestionsCard.visibility = View.GONE
                performSearch()
                true
            } else {
                false
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty().trim()

                suggestRunnable?.let { suggestHandler.removeCallbacks(it) }

                if (text.isBlank()) {
                    suggestionsCard.visibility = View.GONE
                    return
                }

                val runnable = Runnable { fetchSuggestions(text) }
                suggestRunnable = runnable
                suggestHandler.postDelayed(runnable, 300)
            }
        })
    }

    private fun fetchSuggestions(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SuggestClient.instance.getSuggestions(query = query)
                }

                if (response.isSuccessful) {
                    val raw = response.body().orEmpty()
                    val suggestions = parseSuggestions(raw)
                    if (suggestions.isNotEmpty()) {
                        suggestionAdapter.submitList(suggestions)
                        suggestionsCard.visibility = View.VISIBLE
                    } else {
                        suggestionsCard.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                suggestionsCard.visibility = View.GONE
            }
        }
    }

    private fun parseSuggestions(raw: String): List<String> {
        return try {
            val jsonArray = JSONArray(raw)
            val suggestionsArray = jsonArray.getJSONArray(1)
            val list = mutableListOf<String>()
            for (i in 0 until suggestionsArray.length()) {
                val item = suggestionsArray.get(i)
                val text = if (item is JSONArray) item.getString(0) else item.toString()
                list.add(text)
            }
            list.take(8)
        } catch (e: Exception) {
            emptyList()
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

        suggestionsCard.visibility = View.GONE
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
