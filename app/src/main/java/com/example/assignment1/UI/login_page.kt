package com.example.assignment1.UI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.R
import org.json.JSONObject
import java.io.IOException

class login_page : AppCompatActivity() {

    // 🔑 MUST MATCH keys used in home_page.kt
    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"
    // Assuming BaseUrlUtil is available as used in home_page.kt
    private lateinit var baseUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ------------------------------
        // 1. CHECK IF USER IS ALREADY LOGGED IN
        // ------------------------------
        val prefs = getSharedPreferences("SocialAppPrefs", Context.MODE_PRIVATE)
        val savedUserId = prefs.getInt("current_user_id", -1)

        if (savedUserId != -1) {
            // User already logged in → go directly to home_page
            startActivity(Intent(this, home_page::class.java))
            finish()
            return
        }

        // ------------------------------
        // 2. LOAD UI
        // ------------------------------
        setContentView(R.layout.activity_login_page)

        // ------------------------------
        // 3. LOAD BASE URL
        // ------------------------------
        try {
            baseUrl = getBaseUrl(this).trim()

            // Remove trailing slash if exists
//            if (baseUrl.endsWith("/")) {
//                baseUrl = baseUrl.substring(0, baseUrl.length - 1)
//            }

        } catch (e: IOException) {
            Log.e("LOGIN_INIT", "Error loading base URL: ${e.message}")
            Toast.makeText(this, "Base URL missing or invalid.", Toast.LENGTH_LONG).show()
            baseUrl = ""
        }

        // ------------------------------
        // 4. INITIALIZE UI ELEMENTS
        // ------------------------------
        val emailField = findViewById<EditText>(R.id.username)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginButton = findViewById<Button>(R.id.login_button)
        val signUpButton = findViewById<Button>(R.id.sign_up)

        // ------------------------------
        // 5. SIGN UP BUTTON
        // ------------------------------
        signUpButton.setOnClickListener {
            startActivity(Intent(this, sign_up::class.java))
        }

        // ------------------------------
        // 6. LOGIN BUTTON
        // ------------------------------
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }
    }


    private fun loginUser(userEmail: String, userPass: String) {

        // Use the initialized baseUrl
        val url = baseUrl + "login.php"

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Log.d("API_RESPONSE", response)
                try {
                    val json = JSONObject(response)

                    if (json.getString("status") == "success") {

                        // 1. Get the User ID from the response
                        val userId = json.getInt("user_id")

                        // 2. Save the User ID to SharedPreferences
                        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putInt(KEY_USER_ID, userId)
                            // IMPORTANT: .apply() writes the data asynchronously
                            .apply()

                        Toast.makeText(this, "Login success (ID saved: $userId)", Toast.LENGTH_SHORT).show()

                        // 3. Navigate to home_page
                        startActivity(Intent(this, home_page::class.java))
                        finish() // Prevent going back to login screen

                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("JSON_ERROR", "Error processing response: ${e.message}")
                    Toast.makeText(this, "Server response error, please check server logs.", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("API_ERROR", error.toString())
                Toast.makeText(this, "Network error: Check your connection and URL.", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> =
                hashMapOf(
                    "email" to userEmail,
                    "password" to userPass
                )
        }

        Volley.newRequestQueue(this).add(request)
    }

    // Moved URL fetching logic into a utility function or kept it local based on your structure
    fun getBaseUrl(context: Context): String {
        // You can keep your existing logic here, or use BaseUrlUtil as in home_page.kt
        val input = context.resources.openRawResource(R.raw.base_url)
        return input.bufferedReader().use { it.readText().trim() }
    }
}