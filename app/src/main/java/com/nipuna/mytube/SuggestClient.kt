package com.nipuna.mytube

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object SuggestClient {
    private const val BASE_URL = "https://suggestqueries.google.com/"

    val instance: SuggestApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(SuggestApiService::class.java)
    }
}
