package com.example.assignment1.UI

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import com.example.assignment1.R
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class Vcall_page : AppCompatActivity() {

    private var agoraEngine: RtcEngine? = null
    private val APP_ID = "32c73f93ca5b455792c129934e6aa241"
    // NOTE: Make sure this token is fresh / valid. Tokens expire — for testing you can generate a new one.
    private var TOKEN = "007eJxTYEh84bJQZNrJCx4z6i2U6q9rMm9f9muqSp7afWYmv5cLktcoMBgbJZsbp1kaJyeaJpmYmppbGiUbGllaGpukmiUmGpkY1t1Ty2wIZGQIXlDJwsgAgSA+C0NJanEJAwMACvseVA=="
    private var CHANNEL_NAME = "test"
    private val TAG = "video call"

    private var localUid = 0

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camGranted = permissions[Manifest.permission.CAMERA] == true
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (camGranted && micGranted) {
            initAgora()
        } else {
            Toast.makeText(this, "Camera & Microphone permissions are required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vcall_page)

        Log.d(TAG, "Attempting to join channel: $CHANNEL_NAME")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_end_call).setOnClickListener { endCall() }

        requestPermissionsLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ))
    }



    //end of onCreate
    private fun initAgora() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = this@Vcall_page
                mAppId = APP_ID
                mEventHandler = rtcEventHandler
            }
            agoraEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            Log.e(TAG, "RtcEngine.create error: ${e.message}", e)
            Toast.makeText(this, "Agora init error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Explicitly enable local media
        agoraEngine?.enableVideo()
        agoraEngine?.enableLocalVideo(true)
        agoraEngine?.enableAudio()

        agoraEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        agoraEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

        agoraEngine?.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            )
        )

        setupLocalVideo()
        joinChannel()
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Joined channel: $channel, uid: $uid")
            localUid = uid
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "Remote user joined: $uid")
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "Remote user offline: $uid, reason: $reason")
            runOnUiThread { removeRemoteVideo() }
        }

        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            Log.d(TAG, "First remote video decoded: uid=$uid ${width}x$height")
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora Error: $err")
        }
    }

    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)
        container.removeAllViews()

        val localView = SurfaceView(this)
        // Put local view above remote
        localView.setZOrderMediaOverlay(true)
        localView.setZOrderOnTop(true)
        container.addView(localView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // uid 0 means local (auto)
        agoraEngine?.setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_FIT, 0))
        agoraEngine?.startPreview()
        Log.d(TAG, "Local preview started")
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        container.removeAllViews()

        val remoteView = SurfaceView(this)
        // Ensure remote is behind overlay local view
        remoteView.setZOrderMediaOverlay(false)
        remoteView.setZOrderOnTop(false)
        container.addView(remoteView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        agoraEngine?.setupRemoteVideo(VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_FIT, uid))
        Log.d(TAG, "Remote view setup for uid=$uid")
    }

    private fun removeRemoteVideo() {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        container.removeAllViews()
        Log.d(TAG, "Remote view removed")
    }

    private fun joinChannel() {
        // IMPORTANT: tokens commonly expire. If you rely on short-lived tokens generate a fresh one.
        if (TOKEN.isBlank() || TOKEN == "YOUR_TOKEN_HERE") {
            Toast.makeText(this, "Please add a valid token", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Invalid token. Cannot join.")
            return
        }

        try {
            Log.d(TAG, "Joining channel: $CHANNEL_NAME with token")
            // Passing uid 0 => let Agora assign a uid and you'll get it in onJoinChannelSuccess
            agoraEngine?.joinChannel(TOKEN, CHANNEL_NAME, null, 0)
        } catch (e: Exception) {
            Log.e(TAG, "joinChannel failed: ${e.message}", e)
            Toast.makeText(this, "Failed to join: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun leaveChannel() {
        try {
            agoraEngine?.leaveChannel()
            agoraEngine?.stopPreview()
            Log.d(TAG, "Left channel and stopped preview")
        } catch (e: Exception) {
            Log.e(TAG, "leaveChannel error: ${e.message}", e)
        }
    }

    private fun endCall() {
        leaveChannel()
        // destroy engine safely
        try {
            RtcEngine.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "RtcEngine.destroy error: ${e.message}", e)
        }
        agoraEngine = null
        // only finish when user pressed end call; onDestroy will not call endCall again
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // do safe cleanup but don't call finish() from onDestroy
        leaveChannel()
        try {
            RtcEngine.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "RtcEngine.destroy error in onDestroy: ${e.message}", e)
        }
        agoraEngine = null
    }
}