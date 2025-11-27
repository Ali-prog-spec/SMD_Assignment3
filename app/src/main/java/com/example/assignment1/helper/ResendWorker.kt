package com.example.assignment1.helper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.assignment1.ChatMessage

class ResendWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = MessageDatabaseHelper(applicationContext)
        val list = db.getUnsentMessages()

        for (msg in list) {
            val success = sendToServer(msg)
            if (success) {
                db.updateSentFlag(msg.messageId!!)
            }
        }
        return Result.success()
    }

    private fun sendToServer(msg: ChatMessage): Boolean {
        // YOUR API call
        return true
    }
}
