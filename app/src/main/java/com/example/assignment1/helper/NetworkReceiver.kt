package com.example.assignment1.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (isConnected(context)) {
            val workRequest = OneTimeWorkRequest.Builder(ResendWorker::class.java).build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    private fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        return caps != null
    }
}
