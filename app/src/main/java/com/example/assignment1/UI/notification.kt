package com.example.assignment1.UI

import NotificationAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.DataClass.Notification
import com.example.assignment1.R
import com.android.volley.Request
import org.json.JSONObject
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.Utils.BaseUrlUtil
import com.google.firebase.database.*
import java.io.IOException

class notification : AppCompatActivity() {

    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"

    private lateinit var recycler: RecyclerView
    private lateinit var notifAdapter: NotificationAdapter
    private val notifList = ArrayList<Notification>()
    private lateinit var baseUrl: String
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // 1. Initialize Base URL and User ID
        try {
            baseUrl = BaseUrlUtil.getBaseUrl(this)
        } catch (e: IOException) {
            Toast.makeText(this, "Base URL Initialization Error.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        loadUserId()
        //Toast.makeText(this, "About to featch Notification for user : ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()

        if (currentUserId.isNullOrEmpty() || currentUserId == "0") {
            Toast.makeText(this, "Authentication Error: Please log in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Setup RecyclerView
        recycler = findViewById(R.id.notificationRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        // Pass the current user ID to the adapter
        notifAdapter = NotificationAdapter(
            this,
            notifList,
            currentUserId!!
        )
        recycler.adapter = notifAdapter

        // 3. Fetch Data
        fetchNotifications()
    }

    private fun loadUserId() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentUserId = prefs.getInt(KEY_USER_ID, 0).toString()
    }

    private fun fetchNotifications() {
        val userId = currentUserId ?: return

        val url = "${baseUrl}fetch_notifications.php?user_id=$userId"

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.getBoolean("success")) {
                    notifList.clear()
                    val notificationsArray = jsonResponse.getJSONArray("notifications")

                    for (i in 0 until notificationsArray.length()) {
                        val notifJson = notificationsArray.getJSONObject(i)

                        // Map JSON to Data Class (Ensure field names match exactly!)
                        val notif = Notification(
                            id = notifJson.getInt("id"),
                            sender_id = notifJson.getInt("sender_id"),
                            type = notifJson.getString("type"),
                            related_id = if (notifJson.isNull("related_id")) null else notifJson.getInt("related_id"),
                            sender_username = notifJson.getString("sender_username"),
                            sender_dp_path = notifJson.getString("sender_dp_path"),
                            created_at = notifJson.getString("created_at")
                        )
                        notifList.add(notif)
                    }
                    notifAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error fetching notifications: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NOTIF_API", "Parsing error: ${e.message}. Response: $response")
                Toast.makeText(this, "Failed to parse notification data.", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Log.e("NOTIF_API", "Network error: ${error.message}")
            Toast.makeText(this, "Network error fetching notifications.", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }

    // The previous acceptFollowRequest and rejectFollowRequest functions are removed,
    // as the logic is now in the NotificationAdapter (where the buttons are handled).
}