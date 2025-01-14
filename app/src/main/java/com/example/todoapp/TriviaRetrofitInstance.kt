package com.example.todoapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Trivia API interface
interface TriviaApi {
    @GET("api.php?amount=1")
    suspend fun getRandomTrivia(): TriviaResponse
}

// Trivia Retrofit instance
object TriviaRetrofitInstance {
    private const val BASE_URL = "https://opentdb.com/"

    val api: TriviaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TriviaApi::class.java)
    }
}
