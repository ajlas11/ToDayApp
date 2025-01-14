package com.example.todoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.todoapp.databinding.ItemTodoBinding
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val list: List<TodoModel>,
    private val onTaskClick: (TodoModel) -> Unit,
    private val onEditClick: (TodoModel) -> Unit,
    private val onTaskCompleted: (TodoModel, Boolean) -> Unit,
    private val onDeleteClick: (TodoModel) -> Unit,
    private val isDeleteMode: Boolean,
    private val selectedTasks: MutableList<TodoModel>
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    class TodoViewHolder(
        private val binding: ItemTodoBinding,
        private val onTaskClick: (TodoModel) -> Unit,
        private val onEditClick: (TodoModel) -> Unit,
        private val onTaskCompleted: (TodoModel, Boolean) -> Unit,
        private val onDeleteClick: (TodoModel) -> Unit,
        private val isDeleteMode: Boolean,
        private val selectedTasks: MutableList<TodoModel>
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(todoModel: TodoModel) {
            val context = binding.root.context

            val priorityColor = when (todoModel.priority) {
                "Low" -> R.color.priority_low
                "Medium" -> R.color.priority_medium
                "High" -> R.color.priority_high
                else -> R.color.litherGray // Default color
            }
            binding.priorityDot.setBackgroundColor(ContextCompat.getColor(context, priorityColor))

            binding.txtShowTitle.text = todoModel.title
            binding.txtShowTask.text = todoModel.description

            if (todoModel.date != 0L) {
                binding.txtShowDate.visibility = View.VISIBLE
                binding.txtShowDate.text = formatDate(todoModel.date)
            } else {
                binding.txtShowDate.visibility = View.GONE
            }

            if (todoModel.time != 0L) {
                binding.txtShowTime.visibility = View.VISIBLE
                binding.txtShowTime.text = formatTime(todoModel.time)
            } else {
                binding.txtShowTime.visibility = View.GONE
            }

            binding.btnDelete.visibility = if (isDeleteMode) View.GONE else View.VISIBLE
            binding.btnDelete.setOnClickListener {
                onDeleteClick(todoModel)
            }

            binding.root.setOnClickListener {
                onTaskClick(todoModel)
            }

            binding.btnEdit.setOnClickListener {
                onEditClick(todoModel)
            }
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding, onTaskClick, onEditClick, onTaskCompleted, onDeleteClick, isDeleteMode, selectedTasks)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size
}
