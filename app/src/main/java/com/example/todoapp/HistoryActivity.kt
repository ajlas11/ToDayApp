package com.example.todoapp

import android.os.Bundle
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
    private val deletedTasksList = arrayListOf<TodoModel>()
    private val selectedTasks = mutableListOf<TodoModel>() // Track selected tasks for deletion
    private var isDeleteMode = false // Flag to toggle deletion mode

    private val db by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.historyToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.historyToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Set up the RecyclerView
        setupRecyclerView()

        // Fetch deleted tasks from the database
        fetchDeletedTasks()

        // Toggle delete mode
        binding.deleteButton.setOnClickListener {
            toggleDeleteMode()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewDeletedTasks.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = TodoAdapter(
                deletedTasksList,
                onTaskClick = { /* No action needed */ },
                onEditClick = { /* No action needed */ },
                onTaskCompleted = { task, isCompleted -> /* No action needed */ },
                onDeleteClick = { task ->
                    deleteTask(task)
                },
                isDeleteMode = isDeleteMode,
                selectedTasks = selectedTasks
            )
        }
    }

    private fun fetchDeletedTasks() {
        val userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            // Handle invalid userId (if necessary)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch deleted tasks from the database directly
                val tasks = db.todoDao().getDeletedTasks(userId)

                // Update UI on the main thread
                withContext(Dispatchers.Main) {
                    deletedTasksList.clear()
                    deletedTasksList.addAll(tasks)
                    binding.recyclerViewDeletedTasks.adapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                // Handle any potential errors
                e.printStackTrace()
            }
        }
    }

    private fun toggleDeleteMode() {
        isDeleteMode = !isDeleteMode // Toggle delete mode
        binding.recyclerViewDeletedTasks.adapter?.notifyDataSetChanged() // Refresh the adapter to show/hide checkboxes
    }

    private fun deleteTask(task: TodoModel) {
        // Perform deletion in the database
        lifecycleScope.launch(Dispatchers.IO) {
            db.todoDao().deleteTask(task.id)
            // After deletion, update the UI by removing the task from the list
            deletedTasksList.remove(task)
            withContext(Dispatchers.Main) {
                binding.recyclerViewDeletedTasks.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun deleteSelectedTasks() {
        // Perform batch deletion for selected tasks
        lifecycleScope.launch(Dispatchers.IO) {
            selectedTasks.forEach { task ->
                db.todoDao().deleteTask(task.id)
            }
            // After deletion, update the UI by removing the tasks from the list
            deletedTasksList.removeAll(selectedTasks)
            selectedTasks.clear() // Clear selected tasks list
            withContext(Dispatchers.Main) {
                binding.recyclerViewDeletedTasks.adapter?.notifyDataSetChanged()
                Toast.makeText(this@HistoryActivity, "Tasks deleted successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }
}