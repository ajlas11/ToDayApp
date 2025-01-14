package com.example.todoapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todoapp.databinding.ActivityCompletedTasksBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope


class CompletedTasksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompletedTasksBinding
    private val completedTasksList = mutableListOf<TodoModel>()
    private val todoAdapter by lazy {
        TodoAdapter(
            list = completedTasksList,
            onTaskClick = { /* No action needed for completed tasks */ },
            onEditClick = { /* Editing might not be needed for completed tasks */ },
            onTaskCompleted = { _, _ -> /* Task already completed */ },
            onDeleteClick = { deleteTask(it) },
            isDeleteMode = false,
            selectedTasks = mutableListOf()
        )
    }

    private val db by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompletedTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            finish() // Invalid user ID
            return
        }

        setupToolbar()
        setupRecyclerView()
        fetchCompletedTasks()
    }

    private fun setupToolbar() {
        // Set up the toolbar with a back button
        binding.toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        binding.toolbar.setNavigationOnClickListener {
            finish() // Close the activity and go back
        }
    }

    private fun setupRecyclerView() {
        binding.completedTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CompletedTasksActivity)
            adapter = todoAdapter
        }
    }

    private fun fetchCompletedTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val tasks = db.todoDao().getCompletedTasks(userId)
            withContext(Dispatchers.Main) {
                Log.d("CompletedTasksActivity", "Fetched completed tasks: ${tasks.size}")
                completedTasksList.clear()
                completedTasksList.addAll(tasks)
                todoAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun deleteTask(task: TodoModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.todoDao().deleteTask(task.id)
            withContext(Dispatchers.Main) {
                completedTasksList.remove(task)
                todoAdapter.notifyDataSetChanged()
            }
        }
    }
}
