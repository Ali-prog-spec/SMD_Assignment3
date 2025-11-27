package com.example.assignment1.UI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.DataClass.User // ASSUMPTION: User data class exists here
import com.example.assignment1.R
import com.example.assignment1.Adapters.SearchAdapter
import com.example.assignment1.Utils.BaseUrlUtil // ASSUMPTION: BaseUrlUtil exists
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class Search_screen : AppCompatActivity() {

    private lateinit var adapter: SearchAdapter
    private val allUsers = mutableListOf<User>()
    private lateinit var filterSpinner: Spinner
    private var currentFilter = "All"

    // MySQL/API Properties
    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"
    private lateinit var baseUrl: String
    private var currentUserId: String? = null // Current user ID from MySQL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // 1. Initialize API components
        try {
            baseUrl = BaseUrlUtil.getBaseUrl(this)
        } catch (e: IOException) {
            Toast.makeText(this, "Base URL Initialization Error.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        loadUserId()

        if (currentUserId == "0" || currentUserId == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // RecyclerView setup
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SearchAdapter(allUsers) { selectedUser ->
            // Use selectedUser.uid which holds the MySQL ID
            val intent = Intent(this, View_others_profile_follow::class.java)
            // Use 'uid' which we map from 'userId' in fetchAllUsers
            intent.putExtra("id", selectedUser.uid)
            Toast.makeText(this, "Clicked: ${selectedUser.username}", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Search bar
        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
        })
        filterSpinner = findViewById(R.id.filterSpinner)

// Spinner items
        val filterOptions = listOf("All", "Followers", "Following")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentFilter = filterOptions[position]
                loadBasedOnFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Fetch all users using the new API method
        //fetchAllUsers()
    }
    private fun loadBasedOnFilter() {
        when (currentFilter) {
            "All" -> fetchAllUsers()
            "Followers" -> fetchFollowers()
            "Following" -> fetchFollowing()
        }
    }
    private fun fetchFollowers() {
        val url = "${baseUrl}get_follow_data.php?user_id=$currentUserId&type=followers"

        val req = StringRequest(Request.Method.GET, url, { response ->
            parseUserResponse(response)
        }, {
            Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(req)
    }
    private fun fetchFollowing() {
        val url = "${baseUrl}get_follow_data.php?user_id=$currentUserId&type=following"

        val req = StringRequest(Request.Method.GET, url, { response ->
            parseUserResponse(response)
        }, {
            Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(req)
    }

    private fun loadUserId() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Ensure that the stored ID is used as the current user ID (as a String)
        currentUserId = prefs.getInt(KEY_USER_ID, 0).toString()
        if (currentUserId == "0") {
            Log.e("Search_screen", "MySQL User ID not found in SharedPreferences!")
        }
    }

    private fun fetchAllUsers() {
        // Construct the URL to your PHP API, passing the current user ID
        val url = "${baseUrl}get_users.php?user_id=${currentUserId}"

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            try {
                val jsonResponse = JSONObject(response)

                if (jsonResponse.getBoolean("success")) {
                    val usersArray: JSONArray = jsonResponse.getJSONArray("users")
                    val fetchedUsers = mutableListOf<User>()

                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)

                        // Map the MySQL result keys to the User data class fields
                        val user = User(
                            // Assuming User data class uses 'uid' for the user ID
                            uid = obj.getString("userId"),
                            username = obj.getString("username"),
                            dp64 = baseUrl + obj.getString("dp64") // Prepend baseUrl to the relative path
                            // Add other fields as needed (e.g., email, bio)
                        )
                        fetchedUsers.add(user)
                    }

                    allUsers.clear()
                    allUsers.addAll(fetchedUsers)
                    adapter.updateList(allUsers) // Display all users initially

                } else {
                    Toast.makeText(this, "Fetch failed: ${jsonResponse.getString("message")}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("Search_screen", "Parsing error: ${e.message}. Response: $response")
                Toast.makeText(this, "Failed to parse user data.", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Log.e("Search_screen", "Network error: ${error.message}")
            Toast.makeText(this, "Network error fetching users.", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }
    private fun parseUserResponse(response: String) {
        try {
            val jsonResponse = JSONObject(response)

            if (jsonResponse.getBoolean("success")) {
                val usersArray = jsonResponse.getJSONArray("users")
                val fetchedUsers = mutableListOf<User>()

                for (i in 0 until usersArray.length()) {
                    val obj = usersArray.getJSONObject(i)

                    fetchedUsers.add(
                        User(
                            uid = obj.getString("userId"),
                            username = obj.getString("username"),
                            dp64 = baseUrl + obj.getString("dp64")
                        )
                    )
                }

                allUsers.clear()
                allUsers.addAll(fetchedUsers)
                adapter.updateList(allUsers)

            } else {
                Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("Search_screen", "Parsing error: ${e.message}")
        }
    }

    private fun filterUsers(query: String) {
        val filtered = allUsers.filter {
            // Case-insensitive filtering on the username
            it.username?.contains(query, ignoreCase = true) == true
        }
        adapter.updateList(filtered)
    }
}