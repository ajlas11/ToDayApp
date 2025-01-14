package com.example.todoapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Query("SELECT * FROM Reminder")
    fun getAllReminders(): LiveData<List<Reminder>>

    @Query("SELECT * FROM Reminder WHERE id = :reminderId")
    fun getReminderById(reminderId: Int): LiveData<Reminder?>

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}
