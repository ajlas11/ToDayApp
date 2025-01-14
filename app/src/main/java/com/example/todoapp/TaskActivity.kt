package com.example.todoapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.todoapp.databinding.ActivityTaskBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TaskActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityTaskBinding
    private lateinit var myCalendar: Calendar
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var timeSetListener: TimePickerDialog.OnTimeSetListener


    private var finalDate = 0L
    private var finalTime = 0L

    private val priorities = listOf("Low", "Medium", "High") // Priority options

    private var userId: Int = -1
    private var taskId: Long = -1L

    private val db by lazy {
        AppDatabase.getDatabase(this)
    }

    private val startActivityForResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                setResult(RESULT_OK)
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        taskId = intent.getLongExtra("TASK_ID", -1L)

        if (userId == -1) {
            Snackbar.make(binding.root, "Error: User ID not found", Snackbar.LENGTH_SHORT).show()
            finish()
        }

        if (taskId != -1L) {
            populateTaskData()
        }

        binding.dateEdt.setOnClickListener(this)
        binding.timeEdt.setOnClickListener(this)
        binding.saveBtn.setOnClickListener(this)

        setUpPrioritySpinner()
    }

    private fun setUpPrioritySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)
        binding.spinnerPriority.adapter = adapter
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.dateEdt -> setDateListener()
            R.id.timeEdt -> setTimeListener()
            R.id.saveBtn -> {
                if (taskId != -1L) {
                    updateTask()
                } else {
                    saveTodo()
                }
            }
        }
    }

    private fun populateTaskData() {
        val title = intent.getStringExtra("TITLE") ?: ""
        val description = intent.getStringExtra("DESCRIPTION") ?: ""
        val priority = intent.getStringExtra("PRIORITY") ?: "Low"
        val date = intent.getLongExtra("DATE", 0L)
        val time = intent.getLongExtra("TIME", 0L)

        binding.titleInpLay.editText?.setText(title)
        binding.taskInpLay.editText?.setText(description)

        val priorityIndex = priorities.indexOf(priority)
        if (priorityIndex != -1) {
            binding.spinnerPriority.setSelection(priorityIndex)
        }

        binding.dateEdt.setText(if (date == 0L) "" else formatDate(date))
        binding.timeEdt.setText(if (time == 0L) "" else formatTime(time))

        finalDate = date
        finalTime = time

        binding.saveBtn.text = getString(R.string.update_task)
    }

    private fun formatDate(date: Long): String {
        val myFormat = "EEE, d MMM yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        return sdf.format(Date(date))
    }

    private fun formatTime(time: Long): String {
        val myFormat = "h:mm a"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        return sdf.format(Date(time))
    }

    private fun saveTodo() {
        val priority = binding.spinnerPriority.selectedItem.toString()
        val title = binding.titleInpLay.editText?.text.toString()
        val description = binding.taskInpLay.editText?.text.toString()

        if (title.isEmpty() || description.isEmpty()) {
            Snackbar.make(binding.root, "Title and Description cannot be empty", Snackbar.LENGTH_SHORT).show()
            return
        }

        val reminderDate = if (binding.dateEdt.text.isNullOrEmpty()) 0L else finalDate
        val reminderTime = if (binding.timeEdt.text.isNullOrEmpty()) 0L else finalTime

        lifecycleScope.launch(Dispatchers.IO) {
            // Insert the task into the database
            db.todoDao().insertTask(
                TodoModel(
                    title = title,
                    description = description,
                    priority = priority,
                    date = reminderDate,
                    time = reminderTime,
                    isFinished = 0,
                    userId = userId,
                )
            )

            withContext(Dispatchers.Main) {
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun updateTask() {
        val priority = binding.spinnerPriority.selectedItem.toString()
        val title = binding.titleInpLay.editText?.text.toString()
        val description = binding.taskInpLay.editText?.text.toString()

        if (title.isEmpty() || description.isEmpty()) {
            Snackbar.make(binding.root, "Title and Description cannot be empty",Snackbar.LENGTH_SHORT).show()
            return
        }

        val reminderDate = if (binding.dateEdt.text.isNullOrEmpty()) 0L else finalDate
        val reminderTime = if (binding.timeEdt.text.isNullOrEmpty()) 0L else finalTime

        lifecycleScope.launch(Dispatchers.IO) {
            // Update the task in the database
            db.todoDao().updateTask(
                TodoModel(
                    id = taskId,
                    title = title,
                    description = description,
                    priority = priority,
                    date = reminderDate,
                    time = reminderTime,
                    isFinished = 0,
                    userId = userId,
                )
            )


            withContext(Dispatchers.Main) {
                setResult(RESULT_OK)
                finish()
            }
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

    private fun setTimeListener() {
        myCalendar = Calendar.getInstance()

        timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            updateTime()
        }

        TimePickerDialog(
            this,
            timeSetListener,
            myCalendar.get(Calendar.HOUR_OF_DAY),
            myCalendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateTime() {
        val myFormat = "h:mm a"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        finalTime = myCalendar.time.time
        binding.timeEdt.setText(sdf.format(myCalendar.time))
    }

    private fun setDateListener() {
        myCalendar = Calendar.getInstance()

        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDate()
        }

        DatePickerDialog(
            this,
            dateSetListener,
            myCalendar.get(Calendar.YEAR),
            myCalendar.get(Calendar.MONTH),
            myCalendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun updateDate() {
        val myFormat = "EEE, d MMM yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        finalDate = myCalendar.time.time
        binding.dateEdt.setText(sdf.format(myCalendar.time))

        binding.timeInptLay.visibility = View.VISIBLE
    }
}
