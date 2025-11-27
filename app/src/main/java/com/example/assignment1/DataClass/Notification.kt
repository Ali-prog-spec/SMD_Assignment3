package com.example.assignment1.DataClass

// com.example.assignment1.DataClass.Notification

data class Notification(
    val id: Int = 0,
    val sender_id: Int = 0,
    val receiver_id: Int = 0, // Though not strictly needed here, good for completeness
    val type: String = "", // 'follow_request', 'like', 'comment', 'message'
    val related_id: Int? = null, // Can be null
    val sender_username: String = "",
    val sender_dp_path: String = "",
    val created_at: String = ""
)