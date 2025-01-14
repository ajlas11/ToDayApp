package com.example.todoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import com.example.todoapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.DatePickerDialog
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val list = arrayListOf<TodoModel>()
    private val filteredList = arrayListOf<TodoModel>() // For filtered search results
    private val selectedTasks = mutableListOf<TodoModel>() // List to track selected tasks for deletion
    private var isDeleteMode = false // Flag to toggle delete mode
    private val taskListFlow = MutableStateFlow<List<TodoModel>>(emptyList())

    private val db by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    private var userId: Int = -1

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

        setupSortingOptions()
        sortTasksByPriorityAndDate()
        sortTasksByPriority()

        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupSwipeToComplete()
        setupBottomNavigation()

        getTasksForUser(userId)

        setupSearchBar()

        lifecycleScope.launch {
            taskListFlow.collect { tasks ->
                updateTaskList(tasks)
            }
        }

        loadTasks()

        startActivityForResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                refreshTasks()
            }
        }


        binding.addTaskButton.setOnClickListener {
            openNewTask()
        }

        binding.deleteButton.setOnClickListener {
            deleteSelectedTasks()
        }
    }
    override fun onRestart() {
        super.onRestart()
        Log.d("MainActivityLifecycle", "onRestart called: Activity is restarting.")
    }
    override fun onStart() {
        super.onStart()
        Log.d("MainActivityLifecycle", "onStart called: Restoring app state.")

        val sharedPreferences = getSharedPreferences("ToDoAppPrefs", MODE_PRIVATE)
        val lastSearchQuery = sharedPreferences.getString("LastSearchQuery", null)

        if (!lastSearchQuery.isNullOrEmpty()) {
            binding.searchBar.setQuery(lastSearchQuery, false)
            filterTasks(lastSearchQuery)
            Log.d("MainActivityLifecycle", "Restored search query: $lastSearchQuery")
        }
    }


    override fun onPause() {
        super.onPause()
        Log.d("MainActivityLifecycle", "onPause called: Activity is partially visible.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivityLifecycle", "onDestroy called: Activity is being destroyed.")
    }


    override fun onStop() {
        super.onStop()
        Log.d("MainActivityLifecycle", "onStop called: Saving app state.")

        val sharedPreferences = getSharedPreferences("ToDoAppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentSearchQuery = binding.searchBar.query.toString()
        editor.putString("LastSearchQuery", currentSearchQuery)
        editor.apply()
    }

    private fun loadTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val tasks = db.todoDao().getTasksSortedByPriorityAndDate(userId) // Pass userId
            taskListFlow.value = tasks // Update StateFlow
        }
    }

    private fun updateTaskList(tasks: List<TodoModel>) {
        filteredList.clear()
        filteredList.addAll(tasks)
        binding.todoRv.adapter?.notifyDataSetChanged()
    }

    private fun setupSwipeToComplete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val task = filteredList[position]

                lifecycleScope.launch(Dispatchers.IO) {
                    db.todoDao().updateTaskCompletion(task.id.toInt(), true) // Mark as completed
                    withContext(Dispatchers.Main) {
                        filteredList.removeAt(position)
                        binding.todoRv.adapter?.notifyItemRemoved(position)
                        Snackbar.make(binding.root, "Task '${task.title}' marked as completed!", Snackbar.LENGTH_SHORT).show()
                    }
                }
                fetchJoke()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.todoRv)
    }






    private fun fetchJoke() {
        RetrofitInstance.api.getProgrammingJoke().enqueue(object : Callback<JokeResponse> {
            override fun onResponse(call: Call<JokeResponse>, response: Response<JokeResponse>) {
                if (response.isSuccessful) {
                    val jokeResponse = response.body()
                    val joke = if (jokeResponse?.type == "single") {
                        jokeResponse.joke
                    } else {
                        "${jokeResponse?.setup}\n\n${jokeResponse?.delivery}"
                    }

                    // Display the joke in a Material Alert Dialog
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Programming Joke")
                        .setMessage(joke ?: "No joke found")
                        .setPositiveButton("Close") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    Snackbar.make(binding.root, "Failed to fetch joke", Snackbar.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JokeResponse>, t: Throwable) {
                Snackbar.make(binding.root, "Error: ${t.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }



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
                        db.todoDao().updateTaskCompletion(task.id.toInt(), isCompleted)
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
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val tasks = db.todoDao().getActiveTasks(userId)
            withContext(Dispatchers.Main) {
                filteredList.clear()
                filteredList.addAll(tasks)
                binding.todoRv.adapter?.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
            }
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("MainActivityLifecycle", "onResume called: Task list refreshed.")
        getTasksForUser(userId) // Refresh tasks on activity resume
    }


    private fun refreshTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val taskList = db.todoDao().getIncompleteTasksForUser(userId)
            withContext(Dispatchers.Main) {
                list.clear()
                list.addAll(taskList)
                filteredList.clear()
                filteredList.addAll(taskList)
                binding.todoRv.adapter?.notifyDataSetChanged()
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
                R.id.nav_joke -> {
                    fetchJoke() // Fetch and display a joke
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
                Snackbar.make(binding.root, "Selected Date: $formattedDate", Snackbar.LENGTH_SHORT).show()
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
                R.id.nav_completed -> { // New option for Completed Tasks
                    val intent = Intent(this, CompletedTasksActivity::class.java)
                    intent.putExtra("USER_ID", userId) // Pass the user ID
                    startActivity(intent)
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
        binding.todoRv.adapter?.notifyDataSetChanged()

        binding.deleteButton.setOnClickListener {
            deleteSelectedTasks()
        }
    }


    private fun deleteSelectedTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            selectedTasks.forEach { task ->
                db.todoDao().markTaskAsDeleted(task.id) // Persist task deletion in the database
            }

            withContext(Dispatchers.Main) {
                list.removeAll(selectedTasks)
                filteredList.removeAll(selectedTasks)
                selectedTasks.clear()
                binding.todoRv.adapter?.notifyDataSetChanged()
                Snackbar.make(binding.root, "Selected tasks deleted successfully!", Snackbar.LENGTH_SHORT).show()
                toggleDeleteMode() // Exit delete mode
            }
        }
    }



    private fun setupSortingOptions() {
        val sortingOptions = resources.getStringArray(R.array.sorting_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortingOptions)
        binding.sortingOptions.adapter = adapter

        binding.sortingOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (sortingOptions[position]) {
                    "Priority" -> sortTasksByPriorityAndDate() // Call the function here
                    "Due Date" -> sortTasksByDueDate() // Another sorting function
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun sortTasksByPriority() {
        filteredList.sortWith(compareBy(
            { when (it.priority) {
                "High" -> 1
                "Medium" -> 2
                "Low" -> 3
                else -> 4
            }},
            { it.date }
        ))
        binding.todoRv.adapter?.notifyDataSetChanged()
    }

    private fun sortTasksByDueDate() {
        filteredList.sortWith(compareBy({ it.date }, { it.time }))
        binding.todoRv.adapter?.notifyDataSetChanged()
    }

    private fun sortTasksByPriorityAndDate() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sortedTasks = db.todoDao().getTasksSortedByPriorityAndDate(userId)
            withContext(Dispatchers.Main) {
                filteredList.clear()
                filteredList.addAll(sortedTasks)
                binding.todoRv.adapter?.notifyDataSetChanged()
            }
        }
    }
    private fun onLoginSuccess(userId: Int) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
        finish()
    }






}