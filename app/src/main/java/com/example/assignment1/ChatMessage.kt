package com.example.assignment1
data class ChatMessage(
    val messageId: String,
    val senderId: String,
    val message: String?,
    val imagePath: String?,
    val postId: String?,
    val timestamp: Long?,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val receiverId: String? = null,
    var isSent: Boolean = false
)
