package com.example.assignment1.DataClass

import Comment

// NOTE: You must have a 'Comment' data class defined in the same package or imported.

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val postImageBase64: String = "",  // Holds the Image URL/Link
    val caption: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),

    // --- FIXES ADDED BELOW ---

    // FIX 1: Required by PostAdapter for like button logic
    val isLiked: Boolean = false,

    // FIX 2: Required by PostAdapter for comment RecyclerView, fixes toMutableList error
    val comments: List<Comment> = emptyList()
)