package com.nipuna.mytube

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SuggestApiService {

    @GET("complete/search")
    suspend fun getSuggestions(
        @Query("client") client: String = "youtube",
        @Query("ds") ds: String = "yt",
        @Query("q") query: String
    ): Response<String>
}
