package com.example.assignment1.UI

import Comment
import StoryDatabaseHelper
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.Adapters.StoryAdapter
import com.example.assignment1.DataClass.Story
import com.example.assignment1.R
import com.example.assignment1.UI.adapters.PostAdapter
import com.example.assignment1.Utils.BaseUrlUtil
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.DataClass.Post
import com.example.assignment1.helper.PostDatabaseHelper
import org.json.JSONObject
import java.io.IOException

class home_page : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 200
    private lateinit var baseUrl: String // Initialized in onCreate
    private var currentUserId: Int = 0 // Loaded from Shared Preferences

    // Keys for Shared Preferences
    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"

    // ⚠️ Placeholder for services; ensure they exist or remove the references
    private val IncomingCallService = com.example.assignment1.UI.IncomingCallService::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // 1. Load User ID from Shared Preferences
        loadUserId()
        if (currentUserId == 0) {
            Toast.makeText(this, "Session expired, please log in.", Toast.LENGTH_LONG).show()
            // Redirect to login if the session is invalid
            startActivity(Intent(this, initial_login_page::class.java))
            finish()
            return
        }

        try {
            // 2. Initialize baseUrl - MUST BE DONE BEFORE ANY API CALLS
            baseUrl = BaseUrlUtil.getBaseUrl(this)
        } catch (e: IOException) {
            Toast.makeText(this, "Base URL configuration error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // --- Basic UI Setup ---
        findViewById<TextView>(R.id.name).setOnClickListener {
            // 1️⃣ Stop running services
            val serviceIntent = Intent(this, IncomingCallService)
            intent.putExtra("userId",currentUserId.toString())
            stopService(serviceIntent)

            // 2️⃣ Clear saved user session
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_USER_ID).apply() // or use .clear() if you want to remove all prefs

            // 3️⃣ Notify user
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // 4️⃣ Redirect to login page and finish current activity
            startActivity(Intent(this, login_page::class.java))
            finish()
        }


        findViewById<ImageView>(R.id.message).setOnClickListener {
            startActivity(Intent(this, message_page::class.java))
        }

        // --- Start Services ---
        val intent1 = Intent(this, IncomingCallService)
        intent1.putExtra("userId", currentUserId.toString())
        startService(intent1)

        // --- Feature Setup ---
        setActiveNavIcon("home")
        setupStories()
        setupPosts()
        setupNavBar()

        // Update status to online here, AFTER baseUrl is guaranteed to be set.
        setUserStatus("online")
    }

    /**
     * Loads the stored MySQL user ID (user_login_id) from Shared Preferences.
     */
    private fun loadUserId() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentUserId = prefs.getInt(KEY_USER_ID, 0)
    }

    // -------------------------------------------------------------------------
    // LIFECYCLE & STATUS MANAGEMENT (API BASED)
    // -------------------------------------------------------------------------

    override fun onStart() {
        super.onStart()
        // No action needed here
    }

    override fun onResume() {
        super.onResume()
        // Status updates must happen here since the app is returning from background
        setUserStatus("online")
        // Reload stories and posts to reflect latest data (e.g., after posting a new story)
        setupStories()
        setupPosts()
    }

    override fun onPause() {
        super.onPause()
        setUserStatus("offline")
    }

    override fun onStop() {
        super.onStop()
        setUserStatus("offline")
    }

    /**
     * Updates the user's online/offline status via the PHP API.
     */
    private fun setUserStatus(status: String) {
        val userId = currentUserId.toString()
        val url = baseUrl + "update_status.php"

        val stringRequest = object : StringRequest(
            Method.POST, url,
            { Log.d("API", "Status updated successfully: $status") },
            { error -> Log.e("API", "Error updating status: ${error.message}") }
        ) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["user_id"] = userId
                params["status"] = status
                return params
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }

    // -------------------------------------------------------------------------
    // STORY VIEW AND FETCHING (API BASED)
    // -------------------------------------------------------------------------

    /**
     * Initializes the RecyclerView and fetches stories, ensuring the 'Add Story' tile is first.
     */
    private fun setupStories() {

        val storyRecyclerView = findViewById<RecyclerView>(R.id.storyRecyclerView)
        storyRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val stories = mutableListOf<Story>()
        val adapter = StoryAdapter(stories) { story -> handleStoryClick(story) }
        storyRecyclerView.adapter = adapter

        val dbHelper = StoryDatabaseHelper(this)
        val currentDbUserId = currentUserId.toString()

        if (isOnline()) {
            fetchCurrentUserDetails(currentDbUserId, stories, adapter) { fetched ->
                dbHelper.insertStories(fetched)   // save for offline
            }
        } else {
            // OFFLINE MODE
            val cached = dbHelper.getAllStories()


            stories.clear()

            // re-add "Add Story" tile manually
            stories.add(
                Story(
                    userId = currentDbUserId,
                    username = "You",
                    profileImage = "",
                    storyImage = "PLACEHOLDER_ADD_STORY",
                    status = "",
                    timestamp = System.currentTimeMillis()
                )
            )

            stories.addAll(cached)

            adapter.notifyDataSetChanged()
        }
    }



    private fun fetchCurrentUserDetails(
        userId: String,
        stories: MutableList<Story>,
        adapter: StoryAdapter,
        onFetched: (List<Story>) -> Unit
    ) {
        val url = baseUrl + "get_user_profile.php?user_id=$userId"

        val request = StringRequest(Request.Method.GET, url, { response ->
            try {
                val json = JSONObject(response)

                if (json.getBoolean("success")) {
                    val user = json.getJSONObject("user")

                    val addTile = Story(
                        userId = userId,
                        username = user.getString("username"),
                        profileImage = user.getString("profile_image_url"),
                        storyImage = "PLACEHOLDER_ADD_STORY",
                        status = user.getString("status"),
                        timestamp = System.currentTimeMillis()
                    )

                    stories.clear()
                    stories.add(addTile)

                    // fetch other users
                    fetchReelsUsersForStories(userId, stories, adapter, onFetched)

                } else {
                    onFetched(stories)
                }

            } catch (e: Exception) {
                onFetched(stories)
            }

        }, { error ->
            onFetched(stories)
        })

        Volley.newRequestQueue(this).add(request)
    }



    private fun fetchReelsUsersForStories(
        currentUserId: String,
        stories: MutableList<Story>,
        adapter: StoryAdapter,
        onFetched: (List<Story>) -> Unit
    ) {
        val url = baseUrl + "get_reels_user.php?current_user_id=$currentUserId"

        val request = StringRequest(Request.Method.GET, url, { response ->
            try {
                val json = JSONObject(response)

                if (json.getBoolean("success")) {
                    val arr = json.getJSONArray("stories")

                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)

                        stories.add(
                            Story(
                                userId = s.getString("user_id"),
                                username = s.getString("username"),
                                profileImage = s.getString("profile_image_url"),
                                storyImage = s.getString("story_image_url"),
                                status = s.getString("status"),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }

                    // Correct ordering (most recent last)
                    val finalList = stories.drop(1)
                        .sortedByDescending { it.timestamp }

                    val addTile = stories.first()

                    stories.clear()
                    stories.add(addTile)
                    stories.addAll(finalList)

                    adapter.notifyDataSetChanged()
                    onFetched(stories)

                } else {
                    onFetched(stories)
                }

            } catch (e: Exception) {
                onFetched(stories)
            }
        }, { error ->
            onFetched(stories)
        })

        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Handles clicks on story tiles.
     */
    private fun handleStoryClick(story: Story) {
        val currentDbUserId = currentUserId.toString()

        val intent = Intent(this, View_story::class.java)
        intent.putExtra("viewingUserId", story.userId)
        intent.putExtra("viewingUsername", story.username)
        intent.putExtra("baseUrl", baseUrl)

        if (story.storyImage == "PLACEHOLDER_ADD_STORY") {
            intent.putExtra("canPost", true)
        } else if (story.userId == currentDbUserId) {
            intent.putExtra("canPost", true)
        } else {
            intent.putExtra("canPost", false)
            intent.putExtra("hasStories", true)
        }

        startActivity(intent)
    }


    // -------------------------------------------------------------------------
    // POSTS VIEW AND FETCHING (API BASED)
    // -------------------------------------------------------------------------

    @SuppressLint("ServiceCast")
    private fun isOnline(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }



    /**
     * Fetches post IDs for the feed using 'get_feed_post_ids.php' and sets up the PostAdapter.
     */
    private fun setupPosts() {
        val postRecyclerView = findViewById<RecyclerView>(R.id.postRecyclerView)
        postRecyclerView.layoutManager = LinearLayoutManager(this)

        val db = PostDatabaseHelper(this)

        if (!isOnline()) {
            Toast.makeText(this, "Showing offline data", Toast.LENGTH_LONG).show()

            val offlineList = db.loadOfflinePosts()
            Log.d("OFFLINE_SIZE", "Found ${offlineList.size} offline posts")

            postRecyclerView.adapter = PostAdapter(this, offlineList.toMutableList())
            return
        } else {


            // -----------------------------
            // ONLINE MODE → Fetch From API
            // -----------------------------
            val url = baseUrl + "get_posts.php"

            val stringRequest = StringRequest(Request.Method.GET, url, { response ->
                try {
                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.getBoolean("success")) {
                        val jsonPosts = jsonResponse.getJSONArray("posts")
                        val postsList = mutableListOf<Post>()

                        for (i in 0 until jsonPosts.length()) {
                            val postJson = jsonPosts.getJSONObject(i)

                            // Load comments...
                            val jsonComments = postJson.getJSONArray("comments")
                            val commentsList = mutableListOf<Comment>()

                            for (j in 0 until jsonComments.length()) {
                                val cObj = jsonComments.getJSONObject(j)
                                commentsList.add(
                                    Comment(
                                        commentId = cObj.getString("comment_id"),
                                        userId = cObj.getString("user_id"),
                                        username = cObj.getString("username"),
                                        text = cObj.getString("text"),
                                        timestamp = cObj.getLong("timestamp"),
                                        userDpBase64 = cObj.getString("userDpBase64")
                                    )
                                )
                            }

                            postsList.add(
                                Post(
                                    postId = postJson.getString("post_id"),
                                    userId = postJson.getString("user_id"),
                                    username = postJson.getString("username"),
                                    userProfileImage = postJson.getString("userProfileImage"),
                                    postImageBase64 = postJson.getString("postImageBase64"),
                                    caption = postJson.getString("caption"),
                                    likeCount = postJson.getInt("likeCount"),
                                    commentCount = postJson.getInt("commentCount"),
                                    timestamp = postJson.getLong("timestamp"),
                                    isLiked = postJson.getBoolean("isLiked"),
                                    comments = commentsList
                                )
                            )
                        }

                        // SHOW ONLINE POSTS
                        postRecyclerView.adapter = PostAdapter(this, postsList)

                        // SAVE TO SQLITE FOR OFFLINE USE
                        db.savePosts(postsList)

                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Parse Error", Toast.LENGTH_SHORT).show()
                }
            }, { error ->
                Toast.makeText(this, "Network Error", Toast.LENGTH_LONG).show()
            })

            Volley.newRequestQueue(this).add(stringRequest)
        }
    }

    // -------------------------------------------------------------------------
    // CAMERA & NAVIGATION
    // -------------------------------------------------------------------------

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                // Assuming you have an activity to handle posting the captured image
                val intent = Intent(this, AddPostActivity::class.java)
                intent.putExtra("imageBitmap", imageBitmap)
                startActivity(intent)
            } else {
                Toast.makeText(this, "❌ Image not captured", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavBar() {
        findViewById<ImageView>(R.id.nav_create).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            } else {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
            }
        }

        findViewById<ImageView>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, View_profile::class.java))
        }
        findViewById<ImageView>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, search_page::class.java))
        }
        findViewById<ImageView>(R.id.nav_like).setOnClickListener {
            startActivity(Intent(this, Liked_following::class.java))
        }
    }

    private fun setActiveNavIcon(active: String) {
        val home = findViewById<ImageView>(R.id.nav_home)
        val search = findViewById<ImageView>(R.id.nav_search)
        val like = findViewById<ImageView>(R.id.nav_like)
        val profile = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.nav_profile)

        // Reset all icons
        home.setImageResource(R.drawable.home)
        search.setImageResource(R.drawable.search)
        like.setImageResource(R.drawable.like)

        // Set the active icon
        when (active) {
            "home" -> home.setImageResource(R.drawable.home_dark)
            "search" -> search.setImageResource(R.drawable.search_dark)
            "like" -> like.setImageResource(R.drawable.like_dark)
            "profile" -> {
                // Highlight profile border
                profile.setBorderColor(ContextCompat.getColor(this, R.color.dark_brown))
            }
        }
    }
}