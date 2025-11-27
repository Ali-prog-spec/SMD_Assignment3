package com.example.assignment1.UI

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assignment1.Utils.BaseUrlUtil
import org.json.JSONObject

class IncomingCallService : Service() {

    private var userId: String? = null
    private lateinit var baseUrl: String
    private val handler = Handler(Looper.getMainLooper())
    private val pollingDelay = 2000L  // 2 seconds polling

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        baseUrl = BaseUrlUtil.getBaseUrl(this)
        userId = intent?.getStringExtra("userId") ?: "1"

        pollIncomingCalls()

        return START_STICKY
    }

    /** 🔁 Poll MySQL API every 2 seconds */
    private fun pollIncomingCalls() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkIncomingCall()
                handler.postDelayed(this, pollingDelay)
            }
        }, pollingDelay)
    }

    /** 📞 Call API: get_incoming_call.php */
    private fun checkIncomingCall() {

        val url = "${baseUrl}get_incoming_call.php?receiver_id=$userId"

       // Toast.makeText(this, "Checking for incoming calls...", Toast.LENGTH_SHORT).show()

        val request = StringRequest(Request.Method.GET, url, { response ->

            try {
                val json = JSONObject(response)

                if (json.getBoolean("success")) {
                    val call = json.getJSONObject("call")

                    // 🔥 GET ALL VALUES FROM DATABASE
                    val id = call.getString("id")
                    val callerId = call.getString("caller_id")
                    val receiverId = call.getString("receiver_id")
                    //val channelName = call.getString("channelName")
                    val callType = call.getString("callType")
                    val token = call.optString("token", "")
                    val status = call.getString("status")
                    val createdAt = call.getString("created_at")

                    Log.d("CALL_DEBUG", "📞 Incoming call from $callerId")

                    launchIncomingCallScreen(
                        id = id,
                        callerId = callerId,
                        receiverId = receiverId,
                        channelName = "test123",
                        token = token,
                        status = status,
                        callType=callType,
                        createdAt = createdAt
                    )
                }

            } catch (e: Exception) {
                Log.e("CALL_DEBUG", "JSON Error: ${e.message}")
            }

        }, { error ->
            Log.e("CALL_DEBUG", "Network Error: ${error.message}")
        })

        Volley.newRequestQueue(this).add(request)
    }

    /** 🎬 Launch Incoming Call Activity */
    private fun launchIncomingCallScreen(
        id: String,
        callerId: String,
        receiverId: String,
        channelName: String,
        token: String,
        status: String,
        callType:String,
        createdAt: String
    ) {
        //Toast.makeText(this, "going for call", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, incoming_call::class.java).apply {

            putExtra("id", id.toInt())
            putExtra("caller_id", callerId)
            putExtra("receiverId", receiverId)
            putExtra("channelName", channelName)
            putExtra("token", token)
            putExtra("status", status)
            putExtra("createdAt", createdAt)
            putExtra("callType", callType)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        Log.d("IncomingCallService", "🎬 Opening Incoming Call Screen")
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}