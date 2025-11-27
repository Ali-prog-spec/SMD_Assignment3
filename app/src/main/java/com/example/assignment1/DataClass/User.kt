package com.example.assignment1.DataClass

data class User(
    val uid:String="",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dob: String = "",
    val email: String = "",
    val dp64: String = "",
    val password: String = "",
    val followers: Map<String, Boolean> = emptyMap(),
    val following: Map<String, Boolean> = emptyMap()
)
