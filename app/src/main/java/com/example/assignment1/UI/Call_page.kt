package com.example.assignment1.UI

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.assignment1.R
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine

class Call_page : AppCompatActivity() {

    private var mRtcEngine: RtcEngine? = null

    // Agora credentials (replace with your own)
    private val APP_ID = "32c73f93ca5b455792c129934e6aa241"
    private val TOKEN ="007eJxTYOg78vCO+Oy+XW5qnLPnlracXiofrZn0LULpJndmn5Avh5ACg7FRsrlxmqVxcqJpkompqbmlUbKhkaWlsUmqWWKikYnhp+NqmQ2BjAwMuu+ZGRkgEMRnZyhJLS4xNDJmYAAAJFgdxw=="
    private var CHANNEL_NAME="test123"
    private val TAG = "audio Call"

    private lateinit var callStatus: TextView
    private lateinit var endButton: ImageView

    /** Agora event handler **/
    private val mRtcHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                showStyledToast("✅ Joined channel: $channel")
                callStatus.text = "Connected to channel: $channel"
                Log.d(TAG, "Joining channel: $CHANNEL_NAME")

            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                showStyledToast("🎧 User joined the call!")
                callStatus.text = "In call..."
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                showStyledToast("❌ User left the call")
                finishCall()
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                showStyledToast("📞 Call ended")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_page)

        // Bind UI
        callStatus = findViewById(R.id.call_status)
        endButton = findViewById(R.id.btn_end_call)

        // Get channel name from intent
        //CHANNEL_NAME = intent.getStringExtra("channelName")

        if (CHANNEL_NAME.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Missing channel name", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Request audio permission if needed
        if (hasAudioPermission()) {
            initAgoraAndJoin()
        } else {
            requestAudioPermission()
        }

        // End call button action
        endButton.setOnClickListener {
            finishCall()
        }
    }

    /** ✅ Initialize Agora and Join the Channel **/
    private fun initAgoraAndJoin() {
        try {
            // Create Agora engine
            mRtcEngine = RtcEngine.create(baseContext, APP_ID, mRtcHandler)
            mRtcEngine?.setLogFile("${externalCacheDir?.absolutePath}/agora.log")

            mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            mRtcEngine?.enableAudio()

            // Join channel
            mRtcEngine?.joinChannel(TOKEN, CHANNEL_NAME, "", 0)
            callStatus.text = "Connecting..."

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Agora initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.d("Not joined", " Unable To join")

            finish()
        }
    }

    /** ✅ End Call and Clean Up **/
    private fun finishCall() {
        try {
            mRtcEngine?.leaveChannel()
            RtcEngine.destroy()
            mRtcEngine = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

    /** ✅ Red Styled Toast **/
    private fun showStyledToast(message: String) {
        val toast = Toast.makeText(this@Call_page, message, Toast.LENGTH_SHORT)
        val view = toast.view
        view?.setBackgroundColor(Color.RED)
        val text = view?.findViewById<TextView>(android.R.id.message)
        text?.setTextColor(Color.WHITE)
        text?.textSize = 16f
        text?.setPadding(24, 16, 24, 16)
        toast.show()
    }

    /** ✅ Permission Handling **/
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initAgoraAndJoin()
        } else {
            Toast.makeText(this, "Microphone permission is required to make calls", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        finishCall()
    }
}