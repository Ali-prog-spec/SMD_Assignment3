package com.example.assignment1.UI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.Adapters.ChatAdapter
import com.example.assignment1.DataClass.ChatIttem
import com.example.assignment1.R
import com.example.assignment1.Utils.BaseUrlUtil
import com.example.assignment1.helper.UserDatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class message_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatIttem>()

    private var CURRENT_DB_USER_ID: Int = 0
    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"
    private lateinit var baseUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message_page)

        // Initialize base URL and User ID
        try {
            // This baseUrl should contain the current ngrok address (e.g., http://new-ngrok-link/socially_api/)
            baseUrl = BaseUrlUtil.getBaseUrl(this)
        } catch (e: IOException) {
            Toast.makeText(this, "Base URL Initialization Error.", Toast.LENGTH_LONG).show()
            return
        }
        loadUserId()

        // 🔙 Back icon click → go back to home_page
        val backButton = findViewById<ImageView>(R.id.back_btn_msg)
        backButton.setOnClickListener {
            finish() // Use finish() to go back to the previous activity (home_page)
        }

        recyclerView = findViewById(R.id.chat_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatAdapter(chatList) { selectedChat ->
            Log.d("NAVIGATION", "Opening chat with UID: ${selectedChat.userId}")
            val intent = Intent(this, chat_paage::class.java)
            Toast.makeText(this, "Opening chat with: ${selectedChat.username}", Toast.LENGTH_SHORT).show()

            // Pass the required chat details
            intent.putExtra("receiverId", selectedChat.userId)
            intent.putExtra("receiverName", selectedChat.username)
            intent.putExtra("receiverDp", selectedChat.imageUrl) // Using imageUrl for dp64 path
            startActivity(intent)
        }
        recyclerView.adapter = chatAdapter

        // Start fetching data from MySQL API
        fetchUsersFromApi()
    }

    /**
     * Loads the stored MySQL user ID from Shared Preferences.
     */
    private fun loadUserId() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        CURRENT_DB_USER_ID = prefs.getInt(KEY_USER_ID, 0)
        if (CURRENT_DB_USER_ID == 0) {
            Log.e("message_page", "MySQL User ID not found in SharedPreferences! Cannot fetch users.")
            Toast.makeText(this, "Login required.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadOfflineUsers(dbHelper: UserDatabaseHelper) {
        Toast.makeText(this, "Offline Mode Enabled", Toast.LENGTH_SHORT).show()

        val cached = dbHelper.getUsers()

        chatList.clear()
        chatList.addAll(cached)

        chatAdapter.notifyDataSetChanged()
    }


    private fun fetchUsersFromApi() {
        val dbHelper = UserDatabaseHelper(this)

        val url = "${baseUrl}get_users.php?user_id=$CURRENT_DB_USER_ID"

        val request = StringRequest(Request.Method.GET, url, { response ->
            try {
                val json = JSONObject(response)
                chatList.clear()

                if (json.getBoolean("success")) {
                    val arr = json.getJSONArray("users")

                    val tempList = mutableListOf<ChatIttem>()

                    for (i in 0 until arr.length()) {
                        val u = arr.getJSONObject(i)

                        val userItem = ChatIttem(
                            userId = u.getString("userId"),
                            username = u.getString("username"),
                            message = "Start Chat",
                            imageUrl = baseUrl + u.getString("dp64"),
                            time = ""
                        )

                        chatList.add(userItem)
                        tempList.add(userItem)
                    }

                    // SAVE FOR OFFLINE USE
                    dbHelper.saveUsers(tempList)

                    chatAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                loadOfflineUsers(dbHelper)
            }
        }, {
            // NETWORK ERROR → OFFLINE MODE
            loadOfflineUsers(dbHelper)
        })

        Volley.newRequestQueue(this).add(request)
    }

}