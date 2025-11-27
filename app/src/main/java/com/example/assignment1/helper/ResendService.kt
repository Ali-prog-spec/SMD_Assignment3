package com.example.assignment1.helper

import android.app.IntentService
import android.content.Intent
import com.example.assignment1.ChatMessage

class ResendService : IntentService("ResendService") {

    override fun onHandleIntent(intent: Intent?) {
        val db = MessageDatabaseHelper(this)
        val unsent = db.getUnsentMessages()  // all is_sent = 0

        for (msg in unsent) {
            val success = sendToServer(msg)
            if (success) {
                msg.isSent = true
                db.updateSentFlag(msg.messageId!!)
            }
        }
    }

    private fun sendToServer(msg: ChatMessage): Boolean {
        // TODO: your api call
        return true
    }
}
