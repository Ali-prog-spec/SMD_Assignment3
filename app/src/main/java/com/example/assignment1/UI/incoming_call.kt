package com.example.assignment1.UI

import android.content.Intent
import android.os.Bundle
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

class incoming_call : AppCompatActivity() {

    private lateinit var btnAccept: ImageView
    private lateinit var btnReject: ImageView
    private lateinit var tvCallerName: TextView

    private var callId: Int? = null
    private var callType: String? = null
    private var channelName: String? = null
    private var callerName: String? = null
    private lateinit var baseUrl: String

    private val TAG = "INCOMING_CALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        baseUrl = BaseUrlUtil.getBaseUrl(this)

        // Bind UI
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)
        tvCallerName = findViewById(R.id.tvCallerName)

        // Get data from intent
        callId = intent.getIntExtra("id", 0).takeIf { it != 0 }
        channelName = intent.getStringExtra("channelName")
        callerName = intent.getStringExtra("callerName")
        callType = intent.getStringExtra("callType")

        tvCallerName.text = "Incoming call from ${callerName ?: "Unknown"}"

        btnAccept.setOnClickListener { acceptCall() }
        btnReject.setOnClickListener { rejectCall() }
    }

    /** ✅ Accept Call: Update status to "active" */
    private fun acceptCall() {
        if (callId == null || channelName.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid call data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        updateCallStatus("active") {
            Log.d(TAG, "Call accepted, launching call screen")
            // Launch appropriate call screen
            Toast.makeText(this, callType, Toast.LENGTH_SHORT).show()
            val nextIntent = if (callType == "audio") {
                Intent(this, Call_page::class.java)
            } else {
                Intent(this, Vcall_page::class.java)
            }

            nextIntent.putExtra("channelName", channelName)
            nextIntent.putExtra("callId", callId)
            nextIntent.putExtra("callerName", callerName)
            nextIntent.putExtra("callType", callType)
            startActivity(nextIntent)
            finish()
        }
    }

    /** ❌ Reject Call: Update status to "ended" (enum-safe) */
    private fun rejectCall() {
        callId?.let {
            updateCallStatus("ended") {
                Log.d(TAG, "Call rejected")
                Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Log.e(TAG, "rejectCall: callId is null")
            finish()
        }
    }

    /** 🔁 End Call: Update status to "ended" (call from Call_page / Vcall_page) */
    fun endCall() {
        callId?.let {
            updateCallStatus("ended") {
                Log.d(TAG, "Call ended")
                Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
            }
        } ?: Log.e(TAG, "endCall: callId is null")
    }

    /**
     * Update call status via API
     * Only valid ENUM values: ringing, active, ended
     */
    private fun updateCallStatus(status: String, onSuccess: (() -> Unit)? = null) {
        val finalStatus = when (status.lowercase()) {
            "active" -> "active"
            "ended", "rejected", "cancelled" -> "ended"
            else -> "ringing"
        }

        val id = callId
        if (id == null || id == 0) {
            Log.e(TAG, "updateCallStatus: Invalid callId: $id")
            return
        }

        val url = baseUrl + "update_call_status.php"
        val params = hashMapOf(
            "call_id" to id.toString(),
            "status" to finalStatus
        )

        Log.d(TAG, "Updating call status: $params")

        val req = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                Log.d(TAG, "Status update response: $response")
                try {
                    val j = org.json.JSONObject(response)
                    if (j.optBoolean("success", false)) {
                        Log.d(TAG, "Call status updated to $finalStatus")
                        onSuccess?.invoke()
                    } else {
                        Log.e(TAG, "Failed to update status: ${j.optString("message")}")
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error: ${e.message}")
                    Toast.makeText(this, "Parse error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e(TAG, "Network error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> = params
        }

        Volley.newRequestQueue(this).add(req)
    }
}