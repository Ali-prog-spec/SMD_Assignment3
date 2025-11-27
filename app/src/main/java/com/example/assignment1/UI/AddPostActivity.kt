package com.example.assignment1.UI

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment1.R
import com.example.assignment1.Utils.BaseUrlUtil // Assuming this utility exists
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class AddPostActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var captionEditText: EditText
    private lateinit var uploadButton: Button

    // --- New/Updated Fields for PHP/Volley ---
    private lateinit var baseUrl: String
    private var currentUserId: Int = 0
    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"
    // ----------------------------------------

    private var selectedImageUri: Uri? = null
    private var imageBitmapFromCamera: Bitmap? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)
        Toast.makeText(this, "Have Reached Add post", Toast.LENGTH_SHORT).show()

        // 1. Load User ID
        loadUserId()
        if (currentUserId == 0) {
            Toast.makeText(this, "Session expired, cannot upload post.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 2. Initialize Base URL
        try {
            baseUrl = BaseUrlUtil.getBaseUrl(this)
        } catch (e: IOException) {
            Toast.makeText(this, "Base URL configuration error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 3. UI Setup
        imageView = findViewById(R.id.imagePreview)
        captionEditText = findViewById(R.id.captionInput)
        uploadButton = findViewById(R.id.uploadButton)
        //val selectImageButton = findViewById<Button>(R.id.selectImageButton) // Assuming you add a select button

        // Check if imageBitmap was passed from Home (Camera)
        val bitmap = intent.getParcelableExtra<Bitmap>("imageBitmap")
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
            selectedImageUri = null
            imageBitmapFromCamera = bitmap // Store for upload
        }

//        selectImageButton.setOnClickListener {
//            openGallery()
//        }

        uploadButton.setOnClickListener {
            uploadPost()
        }
    }

    // Helper to open the image gallery
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            imageView.setImageURI(selectedImageUri)
            imageBitmapFromCamera = null // Reset camera bitmap if selecting from gallery
        }
    }

    /**
     * Loads the stored MySQL user ID from Shared Preferences.
     */
    private fun loadUserId() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentUserId = prefs.getInt(KEY_USER_ID, 0)
    }

    // -------------------------------------------------------------------------
    // PHP/VOLLEY UPLOAD LOGIC
    // -------------------------------------------------------------------------

    private fun uploadPost() {
        val caption = captionEditText.text.toString().trim()
        val userId = currentUserId.toString()

        if (currentUserId == 0) {
            Toast.makeText(this, "Error: User ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        if ((selectedImageUri == null && imageBitmapFromCamera == null)) {
            Toast.makeText(this, "Select an image to post.", Toast.LENGTH_SHORT).show()
            return
        }

        val base64Image = when {
            selectedImageUri != null -> convertUriToBase64(selectedImageUri!!)
            imageBitmapFromCamera != null -> convertBitmapToBase64(imageBitmapFromCamera!!)
            else -> ""
        }

        if (base64Image.isEmpty()) {
            Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = baseUrl + "upload_post.php"
        Toast.makeText(this, "Starting post upload...", Toast.LENGTH_LONG).show()

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(this, "✅ Post uploaded successfully!", Toast.LENGTH_LONG).show()
                        finish() // Close activity on success
                    } else {
                        val message = jsonResponse.getString("message")
                        Toast.makeText(this, "❌ Upload failed: $message", Toast.LENGTH_LONG).show()
                        Log.e("API_UPLOAD", "Server Error: $message")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ Server response error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("API_UPLOAD", "JSON Parse Error: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(this, "❌ Network error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("API_UPLOAD", "Volley Error: ${error.message}")
            }
        ) {
            // This is the correct way to send large Base64 data with Volley
            override fun getParams(): MutableMap<String, String>? {
                return null // Use getBody for large data
            }

            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["user_id"] = userId
                params["image"] = base64Image
                params["caption"] = caption

                val builder = StringBuilder()
                var first = true
                for ((key, value) in params) {
                    if (!first) {
                        builder.append("&")
                    }
                    builder.append(java.net.URLEncoder.encode(key, "UTF-8"))
                    builder.append("=")
                    builder.append(java.net.URLEncoder.encode(value, "UTF-8"))
                    first = false
                }
                return builder.toString().toByteArray(Charsets.UTF_8)
            }

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=utf-8"
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }

    // Existing helper functions, renamed for clarity
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        // Compress the image to save bandwidth/size
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun convertUriToBase64(uri: Uri): String {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            return convertBitmapToBase64(bitmap)
        } catch (e: Exception) {
            Log.e("ImageConvert", "Failed to convert Uri to Base64: ${e.message}")
            return ""
        }
    }
}