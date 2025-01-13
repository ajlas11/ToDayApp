package com.example.todoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import com.example.todoapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.DatePickerDialog
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val list = arrayListOf<TodoModel>()
    private val filteredList = arrayListOf<TodoModel>() // For filtered search results
    private val selectedTasks = mutableListOf<TodoModel>() // List to track selected tasks for deletion
    private var isDeleteMode = false // Flag to toggle delete mode

    private val db by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    private var userId: Int = -1

    // Define the ActivityResultLauncher to handle activity result from TaskActivity
    private lateinit var startActivityForResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupBottomNavigation()

        // Fetch all tasks for the user
        getTasksForUser(userId)

        setupSearchBar()

        // Initialize the ActivityResultLauncher for TaskActivity
        startActivityForResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Refresh tasks when TaskActivity has added or updated a task
                refreshTasks()
            }
        }

        // Handle "Add Task" button click
        binding.addTaskButton.setOnClickListener {
            openNewTask()
        }

        // Handle "Delete Selected Tasks" button click
        binding.deleteButton.setOnClickListener {
            deleteSelectedTasks()
        }
    }

    // Start TaskActivity for result using the new launcher
    private fun openNewTask() {
        val intent = Intent(this, TaskActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivityForResultLauncher.launch(intent)  // Launch TaskActivity with the result launcher
    }

    private fun setupRecyclerView() {
        binding.todoRv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = TodoAdapter(
                filteredList,
                selectedTasks = selectedTasks, // Pass selectedTasks to the adapter
                isDeleteMode = isDeleteMode, // Pass isDeleteMode to the adapter
                onTaskClick = { task ->
                    if (isDeleteMode) {
                        // Toggle task selection when in delete mode
                        if (selectedTasks.contains(task)) {
                            selectedTasks.remove(task)
                        } else {
                            selectedTasks.add(task)
                        }
                        binding.todoRv.adapter?.notifyDataSetChanged()
                    } else {
                        // Open edit task if not in delete mode
                        openEditTask(task)
                    }
                },
                onEditClick = { task -> openEditTask(task) },
                onTaskCompleted = { task, isCompleted ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.todoDao().updateTaskCompletion(task.id, isCompleted)
                    }
                },
                onDeleteClick = { task ->
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Delete Task")
                        .setMessage("Are you sure you want to delete this task?")
                        .setPositiveButton("Yes") { _, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                db.todoDao().deleteTask(task.id)
                            }
                            list.remove(task)
                            filteredList.remove(task)
                            binding.todoRv.adapter?.notifyItemRemoved(filteredList.indexOf(task))  // Use notifyItemRemoved instead of notifyDataSetChanged
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            )
        }
    }

    // Open TaskActivity in edit mode with selected task details
    private fun openEditTask(task: TodoModel) {
        val intent = Intent(this, TaskActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("TASK_ID", task.id)
            putExtra("TITLE", task.title)
            putExtra("DESCRIPTION", task.description)
            putExtra("PRIORITY", task.priority)
            putExtra("DATE", task.date)
            putExtra("TIME", task.time)
        }
        startActivityForResultLauncher.launch(intent)
    }

    private fun getTasksForUser(userId: Int) {
        binding.progressBar.visibility = View.VISIBLE // Show ProgressBar

        lifecycleScope.launch(Dispatchers.IO) {
            // Query the database in the background (on IO thread)
            val taskList = db.todoDao().getTasksForUser(userId)

            // Log the retrieved tasks
            Log.d("DatabaseCheck", "Fetched tasks for user: $taskList")

            // Switch to the main thread to update the UI
            withContext(Dispatchers.Main) {
                list.clear()
                list.addAll(taskList)
                filteredList.clear()
                filteredList.addAll(taskList)
                binding.todoRv.adapter?.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE // Hide ProgressBar once tasks are fetched
            }
        }
    }

    fun refreshTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val taskList = db.todoDao().getTasksForUser(userId)

            withContext(Dispatchers.Main) {
                list.clear()
                list.addAll(taskList)
                filteredList.clear()
                filteredList.addAll(taskList)
                binding.todoRv.adapter?.notifyDataSetChanged()  // Notify adapter to update RecyclerView
            }
        }
    }

    private fun setupSearchBar() {
        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTasks(newText)
                return true
            }
        })
    }

    private fun filterTasks(query: String?) {
        filteredList.clear()
        if (query.isNullOrEmpty()) {
            filteredList.addAll(list)
        } else {
            filteredList.addAll(
                list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
            )
        }
        binding.todoRv.adapter?.notifyDataSetChanged()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.searchBar.visibility = View.GONE
                    true
                }

                R.id.nav_calendar -> {
                    binding.searchBar.visibility = View.GONE
                    openCalendar()
                    true
                }

                R.id.nav_search -> {
                    toggleSearchBarVisibility()
                    true
                }

                R.id.nav_more -> {
                    binding.searchBar.visibility = View.GONE
                    showMoreMenu()
                    true
                }

                else -> false
            }
        }
    }

    private fun toggleSearchBarVisibility() {
        binding.searchBar.visibility =
            if (binding.searchBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun openCalendar() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                Toast.makeText(this, "Selected Date: $formattedDate", Toast.LENGTH_SHORT).show()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showMoreMenu() {
        val popupMenu = androidx.appcompat.widget.PopupMenu(this, binding.bottomNavigation)
        popupMenu.menuInflater.inflate(R.menu.more_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                    true
                }
                R.id.nav_logout -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes") { _, _ ->
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
                R.id.nav_delete -> {
                    toggleDeleteMode()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun toggleDeleteMode() {
        isDeleteMode = !isDeleteMode
        binding.deleteButton.visibility = if (isDeleteMode) View.VISIBLE else View.GONE
        binding.todoRv.adapter?.notifyDataSetChanged() // Refresh RecyclerView to show checkboxes

        binding.deleteButton.setOnClickListener {
            deleteSelectedTasks() // Call here
        }
    }


    private fun deleteSelectedTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            selectedTasks.forEach { task ->
                db.todoDao().markTaskAsDeleted(task.id) // Mark task as deleted in the database
            }

            withContext(Dispatchers.Main) {
                list.removeAll(selectedTasks)
                filteredList.removeAll(selectedTasks)
                selectedTasks.clear()
                binding.todoRv.adapter?.notifyDataSetChanged()
                Toast.makeText(this@MainActivity, "Tasks deleted successfully", Toast.LENGTH_SHORT).show()
                toggleDeleteMode() // Exit delete mode
            }
        }
    }

}