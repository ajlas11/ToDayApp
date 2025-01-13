package com.example.todoapp

import retrofit2.Call
import retrofit2.http.GET

interface JokeAPI {
    @GET("joke/Any")
    fun getRandomJoke(): Call<JokeResponse>
}
