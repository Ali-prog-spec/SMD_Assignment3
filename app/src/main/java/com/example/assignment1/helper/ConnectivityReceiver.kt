package com.example.assignment1.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class ConnectivityReceiver(private val callback: (Boolean) -> Unit) :
    BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val active = cm.getNetworkCapabilities(network)
        val isOnline = active != null
        callback(isOnline)
    }
}
