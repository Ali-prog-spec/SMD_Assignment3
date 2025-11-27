package com.example.assignment1.helper

import Comment
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.assignment1.DataClass.Post
import org.json.JSONArray
import org.json.JSONObject

class PostDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "offline_posts.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE posts (
                post_id TEXT PRIMARY KEY,
                user_id TEXT,
                username TEXT,
                userProfileImage TEXT,
                postImageBase64 TEXT,
                caption TEXT,
                likeCount INTEGER,
                commentCount INTEGER,
                timestamp LONG,
                isLiked INTEGER,
                comments TEXT
            )
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS posts")
        onCreate(db)
    }

    // ------------------------------------------------------------------------
    // SAVE POSTS LIST INTO DATABASE
    // ------------------------------------------------------------------------
    fun savePosts(posts: List<Post>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (p in posts) {
                val jsonComments = JSONArray()
                p.comments.forEach { c ->
                    val obj = JSONObject()
                    obj.put("comment_id", c.commentId)
                    obj.put("user_id", c.userId)
                    obj.put("username", c.username)
                    obj.put("text", c.text)
                    obj.put("timestamp", c.timestamp)
                    obj.put("userDpBase64", c.userDpBase64)
                    jsonComments.put(obj)
                }

                val cv = ContentValues()
                cv.put("post_id", p.postId)
                cv.put("user_id", p.userId)
                cv.put("username", p.username)
                cv.put("userProfileImage", p.userProfileImage)
                cv.put("postImageBase64", p.postImageBase64)
                cv.put("caption", p.caption)
                cv.put("likeCount", p.likeCount)
                cv.put("commentCount", p.commentCount)
                cv.put("timestamp", p.timestamp)
                cv.put("isLiked", if (p.isLiked) 1 else 0)
                cv.put("comments", jsonComments.toString())

                db.insertWithOnConflict("posts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ------------------------------------------------------------------------
    // LOAD POSTS FROM LOCAL DATABASE
    // ------------------------------------------------------------------------
    fun loadOfflinePosts(): List<Post> {
        val db = readableDatabase
        val list = mutableListOf<Post>()

        val cursor = db.rawQuery("SELECT * FROM posts ORDER BY timestamp DESC", null)
        if (cursor.moveToFirst()) {
            do {
                try {
                    // -------------------------------
                    // LOG EVERYTHING FOR DEBUGGING
                    // -------------------------------
                    Log.d("POST_DB", "Loading post_id = " +
                            cursor.getString(cursor.getColumnIndexOrThrow("post_id")))

                    Log.d("POST_DB", "Raw comments = " +
                            cursor.getString(cursor.getColumnIndexOrThrow("comments")))

                    Log.d("POST_DB", "Raw timestamp = " +
                            cursor.getString(cursor.getColumnIndexOrThrow("timestamp")))

                    // SAFELY PARSE COMMENTS
                    val commentsJsonString =
                        cursor.getString(cursor.getColumnIndexOrThrow("comments")) ?: "[]"

                    val jsonComments = JSONArray(commentsJsonString)
                    val commentsList = mutableListOf<Comment>()

                    for (i in 0 until jsonComments.length()) {
                        val obj = jsonComments.getJSONObject(i)
                        commentsList.add(
                            Comment(
                                commentId = obj.getString("comment_id"),
                                userId = obj.getString("user_id"),
                                username = obj.getString("username"),
                                text = obj.getString("text"),
                                timestamp = obj.getLong("timestamp"),
                                userDpBase64 = obj.getString("userDpBase64")
                            )
                        )
                    }

                    list.add(
                        Post(
                            postId = cursor.getString(cursor.getColumnIndexOrThrow("post_id")),
                            userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
                            username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                            userProfileImage = cursor.getString(cursor.getColumnIndexOrThrow("userProfileImage")),
                            postImageBase64 = cursor.getString(cursor.getColumnIndexOrThrow("postImageBase64")),
                            caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")),
                            likeCount = cursor.getInt(cursor.getColumnIndexOrThrow("likeCount")),
                            commentCount = cursor.getInt(cursor.getColumnIndexOrThrow("commentCount")),
                            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                            isLiked = cursor.getInt(cursor.getColumnIndexOrThrow("isLiked")) == 1,
                            comments = commentsList
                        )
                    )

                } catch (e: Exception) {
                    Log.e("POST_DB_ERROR", "Error while loading post", e)
                }

            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

}
