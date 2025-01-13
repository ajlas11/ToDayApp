package com.example.todoapp

data class JokeResponse(
    val setup: String?,
    val delivery: String?,
    val joke: String? // For single-line jokes
)
