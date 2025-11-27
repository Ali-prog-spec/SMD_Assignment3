package com.example.assignment1.DataClass

data class Story(
    val userId: String,
    val username: String,
    val profileImage: String,
    val storyImage: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)
