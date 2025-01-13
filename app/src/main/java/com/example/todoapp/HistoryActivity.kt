package com.example.todoapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todoapp.databinding.ActivityHistoryBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val deletedTasksList = arrayListOf<TodoModel>() // List of deleted tasks
    private lateinit var deletedTaskAdapter: DeletedTaskAdapter
    private var userId: Int = -1 // Default to invalid user ID

    private val db by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve userId from the intent
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up the toolbar
        setupToolbar()

        // Set up the RecyclerView
        setupRecyclerView()

        // Fetch deleted tasks
        fetchDeletedTasks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.historyToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.historyToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        deletedTaskAdapter = DeletedTaskAdapter(
            tasks = deletedTasksList,
            onRestoreClick = { task -> restoreTask(task) },
            onPermanentlyDeleteClick = { task -> permanentlyDeleteTask(task) }
        )

        binding.recyclerViewDeletedTasks.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = deletedTaskAdapter
        }
    }

    private fun fetchDeletedTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch deleted tasks for the user
                val tasks = db.todoDao().getDeletedTasks(userId)
                Log.d("FetchDeletedTasks", "Fetched deleted tasks: $tasks")
                withContext(Dispatchers.Main) {
                    deletedTasksList.clear()
                    deletedTasksList.addAll(tasks)
                    deletedTaskAdapter.notifyDataSetChanged()
                    Log.d("FetchDeletedTasks", "Deleted tasks added to adapter: ${deletedTasksList.size}")
                }
            } catch (e: Exception) {
                Log.e("FetchDeletedTasks", "Error fetching deleted tasks: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Failed to fetch deleted tasks", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun restoreTask(task: TodoModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Restore the deleted task
                db.todoDao().restoreDeletedTask(task.id)
                Log.d("RestoreTask", "Task restored: ${task.id}")

                deletedTasksList.remove(task)
                withContext(Dispatchers.Main) {
                    deletedTaskAdapter.notifyDataSetChanged()
                    Toast.makeText(this@HistoryActivity, "Task restored", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("RestoreTask", "Error restoring task: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Failed to restore task", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun permanentlyDeleteTask(task: TodoModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Permanently delete the task from the database
                db.todoDao().deleteTask(task.id)
                Log.d("PermanentDelete", "Task permanently deleted: ${task.id}")

                deletedTasksList.remove(task)
                withContext(Dispatchers.Main) {
                    deletedTaskAdapter.notifyDataSetChanged()
                    Toast.makeText(this@HistoryActivity, "Task permanently deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PermanentDelete", "Error deleting task permanently: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Failed to delete task", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }
}
