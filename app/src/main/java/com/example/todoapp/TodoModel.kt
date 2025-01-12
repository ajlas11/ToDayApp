package com.example.todoapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TodoModel")
data class TodoModel(
    var title: String,
    var description: String,
    var priority: String, // Priority field retained
    var date: Long = 0L,
    var time: Long = 0L,
    var isFinished: Int = 0, // Represents task completion status
    var isDeleted: Int = 0, // Represents if the task is deleted
    var userId: Int, // ID of the user who owns the task
    var completed: Boolean = false, // Tracks task completion
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0 // Unique identifier for each task
) {
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(priority in listOf("Low", "Medium", "High")) { "Priority must be Low, Medium, or High" }
    }
}