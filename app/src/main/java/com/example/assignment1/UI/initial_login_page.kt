package com.example.assignment1.UI

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.assignment1.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class initial_login_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial_login_page)

        // Handle insets (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Views
        val loginButton = findViewById<Button>(R.id.login_button)
        val switchAccount = findViewById<Button>(R.id.switch_account)
        val signUpButton = findViewById<Button>(R.id.sign_up)
        val usernameTextView = findViewById<TextView>(R.id.username)
        val dpImage = findViewById<CircleImageView>(R.id.dp)

        // Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // ✅ User is logged in
            val userId = currentUser.uid
            database.child(userId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("username").value.toString()
                    usernameTextView.text = username

                    val dp64 = snapshot.child("dp64").value?.toString() ?: ""

                    // ✅ Convert Base64 -> Bitmap and set to dpImage
                    if (dp64.isNotEmpty() && dp64 != "null") {
                        try {
                            val imageBytes = Base64.decode(dp64, Base64.DEFAULT)
                            val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            dpImage.setImageBitmap(decodedBitmap)
                            dpImage.setPadding(0, 0, 0, 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            dpImage.setImageResource(R.drawable.dp6)
                            Toast.makeText(this, "Error loading profile image", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // ✅ Default DP if no image found
                        dpImage.setImageResource(R.drawable.dp6)
                    }
                } else {
                    Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }

            Toast.makeText(this, "Logged in as: ${currentUser.email}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No user is currently logged in", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, login_page::class.java)
            startActivity(intent)
            finish()
        }

        // 🔹 Buttons
        signUpButton.setOnClickListener {
            val intent = Intent(this, sign_up::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val username = usernameTextView.text.toString() // get the text
            val intent = Intent(this, home_page::class.java)
            intent.putExtra("receiverName", username) // key = "receiverName", value = actual text
            startActivity(intent)

        }


        switchAccount.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, login_page::class.java)
            startActivity(intent)
            finish()
        }
    }
}
