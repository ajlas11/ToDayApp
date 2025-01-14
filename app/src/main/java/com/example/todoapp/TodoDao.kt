package com.example.todoapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TodoDao {

    // Insert a new task
    @Insert
    suspend fun insertTask(todoModel: TodoModel): Long

    // Get all unfinished and not deleted tasks
    @Query("SELECT * FROM TodoModel WHERE isFinished = 0 AND isDeleted = 0")
    suspend fun getTask(): List<TodoModel>

    // Get incomplete tasks for a specific user
    @Query("SELECT * FROM TodoModel WHERE userId = :userId AND isFinished = 0 AND isDeleted = 0")
    suspend fun getIncompleteTasksForUser(userId: Int): List<TodoModel>

    // Mark a task as finished
    @Query("UPDATE TodoModel SET isFinished = 1 WHERE id = :uid")
    suspend fun finishTask(uid: Long)

    // Mark a task as deleted
    @Query("UPDATE TodoModel SET isDeleted = 1 WHERE id = :uid")
    suspend fun markTaskAsDeleted(uid: Long)

    // Get deleted tasks for a specific user
    @Query("SELECT * FROM TodoModel WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedTasks(userId: Int): List<TodoModel>

    // Permanently delete a task
    @Query("DELETE FROM TodoModel WHERE id = :uid")
    suspend fun deleteTask(uid: Long)

    // Restore a deleted task
    @Query("UPDATE TodoModel SET isDeleted = 0 WHERE id = :uid")
    suspend fun restoreDeletedTask(uid: Long)

    // Update a task object (general purpose update)
    @Update
    suspend fun updateTask(todoModel: TodoModel)

    // Update specific fields of a task
    @Query("UPDATE TodoModel SET title = :title, description = :description, priority = :priority, date = :date, time = :time WHERE id = :taskId")
    suspend fun updateTask(
        taskId: Long,
        title: String,
        description: String,
        priority: String,
        date: Long,
        time: Long
    )

    // Update task completion state
    @Query("UPDATE TodoModel SET completed = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean)

    // Get all completed tasks for a user (optional for displaying completed tasks)
    @Query("SELECT * FROM TodoModel WHERE userId = :userId AND isFinished = 1 AND isDeleted = 0")
    suspend fun getCompletedTasks(userId: Int): List<TodoModel>

    // Get tasks sorted by priority and due date
    @Query("""
        SELECT * FROM TodoModel 
        WHERE isDeleted = 0 
        ORDER BY 
            CASE 
                WHEN priority = 'High' THEN 1 
                WHEN priority = 'Medium' THEN 2 
                WHEN priority = 'Low' THEN 3 
            END,
            date ASC
    """)
    suspend fun getTasksSortedByPriorityAndDate(): List<TodoModel>
}
