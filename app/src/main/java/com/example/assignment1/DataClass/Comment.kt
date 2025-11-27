data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    var userDpBase64: String = "" // 🆕 added for profile picture
)
