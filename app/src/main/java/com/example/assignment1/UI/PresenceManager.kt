package com.example.assignment1.UI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object PresenceManager {

    private val dbUsers = FirebaseDatabase.getInstance(
        "https://socially-app-firebase-default-rtdb.firebaseio.com/"
    ).getReference("Users")

    fun setUserStatus(status: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        dbUsers.child(uid).child("status").setValue(status)
    }
}