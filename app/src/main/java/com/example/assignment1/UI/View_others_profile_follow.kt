package com.example.assignment1.UI

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.R
import com.example.assignment1.Utils.BaseUrlUtil
import com.bumptech.glide.Glide // 🛑 IMPORTANT: GLIDE IMPORT
import org.json.JSONObject
import java.io.IOException
import java.util.HashMap

class View_others_profile_follow : AppCompatActivity() {

    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"
    private lateinit var baseUrl: String

    private var currentUserId: String? = null // Current user ID (MySQL ID from prefs)
    private lateinit var profileUserId: String // ID of the user being viewed

    private lateinit var usernameView: TextView
    private lateinit var usernameView2: TextView

    private lateinit var profileImage: ImageView
    private lateinit var followButton: TextView
    private lateinit var followerCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var postCount: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_others_profile_follow)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // 1. Initialize API components and IDs
        try {
            baseUrl = BaseUrlUtil.getBaseUrl(this)
        } catch (e: IOException) {
            Toast.makeText(this, "Base URL Initialization Error.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        loadUserId()

        profileUserId = intent.getStringExtra("id") ?: return
        Toast.makeText(this, "loaded usser id  ${currentUserId}", Toast.LENGTH_SHORT).show()

        if (currentUserId.isNullOrEmpty() || currentUserId == "0") {
            Toast.makeText(this, "Authentication Error: Please log in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Toast.makeText(this, "about to open teh profile page of ${currentUserId}", Toast.LENGTH_SHORT).show()
        // 2. Bind UI elements
        usernameView = findViewById(R.id.username)
        usernameView2 = findViewById(R.id.username2)
        profileImage = findViewById(R.id.profile_image)
        followButton = findViewById(R.id.follow)
        val messageButton = findViewById<TextView>(R.id.message)
        val backButton = findViewById<ImageView>(R.id.back_button)
        followerCount = findViewById(R.id.followersCount)
        followingCount = findViewById(R.id.followingCount)
        postCount = findViewById(R.id.postCount)
        Log.d("PROFILE_API", "About to load Profile ") // Added Log

        // 3. Load profile data (includes counts and follow status)
        loadUserProfile()
        Log.d("PROFILE_API", "loaded profile  ") // Added Log

        // 4. Follow / Request logic
        followButton.setOnClickListener {
            handleFollowAction()
        }

        // 5. Open chat
        messageButton.setOnClickListener {
            val intent = Intent(this, chat_paage::class.java)
            intent.putExtra("receiverId", profileUserId)
            // You may need to pass receiverName and receiverDpUrl as well if chat_paage uses them
            startActivity(intent)
        }

        backButton.setOnClickListener { finish() }
    }

    private fun loadUserId() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentUserId = prefs.getInt(KEY_USER_ID, 0).toString()
    }

    // --- Data Loading ---

    private fun loadUserProfile() {
        val url = "${baseUrl}get_profile.php?current_user_id=${currentUserId}&profile_user_id=${profileUserId}"
        Log.d("PROFILE_API", "Responsed URL: $url") // Added Log

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                Log.d("PROFILE_API", "Response: $response") // Added Log

                if (jsonResponse.getBoolean("success")) {
                    val userData = jsonResponse.getJSONObject("user_data")

                    // 🛑 FIX: Load Profile Picture using Glide (with requested path logic)
                    val dpPathFullUrl = userData.getString("dp_path")

                    if (dpPathFullUrl.isNotEmpty()) {
                        val uploadsIndex = dpPathFullUrl.indexOf("uploads/")

                        if (uploadsIndex != -1) {
                            // Extract the relative path, e.g., "uploads/user_6.jpg"
                            val relativePath = dpPathFullUrl.substring(uploadsIndex)
                            // Concatenate with baseUrl to get the final path
                            val finalImageUrl = baseUrl + relativePath

                            Log.d("PROFILE_API", "Image URL: $finalImageUrl")

                            Glide.with(this)
                                .load(finalImageUrl)
                                .placeholder(R.drawable.ic_launcher_background) // Replace with a real placeholder
                                .error(R.drawable.ic_launcher_background)        // Replace with a real error image
                                .into(profileImage)
                        } else {
                            Log.e("PROFILE_API", "Could not find 'uploads/' in path: $dpPathFullUrl")
                        }
                    }

                    // ✅ Update Static Data
                    usernameView.text = userData.getString("username")
                    usernameView2.text = userData.getString("username")

                    followerCount.text = userData.getString("follower_count")
                    followingCount.text = userData.getString("following_count")
                    postCount.text = userData.getString("post_count")

                    // Update Follow Button Status
                    updateFollowButton(jsonResponse.getString("follow_status"))

                } else {
                    Toast.makeText(this, "Error: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                    Log.e("PROFILE_API", "Server success=false: ${jsonResponse.getString("message")}")
                }
            } catch (e: Exception) {
                // This catch block will now catch JSON parsing errors, not Base64 crashes
                Log.e("PROFILE_API", "Parsing error: ${e.message}. Response: $response")
                Toast.makeText(this, "Failed to parse profile data.", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Log.e("PROFILE_API", "Network error: ${error.message}")
            Toast.makeText(this, "Network error fetching profile.", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun updateFollowButton(status: String) {
        when (status) {
            "following" -> followButton.text = "Following ▽"
            "pending" -> followButton.text = "Requested ✓"
            else -> followButton.text = "Follow"
        }
    }

    // --- Follow Logic (Unchanged from previous solution) ---

    private fun handleFollowAction() {
        when (followButton.text.toString()) {
            "Follow" -> handleFollowApi("follow")
            "Requested ✓" -> handleFollowApi("cancel_request")
            "Following ▽" -> handleFollowApi("unfollow")
        }
    }

    private fun handleFollowApi(action: String) {
        val url = baseUrl + "handle_follow.php"

        val stringRequest = object : StringRequest(Request.Method.POST, url, { response ->
            try {
                val jsonResponse = JSONObject(response)

                if (jsonResponse.getBoolean("success")) {
                    // Refresh UI to update counts and button status
                    loadUserProfile()
                    Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Action failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FOLLOW_API", "Response error: ${e.message}")
                Toast.makeText(this, "API response error.", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Log.e("FOLLOW_API", "Network error: ${error.message}")
            Toast.makeText(this, "Network error during follow action.", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["action"] = action
                params["sender_id"] = currentUserId!!
                params["receiver_id"] = profileUserId
                return params
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }

    // --- Utility ---

    /** REMOVED Base64 function as we now use Glide for URLs */
}