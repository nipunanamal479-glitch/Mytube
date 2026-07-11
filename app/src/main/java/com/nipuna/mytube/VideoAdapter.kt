package com.nipuna.mytube

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.nipuna.mytube.model.VideoUiModel

class VideoAdapter(
    private val onVideoClick: (videoId: String, title: String) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private val videos = mutableListOf<VideoUiModel>()
    private var lastAnimatedPosition = -1

    fun submitList(newVideos: List<VideoUiModel>) {
        videos.clear()
        videos.addAll(newVideos)
        lastAnimatedPosition = -1
        notifyDataSetChanged()
    }

    fun clear() {
        videos.clear()
        lastAnimatedPosition = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = videos[position]
        holder.bind(item)

        if (position > lastAnimatedPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.card_animation)
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = position
        }

        holder.itemView.setOnClickListener {
            onVideoClick(item.videoId, item.title)
        }
    }

    override fun getItemCount(): Int = videos.size

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.thumbnailImageView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val channelTextView: TextView = itemView.findViewById(R.id.channelTextView)
        private val channelInitialTextView: TextView = itemView.findViewById(R.id.channelInitialTextView)

        fun bind(item: VideoUiModel) {
            titleTextView.text = item.title
            channelTextView.text = item.channelTitle
            channelInitialTextView.text = item.channelTitle
                .trim()
                .take(1)
                .uppercase()

            Glide.with(itemView.context)
                .load(item.thumbnailUrl)
                .apply(RequestOptions().centerCrop())
                .into(thumbnailImageView)
        }
    }
}
