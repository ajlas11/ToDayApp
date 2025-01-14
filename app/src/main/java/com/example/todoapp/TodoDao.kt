package com.example.todoapp

import android.content.IntentSender.OnFinished
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TodoDao {

    @Insert
    suspend fun insertTask(todoModel: TodoModel): Long

    @Query("SELECT * FROM TodoModel WHERE isFinished = 0 AND isDeleted = 0")
    suspend fun getTask(): List<TodoModel>

    @Query("SELECT * FROM TodoModel WHERE userId = :userId AND isFinished = 0 AND isDeleted = 0")
    suspend fun getIncompleteTasksForUser(userId: Int): List<TodoModel>

    @Query("UPDATE TodoModel SET isFinished = 1 WHERE id = :uid")
    suspend fun finishTask(uid: Long)

    @Query("UPDATE TodoModel SET isDeleted = 1 WHERE id = :uid")
    suspend fun markTaskAsDeleted(uid: Long)

    @Query("SELECT * FROM TodoModel WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedTasks(userId: Int): List<TodoModel>

    @Query("DELETE FROM TodoModel WHERE id = :uid")
    suspend fun deleteTask(uid: Long)

    @Query("UPDATE TodoModel SET isDeleted = 0 WHERE id = :uid")
    suspend fun restoreDeletedTask(uid: Long)

    @Update
    suspend fun updateTask(todoModel: TodoModel)

    @Query("UPDATE TodoModel SET title = :title, description = :description, priority = :priority, date = :date, time = :time WHERE id = :taskId")
    suspend fun updateTask(
        taskId: Long,
        title: String,
        description: String,
        priority: String,
        date: Long,
        time: Long
    )

    @Query("UPDATE TodoModel SET isFinished = :isFinished WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Int, isFinished: Boolean)


    @Query("SELECT * FROM TodoModel WHERE userId = :userId AND isFinished = 1 AND isDeleted = 0")
    suspend fun getCompletedTasks(userId: Int): List<TodoModel>

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

    @Query("DELETE FROM TodoModel WHERE id = :uid")
    suspend fun deleteTaskPermanently(uid: Long)



}
