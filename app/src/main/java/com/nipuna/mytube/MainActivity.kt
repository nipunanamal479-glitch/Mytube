package com.nipuna.mytube

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val API_KEY = BuildConfig.YOUTUBE_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchBox = findViewById<EditText>(R.id.searchBox)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        searchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchBox.text.toString()
                if (query.isNotBlank()) searchYoutube(query, recyclerView)
                true
            } else false
        }

        searchYoutube("trending sri lanka", recyclerView)
    }

    private fun searchYoutube(query: String, recyclerView: RecyclerView) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.searchVideos(
                    query = query,
                    apiKey = API_KEY
                )
                val adapter = VideoAdapter(response.items) { video ->
                    val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                    intent.putExtra("VIDEO_ID", video.id.videoId)
                    startActivity(intent)
                }
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
