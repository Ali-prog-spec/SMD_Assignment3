package com.example.assignment1.UI

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment1.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // 🔹 Already logged in
                startActivity(Intent(this, home_page::class.java))
            } else {
                // 🔹 Not logged in
                startActivity(Intent(this, login_page::class.java))
            }
            finish()
        }, 2000)
    }
}
