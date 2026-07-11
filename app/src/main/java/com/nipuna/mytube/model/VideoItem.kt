package com.nipuna.mytube.model

// ---- Search endpoint response (search.list) ----
data class SearchResponse(
    val items: List<SearchItem> = emptyList()
)

data class SearchItem(
    val id: VideoId,
    val snippet: Snippet
)

data class VideoId(
    val videoId: String? = null
)

// ---- Trending endpoint response (videos.list) ----
data class VideoListResponse(
    val items: List<VideoListItem> = emptyList()
)

data class VideoListItem(
    val id: String,
    val snippet: Snippet
)

// ---- Shared ----
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

// ---- UI model used by the adapter (both search & trending map to this) ----
data class VideoUiModel(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String
)

fun SearchItem.toUiModel(): VideoUiModel? {
    val videoId = this.id.videoId ?: return null
    return VideoUiModel(
        videoId = videoId,
        title = snippet.title,
        channelTitle = snippet.channelTitle,
        thumbnailUrl = snippet.thumbnails.high.url
    )
}

fun VideoListItem.toUiModel(): VideoUiModel {
    return VideoUiModel(
        videoId = id,
        title = snippet.title,
        channelTitle = snippet.channelTitle,
        thumbnailUrl = snippet.thumbnails.high.url
    )
}
