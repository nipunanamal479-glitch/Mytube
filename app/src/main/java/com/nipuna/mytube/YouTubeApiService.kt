package com.nipuna.mytube

import com.nipuna.mytube.model.SearchResponse
import com.nipuna.mytube.model.VideoListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String
    ): Response<SearchResponse>

    @GET("videos")
    suspend fun getTrendingVideos(
        @Query("part") part: String = "snippet",
        @Query("chart") chart: String = "mostPopular",
        @Query("regionCode") regionCode: String = "LK",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String
    ): Response<VideoListResponse>
}
