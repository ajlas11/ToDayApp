package com.example.todoapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todoapp.databinding.ItemDeletedTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class DeletedTaskAdapter(
    private val tasks: List<TodoModel>,
    private val onRestoreClick: (TodoModel) -> Unit,
    private val onPermanentlyDeleteClick: (TodoModel) -> Unit
) : RecyclerView.Adapter<DeletedTaskAdapter.DeletedTaskViewHolder>() {

    class DeletedTaskViewHolder(
        private val binding: ItemDeletedTaskBinding,
        private val onRestoreClick: (TodoModel) -> Unit,
        private val onPermanentlyDeleteClick: (TodoModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TodoModel) {
            binding.txtDeletedTitle.text = task.title
            binding.txtDeletedDescription.text = task.description
            binding.txtDeletedDateTime.text = "Deleted on: ${formatDate(task.date)}"

            // Restore button
            binding.btnRestoreTask.setOnClickListener {
                onRestoreClick(task)
            }

            // Permanently delete button
            binding.btnPermanentlyDeleteTask.setOnClickListener {
                onPermanentlyDeleteClick(task)
            }
        }

        private fun formatDate(date: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(Date(date))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletedTaskViewHolder {
        val binding = ItemDeletedTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeletedTaskViewHolder(binding, onRestoreClick, onPermanentlyDeleteClick)
    }

    override fun onBindViewHolder(holder: DeletedTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size
}
