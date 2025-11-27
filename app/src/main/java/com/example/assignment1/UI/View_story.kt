package com.example.assignment1.UI

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.assignment1.R
import com.example.assignment1.Utils.showTopToast
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class View_story : AppCompatActivity() {

    // Properties initialized in onCreate
    private lateinit var baseUrl: String
    private lateinit var viewingUserId: String
    private lateinit var viewingUsername: String
    private var canPost: Boolean = false

    // UI Elements
    private lateinit var storyImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var addStoryButton: ImageView
    private lateinit var backButton: ImageView

    // Data Structure to hold the story content fetched from the API
    private var storyImageUrls: List<String> = emptyList()
    private var currentStoryIndex: Int = 0

    // --- NEW CONSTANTS FOR GALLERY UPLOAD ---
    private val GALLERY_REQUEST_CODE = 400
    private val ADD_STORY_REQUEST_CODE = 300
    // ----------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_story)

        // --- 1. SAFE RETRIEVAL OF INTENT EXTRAS (Fixes NullPointerException) ---
        viewingUserId = intent.getStringExtra("viewingUserId") ?: run {
            Toast.makeText(this, "Error: Missing user ID for story view.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        viewingUsername = intent.getStringExtra("viewingUsername") ?: "Unknown User"
        baseUrl = intent.getStringExtra("baseUrl") ?: ""
        canPost = intent.getBooleanExtra("canPost", false)

        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "Error: Base URL is not configured.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // --- 2. Initialize UI Components using YOUR XML IDs ---
        try {
            storyImageView = findViewById(R.id.reel_image)
            usernameTextView = findViewById(R.id.reel_username)
            addStoryButton = findViewById(R.id.btn_add_story)
            backButton = findViewById(R.id.btn_close)

        } catch (e: Exception) {
            Log.e("VIEW_STORY", "Error finding view by ID: ${e.message}")
            Toast.makeText(this, "UI setup error. Check XML IDs.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Set listeners and data
        backButton.setOnClickListener { finish() }
        usernameTextView.text = viewingUsername

        // --- 3. Setup Logic based on who was clicked ---

        if (canPost) {
            // User clicked on their own 'Add Story' tile
            addStoryButton.visibility = View.VISIBLE
            addStoryButton.setOnClickListener {
                showTopToast("Launching gallery to select story...")
                // Direct call to start the gallery intent
                startGalleryForStory()
            }
            // Fetch the user's current stories (if any)
            fetchUserReels(viewingUserId)
        } else {
            // User clicked on another user's story
            addStoryButton.visibility = View.GONE
            fetchUserReels(viewingUserId)
        }

        // Set up click listeners for advancing/going back
        setupStoryNavigationListeners()
    }

    // --------------------------------------------------------------------------------
    // NEW STORY UPLOAD LOGIC
    // --------------------------------------------------------------------------------

    /**
     * Launches the device gallery to select an image for the story.
     */
    private fun startGalleryForStory() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    val imageUri: Uri = data.data!!
                    // Start the upload process
                    uploadStory(imageUri)
                }
                ADD_STORY_REQUEST_CODE -> {
                    // This can be used if you launch a dedicated Activity for final posting/cropping
                    fetchUserReels(viewingUserId)
                }
            }
        } else if (requestCode == GALLERY_REQUEST_CODE) {
            showTopToast("Story selection cancelled.", Toast.LENGTH_SHORT)
        }
    }

    /**
     * Converts the image URI to a Base64 string and sends it to the server.
     */
    private fun uploadStory(imageUri: Uri) {
        try {
            // 1. Convert URI to Bitmap
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

            // 2. Convert Bitmap to Base64 String
            val byteArrayOutputStream = ByteArrayOutputStream()
            // Compress the image. You may need to adjust the quality (0-100)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            // 3. Send Base64 String to PHP endpoint
            sendImageToServer(imageBase64)

        } catch (e: IOException) {
            Log.e("UPLOAD", "Error processing image URI: ${e.message}")
            showTopToast("Failed to prepare image for upload.", Toast.LENGTH_LONG)
        }
    }

    /**
     * Sends the Base64 image data and user ID to the server via Volley POST request.
     */
    private fun sendImageToServer(imageBase64: String) {
        val url = baseUrl + "upload_story.php"

        showTopToast("Uploading story...", Toast.LENGTH_SHORT)

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                // ... (rest of success handler is unchanged)
                Log.d("UPLOAD_API", "Response: $response")
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        showTopToast("✅ Story uploaded successfully!", Toast.LENGTH_LONG)
                        fetchUserReels(viewingUserId)
                    } else {
                        showTopToast("Upload failed: ${jsonResponse.getString("message")}", Toast.LENGTH_LONG)
                    }
                } catch (e: Exception) {
                    Log.e("UPLOAD_API", "JSON Parse Error: ${e.message}")
                    showTopToast("Server error during upload confirmation.", Toast.LENGTH_LONG)
                }
            },
            { error ->
                Log.e("UPLOAD_API", "Volley Error: ${error.message}")
                showTopToast("Network error during upload.", Toast.LENGTH_LONG)
            }
        ) {
            // --- FIX FOR 414 ERROR: Override getBody and getBodyContentType ---

            override fun getParams(): MutableMap<String, String>? {
                // Return null or an empty map here to prevent Volley from trying to use the URL
                return null
            }

            override fun getBody(): ByteArray {
                // Manually format the parameters as URL-encoded data for the POST body
                val params = HashMap<String, String>()
                params["user_id"] = viewingUserId
                params["image"] = imageBase64

                val builder = StringBuilder()
                var first = true
                for ((key, value) in params) {
                    if (first) {
                        first = false
                    } else {
                        builder.append("&")
                    }
                    builder.append(java.net.URLEncoder.encode(key, "UTF-8"))
                    builder.append("=")
                    builder.append(java.net.URLEncoder.encode(value, "UTF-8"))
                }
                return builder.toString().toByteArray()
            }

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=utf-8"
            }
            // ------------------------------------------------------------------
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }
    // --------------------------------------------------------------------------------
    // EXISTING STORY VIEW AND NAVIGATION LOGIC
    // --------------------------------------------------------------------------------

    /**
     * Fetches the specific reel URLs for the current viewing user.
     */
    private fun fetchUserReels(userId: String) {
        // ... (existing fetchUserReels code remains here)
        val url = baseUrl + "get_user_reels.php?user_id=$userId"
        // ...
        // [rest of fetchUserReels method unchanged]
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                Log.d("STORY_API", "Reels Response: $response")
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        val reelsArray = jsonResponse.getJSONArray("reels")
                        storyImageUrls = (0 until reelsArray.length()).map {
                            reelsArray.getString(it)
                        }

                        if (storyImageUrls.isNotEmpty()) {
                            currentStoryIndex = 0
                            displayCurrentStory()
                        } else if (!canPost) {
                            Toast.makeText(this, "$viewingUsername has no stories.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else if (!canPost) {
                        Toast.makeText(this, "No stories to view.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("STORY_API", "JSON Parse Error: ${e.message}")
                    if (!canPost) finish()
                }
            },
            { error ->
                Log.e("STORY_API", "Network Error: ${error.message}")
                if (!canPost) finish()
            }
        )
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun displayCurrentStory() {
        if (storyImageUrls.isEmpty()) return

        val imageUrl = storyImageUrls[currentStoryIndex]
        val fullImageUrl = if (imageUrl.startsWith("http")) imageUrl else baseUrl + imageUrl

        Glide.with(this)
            .load(fullImageUrl)
            .placeholder(R.drawable.default_icon) // Use your own default drawable
            .error(R.drawable.default_icon) // Use your own error drawable
            .into(storyImageView)
    }

    private fun setupStoryNavigationListeners() {
        val tapLeft = findViewById<View>(R.id.touch_left)
        val tapRight = findViewById<View>(R.id.touch_right)

        tapLeft.setOnClickListener {
            if (currentStoryIndex > 0) {
                currentStoryIndex--
                displayCurrentStory()
            } else {
                Toast.makeText(this, "End of stories.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        tapRight.setOnClickListener {
            if (currentStoryIndex < storyImageUrls.size - 1) {
                currentStoryIndex++
                displayCurrentStory()
            } else {
                Toast.makeText(this, "End of stories.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}