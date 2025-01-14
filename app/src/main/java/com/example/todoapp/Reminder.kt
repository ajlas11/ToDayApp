package com.example.todoapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Reminder",
    foreignKeys = [
        ForeignKey(
            entity = TodoModel::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val reminderTime: Long
)
