package com.example.todoapp

data class JokeResponse(
    val error: Boolean,
    val type: String,
    val joke: String?,
    val setup: String?,
    val delivery: String?
)
