package com.nipuna.mytube

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VideoAdapter(
    private val videos: List<VideoItem>,
    private val onClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val title: TextView = view.findViewById(R.id.videoTitle)
        val channel: TextView = view.findViewById(R.id.channelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.title.text = video.snippet.title
        holder.channel.text = video.snippet.channelTitle
        Glide.with(holder.itemView.context)
            .load(video.snippet.thumbnails.medium.url)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener { onClick(video) }
    }

    override fun getItemCount() = videos.size
}
