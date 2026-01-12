// File: chat_paage.kt (full Activity)
package com.example.assignment1.UI

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.Adapters.MessageAdapter
import com.example.assignment1.ChatMessage
import com.example.assignment1.R
import com.example.assignment1.Utils.BaseUrlUtil
import com.example.assignment1.helper.MessageDatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.HashMap
import android.os.FileObserver
import android.widget.ToggleButton
import com.android.volley.Request
import com.android.volley.Request.Method.POST

private var screenshotObserver: FileObserver? = null

class chat_paage : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageView
    private lateinit var videCall: ImageView
    private lateinit var voiceCall: ImageView

    private lateinit var sendImageButton: ImageView
    private lateinit var chatAdapter: MessageAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var dbHelper: MessageDatabaseHelper

    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"
    private lateinit var baseUrl: String

    private var currentUserId: String? = null
    private var receiverId: String? = null
    private var receiverName: String? = null
    private var receiverDpUrl: String? = null

    private val IMAGE_PICK_CODE = 1001

    // Polling setup for simulated real-time
    private val handler = Handler(Looper.getMainLooper())
    private val POLL_INTERVAL_MS: Long = 2000 // Poll every 2 seconds
    public var is_vanish_mode = "0"
    private val fetchMessagesRunnable = object : Runnable {
        override fun run() {
            listenForMessages()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private lateinit var receiver: com.example.assignment1.helper.ConnectivityReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_paage)

        dbHelper = MessageDatabaseHelper(this)

        // start resend service if online
        if (isOnline()) {
            startService(Intent(this, com.example.assignment1.helper.ResendService::class.java))
        }

        try {
            baseUrl = BaseUrlUtil.getBaseUrl(this)
        } catch (e: IOException) {
            Toast.makeText(this, "Base URL  Eror.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        loadUserId()

        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        receiverDpUrl = intent.getStringExtra("receiverDp")

        if (currentUserId == "0" || receiverId == null) {
            Toast.makeText(this, "Error: Invalid chattt user or not logged in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.searchEditText).text = receiverName
        findViewById<ImageView>(R.id.back_icon).setOnClickListener { finish() }
        videCall = findViewById(R.id.video_call)
        voiceCall = findViewById(R.id.audio_call)
        videCall.setOnClickListener {
            Toast.makeText(this, "clicked vid  call", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, outgoing_call::class.java)
            intent.putExtra("callType", "video")
            intent.putExtra("receiverId", receiverId)
            intent.putExtra("current_user_id", currentUserId.toString())


            startActivity(intent)
        }
        voiceCall.setOnClickListener {
            val intent = Intent(this, outgoing_call::class.java)
            Toast.makeText(this, "clicked Voice  call", Toast.LENGTH_SHORT).show()

            intent.putExtra("callType", "audio")
            intent.putExtra("receiverId", receiverId)
            intent.putExtra("current_user_id", currentUserId)

            startActivity(intent)

        }
        val toggle = findViewById<ToggleButton>(R.id.toggle_button)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            is_vanish_mode = if (isChecked) "1" else "0"
        }

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatRecyclerView.layoutManager = layoutManager

        // currentUserId should be non-null here; force unwrap is ok after earlier checks
        chatAdapter = MessageAdapter(messageList, currentUserId!!, baseUrl)
        chatRecyclerView.adapter = chatAdapter

        messageEditText = findViewById(R.id.s1)
        sendButton = findViewById(R.id.v1)
        sendImageButton = findViewById(R.id.i1)

        sendButton.setOnClickListener { sendMessage() }
        sendImageButton.setOnClickListener { pickImageFromGallery() }

        // enable edit / delete callbacks
        chatAdapter.onEdit = { message ->
            if (message.senderId == currentUserId && message.imagePath.isNullOrEmpty()) showEditDialog(message)
            else Toast.makeText(this, "Only your text messages can be edited.", Toast.LENGTH_SHORT).show()
        }
        chatAdapter.onDelete = { message ->
            if (message.senderId == currentUserId) deleteMessage(message)
            else Toast.makeText(this, "You can only delete your own messages.", Toast.LENGTH_SHORT).show()
        }

        loadOfflineMessages()
        startScreenshotDetection()
        startPolling()
    }

    private fun loadUserId() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentUserId = prefs.getInt(KEY_USER_ID, 0).toString()
        if (currentUserId == "0") Log.e("chat_page", "MySQL User ID not found in SharedPreferences!")
    }

    override fun onResume() {
        super.onResume()
        // register connectivity receiver dynamically
        receiver = com.example.assignment1.helper.ConnectivityReceiver { online ->
            if (online) startService(Intent(this, com.example.assignment1.helper.ResendService::class.java))
        }
        registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.w("chat_page", "Receiver not registered: ${e.message}")
        }
        stopPolling()
    }

    private fun startScreenshotDetection() {
        val dirs = listOf(
            "/storage/emulated/0/DCIM/Screenshots",
            "/storage/emulated/0/Pictures/Screenshots",
            "/storage/emulated/0/DCIM/ScreenShots",
            "/storage/emulated/0/Pictures/ScreenShots",
            "/storage/emulated/0/DCIM/Camera",
            "/storage/emulated/0/"
        )

        for (dir in dirs) {
            val folder = File(dir)
            if (folder.exists()) {
                val observer = object : FileObserver(dir, CREATE) {
                    override fun onEvent(event: Int, path: String?) {
                        if (path != null) {
                            val lower = path.lowercase()
                            if (lower.contains("screenshot") || lower.contains("screen") || lower.contains("shot") || lower.contains("capture")) {
                                runOnUiThread {
                                    Toast.makeText(this@chat_paage, "📸 Screenshot Detected!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
                screenshotObserver = observer
                screenshotObserver?.startWatching()
                Log.d("SCREENSHOT", "Watching: $dir")
                break
            }
        }
    }

    private fun startPolling() { handler.post(fetchMessagesRunnable) }
    private fun stopPolling() { handler.removeCallbacks(fetchMessagesRunnable) }

    private fun sendMessage() {
        val text = messageEditText.text.toString().trim()
        if (text.isEmpty()) return

        val msg = ChatMessage(
            messageId = "local_${System.currentTimeMillis()}",
            senderId = currentUserId!!,
            receiverId = receiverId,
            message = text,
            imagePath = null,
            postId = null,
            timestamp = System.currentTimeMillis(),
            isEdited = false,
            isDeleted = false,
            isSent = false
        )

        // UI + DB
        messageList.add(msg)
        chatAdapter.notifyItemInserted(messageList.size - 1)
        chatRecyclerView.scrollToPosition(messageList.size - 1)
        dbHelper.saveMessage(msg)
        messageEditText.setText("")

        // attempt send if online
        if (isOnline()) sendApiMessage(msg)
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    @Deprecated("onActivityResult is deprecated but still works on many projects. Consider Activity Result API later.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val maxSide = 1080
                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val (newW, newH) = if (originalBitmap.width > originalBitmap.height) {
                    Pair(maxSide, (maxSide / ratio).toInt())
                } else {
                    Pair((maxSide * ratio).toInt(), maxSide)
                }
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
                val baos = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                // build message and send (same flow as text)
                val msg = ChatMessage(
                    messageId = "local_${System.currentTimeMillis()}",
                    senderId = currentUserId!!,
                    receiverId = receiverId,
                    message = null,
                    imagePath = base64,
                    postId = null,
                    timestamp = System.currentTimeMillis(),
                    isEdited = false,
                    isDeleted = false,
                    isSent = false
                )

                messageList.add(msg)
                chatAdapter.notifyItemInserted(messageList.size - 1)
                chatRecyclerView.scrollToPosition(messageList.size - 1)
                dbHelper.saveMessage(msg)

                if (isOnline()) sendApiMessage(msg)

            } catch (e: Exception) {
                Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CHAT_IMAGE", "Image processing error", e)
            }
        }
    }

    private fun sendApiMessage(msg: ChatMessage) {
        val url = baseUrl + "send_message.php"
        val req = object : StringRequest(POST, url, { response ->
            try {
                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    // mark as sent locally
                    dbHelper.updateMessageStatus(msg.messageId, true)
                    // optional: update in-memory message state and refresh adapter
                    msg.isSent = true
                    chatAdapter.notifyDataSetChanged()
                    listenForMessages()
                } else {
                    Log.w("SEND", "Server returned success=false")
                }
            } catch (e: Exception) {
                Log.e("SEND", "Response parse error: ${e.message}")
            }
        }, { error ->
            Log.e("SEND", "Volley error: ${error.message}")
        }) {
            override fun getParams(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["sender_id"] = currentUserId!!
                map["receiver_id"] = receiverId!!
                map["text"] = msg.message ?: ""
                map["base64_image"] = msg.imagePath ?: ""
                map["vanish_mode"] = is_vanish_mode
                return map
            }
        }

        Volley.newRequestQueue(this).add(req)
    }

    private fun loadOfflineMessages() {
        if (receiverId == null) return
        val cached = dbHelper.getMessagesForChat(receiverId!!)
        messageList.clear()
        messageList.addAll(cached)
        chatAdapter.notifyDataSetChanged()
        chatRecyclerView.scrollToPosition(messageList.size - 1)
    }



    private fun listenForMessages() {
        if (receiverId == null) return
        val url = "${baseUrl}get_messages.php?user_id=${currentUserId}&receiver_id=${receiverId}"
        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.getBoolean("success")) {
                    val newMessages = mutableListOf<ChatMessage>()
                    val messagesArray: JSONArray = jsonResponse.getJSONArray("messages")
                    for (i in 0 until messagesArray.length()) {
                        val obj = messagesArray.getJSONObject(i)
                        val imagePath: String? = if (obj.has("image_path") && obj.get("image_path") != JSONObject.NULL) {
                            val path = obj.getString("image_path")
                            if (path.isNotEmpty()) baseUrl + path else null
                        } else null
                        val postId = if (obj.has("postId") && obj.get("postId") != JSONObject.NULL) obj.getString("postId") else null
                        val m = ChatMessage(
                            messageId = obj.getString("messageId"),
                            senderId = obj.getString("senderId"),
                            receiverId = receiverId,
                            message = obj.optString("message", null),
                            imagePath = imagePath,
                            postId = postId,
                            timestamp = obj.getLong("timestamp"),
                            isEdited = obj.optInt("is_edited", 0) == 1,
                            isDeleted = obj.optInt("is_deleted", 0) == 1,
                            isSent = true
                        )
                        newMessages.add(m)
                    }

                    dbHelper.deleteMessagesForChat(receiverId!!)
                    dbHelper.saveMessages(newMessages)

                    messageList.clear()
                    messageList.addAll(newMessages)
                    chatAdapter.notifyDataSetChanged()
                    if (newMessages.isNotEmpty()) chatRecyclerView.scrollToPosition(messageList.size - 1)
                } else {
                    Log.e("CHAT_API", "Fetch message error: ${jsonResponse.getString("message")}")
                }
            } catch (e: Exception) {
                Log.e("CHAT_API", "kjhkkjk JSON error: ${e.message}")
                loadOfflineMessages()
            }
        }, { error ->
            Log.e("CHAT_API", "Network error: ${error.message}")
            loadOfflineMessages()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun showEditDialog(message: ChatMessage) {
        val editText = EditText(this)
        editText.setText(message.message)
        editText.inputType = InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty() && newText != message.message) {
                    editMessageApi(message.messageId!!, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editMessageApi(messageId: String, newText: String) {
        val url = baseUrl + "edit_message.php"
        val stringRequest = object : StringRequest(POST, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.getBoolean("success")) {
                    Toast.makeText(this, "Message updated.", Toast.LENGTH_SHORT).show()
                    listenForMessages()
                } else {
                    Toast.makeText(this, "Edit failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Edit response error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Toast.makeText(this, "Edit network error: ${error.message}", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["message_id"] = messageId
                params["new_text"] = newText
                return params
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun deleteMessage(message: ChatMessage) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message?")
            .setMessage("This action will mark the message as deleted for both parties.")
            .setPositiveButton("Delete") { _, _ -> deleteMessageApi(message.messageId!!) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteVanishMessages(currentUserId.toString(), receiverId.toString())
        screenshotObserver?.stopWatching()
    }

    private fun deleteVanishMessages(user1: String, user2: String) {
        val url = baseUrl + "delete_vanish_messages.php"
        val request = object : StringRequest(Method.POST, url,
            { response -> Log.d("VANISH_DELETE", "Deleted vanish messages: $response") },
            { error -> Log.e("VANISH_DELETE", "Error: $error") }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["user1"] = user1
                map["user2"] = user2
                return map
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun deleteMessageApi(messageId: String) {
        val url = baseUrl + "delete_vanish_messages.php"
        val stringRequest = object : StringRequest(POST, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.getBoolean("success")) {
                    Toast.makeText(this, "Message deleted.", Toast.LENGTH_SHORT).show()
                    listenForMessages()
                } else {
                    Toast.makeText(this, "Delete failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Delete response error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Toast.makeText(this, "Delete network error: ${error.message}", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["message_id"] = messageId
                return params
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }
}
