package com.example.todoapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TodoModel")
data class TodoModel(
    var title: String,
    var description: String,
    var priority: String,
    var date: Long = 0L,
    var time: Long = 0L,
    var isFinished: Int = 0,
    var isDeleted: Int = 0,
    var userId: Int,
    var completed: Boolean = false,
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
) {
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(priority in listOf("Low", "Medium", "High")) { "Priority must be Low, Medium, or High" }
    }
}