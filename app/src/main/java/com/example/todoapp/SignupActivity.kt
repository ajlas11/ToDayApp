package com.example.todoapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.todoapp.databinding.ActivitySignupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signupButton.setOnClickListener {
            val email = binding.username.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            if (validateInput(email, password, confirmPassword)) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val existingUser = db.userDao().getUserByEmail(email)

                    if (existingUser != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SignupActivity, "Email is already registered", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val newUser = User(username = email, email = email, password = password)
                        db.userDao().insertUser(newUser)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SignupActivity, "Signup successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 8 || !PASSWORD_PATTERN.matcher(password).matches()) {
            Toast.makeText(
                this,
                "Password must be at least 8 characters long, contain a number, and an uppercase letter",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    companion object {
        private val PASSWORD_PATTERN: Pattern =
            Pattern.compile(
                "^(?=.*[A-Z])(?=.*[0-9]).{8,}$" // At least 8 characters, one uppercase letter, and one digit
            )
    }
}