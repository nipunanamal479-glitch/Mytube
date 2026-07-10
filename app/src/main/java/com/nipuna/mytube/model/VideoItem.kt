package com.nipuna.mytube.model

data class SearchResponse(
    val items: List<SearchItem> = emptyList(),
    val nextPageToken: String? = null
)

data class SearchItem(
    val id: VideoId,
    val snippet: Snippet
)

data class VideoId(
    val videoId: String? = null
)

data class Snippet(
    val title: String,
    val channelTitle: String,
    val thumbnails: Thumbnails
)

data class Thumbnails(
    val medium: Thumbnail,
    val high: Thumbnail
)

data class Thumbnail(
    val url: String
)
