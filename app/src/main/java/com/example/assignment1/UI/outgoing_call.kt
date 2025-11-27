package com.example.assignment1.UI

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.R
import com.example.assignment1.Utils.BaseUrlUtil
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.util.*

class outgoing_call : AppCompatActivity() {

    private lateinit var tvCalleeName: TextView
    private lateinit var btnCancelCall: ImageView
    private lateinit var imgCallee: CircleImageView

    private var receiverId: String? = null
    private var receiverName: String? = null
    private var receiverProfileBase64: String? = null
    private var callType: String? = "audio"

    private var callIdFromServer: Int? = null
    private var callLocalUUID: String = UUID.randomUUID().toString()
    private var channelName = "test123"
    private var token: String ="007eJxTYOg78vCO+Oy+XW5qnLPnlracXiofrZn0LULpJndmn5Avh5ACg7FRsrlxmqVxcqJpkompqbmlUbKhkaWlsUmqWWKikYnhp+NqmQ2BjAwMuu+ZGRkgEMRnZyhJLS4xNDJmYAAAJFgdxw=="
    private var currentUserId = ""
    private lateinit var baseUrl: String

    private val handler = Handler(Looper.getMainLooper())
    private val pollDelay = 1500L
    private val pollRunnable = object : Runnable {
        override fun run() {
            callIdFromServer?.let { pollCallStatus(it) }
            handler.postDelayed(this, pollDelay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outgoing_call)

        tvCalleeName = findViewById(R.id.tvCalleeName)
        btnCancelCall = findViewById(R.id.btnCancelCall)
        imgCallee = findViewById(R.id.imgCallee)

        // Get intent extras
        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        receiverProfileBase64 = intent.getStringExtra("receiverProfile")
        callType = intent.getStringExtra("callType") ?: "audio"
        currentUserId = intent.getStringExtra("current_user_id").toString()

        tvCalleeName.text = "Calling ${receiverName ?: "User"}..."
        setProfilePicture(receiverProfileBase64)

        baseUrl = BaseUrlUtil.getBaseUrl(this)

        if (currentUserId.isEmpty() || receiverId.isNullOrEmpty()) {
            Toast.makeText(this, "Missing user info", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnCancelCall.setOnClickListener { cancelCall() }

        // Create call on server
        createCallOnServer()
    }

    private fun setProfilePicture(base64String: String?) {
        if (!base64String.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgCallee.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imgCallee.setImageResource(R.drawable.default_user)
            }
        } else {
            imgCallee.setImageResource(R.drawable.default_user)
        }
    }

    /** Create call on server and start polling */
    private fun createCallOnServer() {
        val url = baseUrl + "create_call.php"
        val req = object : StringRequest(Method.POST, url,
            { response ->
                Log.d("CALL_DEBUG", "Create call response: $response")
                try {
                    val j = JSONObject(response)
                    if (j.optBoolean("success", false)) {
                        val call = j.getJSONObject("call")
                        callIdFromServer = call.optInt("id", 0).takeIf { it != 0 }
                        channelName = call.optString("channel_name", channelName)
                        Log.d("CALL_DEBUG", "Call created: id=$callIdFromServer, channel=$channelName")
                        handler.post(pollRunnable)
                    } else {
                        Toast.makeText(this, "Failed to create call: ${j.optString("message")}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("CALL_DEBUG", "Create call parse error: ${e.message}")
                    finish()
                }
            },
            { error ->
                Log.e("CALL_DEBUG", "Create call network error: ${error.message}")
                finish()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "caller_id" to currentUserId,
                    "receiver_id" to receiverId!!,
                    "channel_name" to channelName,
                    "token" to (token ?: ""),
                    "status" to "ringing",
                    "callType" to (callType ?: "audio")

                )
            }
        }

        Volley.newRequestQueue(this).add(req)
    }

    /** Poll the server for status updates */
    private fun pollCallStatus(serverCallId: Int) {
        val url = "${baseUrl}get_call_status.php?call_id=$serverCallId"
        val req = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val j = JSONObject(response)
                    if (j.optBoolean("success", false)) {
                        val call = j.getJSONObject("call")
                        val status = call.optString("status", "ringing")
                        Log.d("CALL_DEBUG", "Polled status: $status")

                        when (status) {
                            "active" -> {
                                handler.removeCallbacks(pollRunnable)
                                val channelFromServer = call.optString("channel_name", channelName)
                                updateCallStatus("active") // update before launching
                                goToCallScreen(channelFromServer, token)
                            }
                            "ended" -> {
                                handler.removeCallbacks(pollRunnable)
                                updateCallStatus("ended") // map all to ENUM
                                Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } else {
                        Log.w("CALL_DEBUG", "Poll call failed: ${j.optString("message")}")
                    }
                } catch (e: Exception) {
                    Log.e("CALL_DEBUG", "Poll parse error: ${e.message}")
                }
            },
            { error ->
                Log.e("CALL_DEBUG", "Poll network error: ${error.message}")
            })

        Volley.newRequestQueue(this).add(req)
    }

    /** Go to actual call screen */
    private fun goToCallScreen(channel: String, token: String?) {
        val nextIntent = if (callType == "audio") {
            Intent(this@outgoing_call, Call_page::class.java)
        } else {
            Intent(this@outgoing_call, Vcall_page::class.java)
        }
        nextIntent.putExtra("channelName", channel)
        nextIntent.putExtra("token", token)
        nextIntent.putExtra("callId", callIdFromServer)
        nextIntent.putExtra("callerId", currentUserId)
        nextIntent.putExtra("receiverId", receiverId)
        startActivity(nextIntent)
        finish()
    }

    /** Cancel call */
    private fun cancelCall() {
        callIdFromServer?.let {
            updateCallStatus("ended") {
                Toast.makeText(this, "Call cancelled", Toast.LENGTH_SHORT).show()
                handler.removeCallbacks(pollRunnable)
                finish()
            }
        } ?: run {
            Log.e("CALL_DEBUG", "cancelCall called but callIdFromServer is null")
            handler.removeCallbacks(pollRunnable)
            finish()
        }
    }

    /** Update call status via API with ENUM mapping */
    private fun updateCallStatus(status: String, onSuccess: (() -> Unit)? = null) {
        val finalStatus = when (status) {
            "accepted", "active" -> "active"
            "cancelled", "rejected", "ended" -> "ended"
            else -> "ringing"
        }

        val id = callIdFromServer
        if (id == null || id == 0) {
            Log.e("CALL_DEBUG", "updateCallStatus called with invalid callId: $id")
            return
        }

        val url = baseUrl + "update_call_status.php"
        val params = hashMapOf(
            "call_id" to id.toString(),
            "status" to finalStatus
        )

        Log.d("CALL_DEBUG", "Updating call status: $params")

        val req = object : StringRequest(Method.POST, url,
            { response ->
                Log.d("CALL_DEBUG", "Status update response: $response")
                try {
                    val j = JSONObject(response)
                    if (j.optBoolean("success", false)) {
                        Log.d("CALL_DEBUG", "Call status updated to $finalStatus")
                        onSuccess?.invoke()
                    } else {
                        Log.e("CALL_DEBUG", "Failed to update status: ${j.optString("message")}")
                    }
                } catch (e: Exception) {
                    Log.e("CALL_DEBUG", "Update parse error: ${e.message}")
                }
            },
            { error ->
                Log.e("CALL_DEBUG", "Update network error: ${error.message}")
            }) {
            override fun getParams(): MutableMap<String, String> = params
        }

        Volley.newRequestQueue(this).add(req)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }
}