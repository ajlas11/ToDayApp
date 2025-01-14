package com.example.todoapp

data class JokeResponse(
    val error: Boolean,
    val type: String,
    val joke: String?, // For single-type jokes
    val setup: String?, // For two-part jokes
    val delivery: String? // For two-part jokes
)
