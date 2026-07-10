package com.nipuna.mytube

data class SearchResponse(val items: List<VideoItem>)
data class VideoItem(val id: VideoId, val snippet: Snippet)
data class VideoId(val videoId: String?)
data class Snippet(
    val title: String,
    val channelTitle: String,
    val thumbnails: Thumbnails
)
data class Thumbnails(val medium: Thumbnail)
data class Thumbnail(val url: String)
