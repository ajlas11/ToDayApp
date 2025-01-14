package com.example.todoapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface JokeAPI {
    @GET("joke/Programming")
    fun getProgrammingJoke(
        @Query("type") type: String = "single" // You can also use "twopart" if you want two-part jokes
    ): Call<JokeResponse>
}
