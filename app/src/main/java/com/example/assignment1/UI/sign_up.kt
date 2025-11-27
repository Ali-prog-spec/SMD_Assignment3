package com.example.assignment1.UI

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.R
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class sign_up : AppCompatActivity() {

    private lateinit var dpIcon: CircleImageView
    private var selectedImageBase64: String? = null

    // ✅ Launch gallery picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val selectedImageUri = result.data!!.data
                if (selectedImageUri != null) {
                    dpIcon.setImageURI(selectedImageUri)
                    dpIcon.setPadding(0, 0, 0, 0)
                    selectedImageBase64 = uriToBase64(selectedImageUri)
                    Log.d("IMAGE", "Image selected and converted (${selectedImageBase64?.length ?: 0} chars)")
                }
            }
        }

    // ✅ Request permission and open gallery
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openGallery()
            else Toast.makeText(this, "Permission required to open gallery", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val createAccountButton = findViewById<Button>(R.id.create_account_button)

        val usernameField = findViewById<EditText>(R.id.usernameField)
        val firstNameField = findViewById<EditText>(R.id.firstNameField)
        val lastNameField = findViewById<EditText>(R.id.lastNameField)
        val dobField = findViewById<EditText>(R.id.dobField)
        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)

        dpIcon = findViewById(R.id.dp)

        // ✅ Back button
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // ✅ Handle profile picture click
        dpIcon.setOnClickListener { checkPermissionAndOpenGallery() }

        // ✅ Handle create account button
        createAccountButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val firstName = firstNameField.text.toString().trim()
            val lastName = lastNameField.text.toString().trim()
            val dob = dobField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
                dob.isEmpty() || email.isEmpty() || password.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imageBase64 = selectedImageBase64 ?: drawableToBase64(R.drawable.imran_khan_dp)

            // ✅ Make POST request to PHP API
            registerUser(username, firstName, lastName, dob, email, password, imageBase64)
        }
    }

    // ✅ Function to send POST request via Volley
    private fun registerUser(
        username: String,
        firstName: String,
        lastName: String,
        dob: String,
        email: String,
        password: String,
        imageBase64: String
    ) {
        //val url = "https://interracial-lennon-daughterly.ngrok-free.dev/socially_api/register.php"
        //val url = "https://shah-salman.fwh.is/socially_api/register.php"
        val url = getBaseUrl(this) + "register.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                Log.d("API", response)
                Toast.makeText(this, "Registered", Toast.LENGTH_SHORT).show()
                // Optionally navigate to login page
                val intent = Intent(this, initial_login_page::class.java)
                startActivity(intent)
                finish()
            },
            //shah-salman.fwh.is
            Response.ErrorListener { error ->
                Log.e("API_ERROR", error.toString())
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["username"] = username
                params["firstName"] = firstName
                params["lastName"] = lastName
                params["dob"] = dob
                params["email"] = email
                params["password"] = password
                params["image"] = imageBase64
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    // ✅ Check and request permission
    private fun checkPermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> openGallery()
                else -> requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> openGallery()
                else -> requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // ✅ Open gallery
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    // ✅ Convert selected URI to Base64 (compressed)
    private fun uriToBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    // ✅ Convert drawable resource to Base64
    private fun drawableToBase64(drawableId: Int): String {
        val drawable = ContextCompat.getDrawable(this, drawableId)
        val bitmap = (drawable as BitmapDrawable).bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
    fun getBaseUrl(context: Context): String {
        val input = context.resources.openRawResource(R.raw.base_url)
        return input.bufferedReader().use { it.readText().trim() }
    }
}