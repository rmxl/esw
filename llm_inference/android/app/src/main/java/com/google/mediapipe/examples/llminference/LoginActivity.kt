// Raghav old
/*
package com.google.mediapipe.examples.llminference

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameEditText: EditText = findViewById(R.id.login_username)
        val passwordEditText: EditText = findViewById(R.id.login_password)
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val loginButton: Button = findViewById(R.id.login_button)
        val signUpLink: TextView = findViewById(R.id.signup_link)
        val signUpUsernameEditText: EditText = findViewById(R.id.signup_username)
        val signUpPasswordEditText: EditText = findViewById(R.id.signup_password)
        val signUpButton: Button = findViewById(R.id.sign_up_button)

        // Initially show the login form, hide sign-up form
        showLoginForm()

        // Handle login button click
        loginButton.setOnClickListener {
            Log.i("LoginActivity", "Login Attempt")
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Retrieve stored credentials from SharedPreferences
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val storedUsername = sharedPreferences.getString("username", null)
            val storedPassword = sharedPreferences.getString("password", null)

            // Check if entered username and password match the stored credentials
            if (username == storedUsername && password == storedPassword) {
                val editor = sharedPreferences.edit()
                editor.putBoolean("isLoggedIn", true)
                editor.putString("current_username", username)  // Store current username
                editor.apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Show sign-up form when "Sign Up" link is clicked
        signUpLink.setOnClickListener {
            showSignUpForm()
        }

        // Handle sign-up button click
        signUpButton.setOnClickListener {
            val username = signUpUsernameEditText.text.toString()
            val password = signUpPasswordEditText.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Save user credentials to SharedPreferences
                val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("username", username)
                editor.putString("password", password)
                editor.putBoolean("isLoggedIn", true)
                editor.putString("current_username", username)  // Store current username
                editor.apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Sign-up failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Rest of the code remains unchanged...
    private fun showLoginForm() {
        val loginUsernameEditText: EditText = findViewById(R.id.login_username)
        val loginPasswordEditText: EditText = findViewById(R.id.login_password)
        val loginButton: Button = findViewById(R.id.login_button)
        val signUpLink: TextView = findViewById(R.id.signup_link)

        loginUsernameEditText.visibility = android.view.View.VISIBLE
        loginPasswordEditText.visibility = android.view.View.VISIBLE
        loginButton.visibility = android.view.View.VISIBLE
        signUpLink.visibility = android.view.View.VISIBLE

        val signUpUsernameEditText: EditText = findViewById(R.id.signup_username)
        val signUpPasswordEditText: EditText = findViewById(R.id.signup_password)
        val signUpButton: Button = findViewById(R.id.sign_up_button)

        signUpUsernameEditText.visibility = android.view.View.GONE
        signUpPasswordEditText.visibility = android.view.View.GONE
        signUpButton.visibility = android.view.View.GONE
    }

    private fun showSignUpForm() {
        val loginUsernameEditText: EditText = findViewById(R.id.login_username)
        val loginPasswordEditText: EditText = findViewById(R.id.login_password)
        val loginButton: Button = findViewById(R.id.login_button)
        val signUpLink: TextView = findViewById(R.id.signup_link)

        loginUsernameEditText.visibility = android.view.View.GONE
        loginPasswordEditText.visibility = android.view.View.GONE
        loginButton.visibility = android.view.View.GONE
        signUpLink.visibility = android.view.View.GONE

        val signUpUsernameEditText: EditText = findViewById(R.id.signup_username)
        val signUpPasswordEditText: EditText = findViewById(R.id.signup_password)
        val signUpButton: Button = findViewById(R.id.sign_up_button)

        signUpUsernameEditText.visibility = android.view.View.VISIBLE
        signUpPasswordEditText.visibility = android.view.View.VISIBLE
        signUpButton.visibility = android.view.View.VISIBLE
    }
}

*/

package com.google.mediapipe.examples.llminference

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class LoginActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Auto-login if already logged in
        if (sharedPreferences.getBoolean(IS_LOGGED_IN, false)) {
            navigateToMain()
        }

        val usernameEditText: EditText = findViewById(R.id.login_username)
        val passwordEditText: EditText = findViewById(R.id.login_password)
        val loginButton: Button = findViewById(R.id.login_button)
        val signUpLink: TextView = findViewById(R.id.signup_link)
        val signUpUsernameEditText: EditText = findViewById(R.id.signup_username)
        val signUpPasswordEditText: EditText = findViewById(R.id.signup_password)
        val signUpButton: Button = findViewById(R.id.sign_up_button)

        showLoginForm()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                showToast("Please enter both username and password")
                return@setOnClickListener
            }

            val storedUsername = sharedPreferences.getString(USERNAME_KEY, null)
            val storedPassword = sharedPreferences.getString(PASSWORD_KEY, null)

            if (username == storedUsername && password == storedPassword) {
                sharedPreferences.edit()
                    .putBoolean(IS_LOGGED_IN, true)
                    .putString(CURRENT_USERNAME, username)
                    .apply()
                navigateToMain()
            } else {
                showToast("Login failed")
            }
        }

        signUpLink.setOnClickListener {
            showSignUpForm()
        }

        signUpButton.setOnClickListener {
            val username = signUpUsernameEditText.text.toString()
            val password = signUpPasswordEditText.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                sharedPreferences.edit()
                    .putString(USERNAME_KEY, username)
                    .putString(PASSWORD_KEY, password)
                    .putBoolean(IS_LOGGED_IN, true)
                    .putString(CURRENT_USERNAME, username)
                    .apply()
                navigateToMain()
            } else {
                showToast("Sign-up failed")
            }
        }
    }

    private fun showLoginForm() {
        findViewById<EditText>(R.id.login_username).visibility = android.view.View.VISIBLE
        findViewById<EditText>(R.id.login_password).visibility = android.view.View.VISIBLE
        findViewById<Button>(R.id.login_button).visibility = android.view.View.VISIBLE
        findViewById<TextView>(R.id.signup_link).visibility = android.view.View.VISIBLE

        findViewById<EditText>(R.id.signup_username).visibility = android.view.View.GONE
        findViewById<EditText>(R.id.signup_password).visibility = android.view.View.GONE
        findViewById<Button>(R.id.sign_up_button).visibility = android.view.View.GONE
    }

    private fun showSignUpForm() {
        findViewById<EditText>(R.id.login_username).visibility = android.view.View.GONE
        findViewById<EditText>(R.id.login_password).visibility = android.view.View.GONE
        findViewById<Button>(R.id.login_button).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.signup_link).visibility = android.view.View.GONE

        findViewById<EditText>(R.id.signup_username).visibility = android.view.View.VISIBLE
        findViewById<EditText>(R.id.signup_password).visibility = android.view.View.VISIBLE
        findViewById<Button>(R.id.sign_up_button).visibility = android.view.View.VISIBLE
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
        const val IS_LOGGED_IN = "isLoggedIn"
        const val CURRENT_USERNAME = "current_username"
    }
}


