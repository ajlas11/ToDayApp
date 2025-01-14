package com.example.todoapp

import android.os.Bundle
import android.text.Html
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.todoapp.databinding.ActivityTriviaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TriviaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTriviaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTriviaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar with back navigation
        binding.toolbar.setNavigationOnClickListener {
            finish() // Finish the activity to navigate back
        }

        binding.fetchTriviaButton.setOnClickListener {
            fetchTrivia()
        }
    }

    private fun fetchTrivia() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = TriviaRetrofitInstance.api.getRandomTrivia() // Use the new instance
                val triviaQuestion = response.results.firstOrNull()

                withContext(Dispatchers.Main) {
                    triviaQuestion?.let {
                        // Decode HTML entities for better readability
                        val decodedQuestion = Html.fromHtml(it.question, Html.FROM_HTML_MODE_COMPACT).toString()
                        val decodedCategory = Html.fromHtml(it.category, Html.FROM_HTML_MODE_COMPACT).toString()

                        binding.triviaQuestion.text = "Category: $decodedCategory\n\nQ: $decodedQuestion"
                    } ?: run {
                        Toast.makeText(this@TriviaActivity, "No trivia found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TriviaActivity, "Failed to fetch trivia", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
