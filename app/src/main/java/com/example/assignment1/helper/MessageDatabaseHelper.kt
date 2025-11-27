package com.example.assignment1.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.assignment1.ChatMessage

class MessageDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "messages.db", null, 2) {   // BUMPED VERSION TO 2

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message_id TEXT UNIQUE,
                sender_id TEXT,
                receiver_id TEXT,
                message TEXT,
                image_path TEXT,
                post_id TEXT,
                timestamp LONG,
                is_sent INTEGER DEFAULT 0,
                is_edited INTEGER DEFAULT 0,
                is_deleted INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        Log.d("DB_HELPER", "Database created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // ADD MISSING COLUMN WITHOUT DROPPING TABLE
            db.execSQL("ALTER TABLE messages ADD COLUMN is_sent INTEGER DEFAULT 0")
        }
        Log.d("DB_HELPER", "Database upgraded from $oldVersion to $newVersion")
    }

    /** Mark message as sent */
    fun updateSentFlag(messageId: String) {
        val cv = ContentValues().apply { put("is_sent", 1) }

        writableDatabase.update(
            "messages",
            cv,
            "message_id = ?",
            arrayOf(messageId)
        )

        Log.d("DB", "Marked as sent: $messageId")
    }



    /** Save or update a message */
    fun saveMessage(msg: ChatMessage) {
        val cv = ContentValues().apply {
            put("message_id", msg.messageId ?: "local_${System.currentTimeMillis()}")
            put("sender_id", msg.senderId)
            put("receiver_id", msg.receiverId ?: "")
            put("message", msg.message ?: "")
            put("image_path", msg.imagePath ?: "")
            put("post_id", msg.postId ?: "")
            put("timestamp", msg.timestamp)
            put("is_sent", if (msg.isSent) 1 else 0)
            put("is_edited", if (msg.isEdited) 1 else 0)
            put("is_deleted", if (msg.isDeleted) 1 else 0)
        }

        val id = writableDatabase.insertWithOnConflict(
            "messages",
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        Log.d("DB_HELPER", "Saved message=${msg.messageId} insertedId=$id")
    }

    fun saveMessages(list: List<ChatMessage>) {
        list.forEach { saveMessage(it) }
    }

    fun updateMessageStatus(messageId: String, isSent: Boolean) {
        val cv = ContentValues().apply { put("is_sent", if (isSent) 1 else 0) }

        writableDatabase.update("messages", cv, "message_id=?", arrayOf(messageId))
        Log.d("DB_HELPER", "updateMessageStatus: $messageId = $isSent")
    }

    /** Load chat messages */
    fun getMessagesForChat(userId: String): MutableList<ChatMessage> {
        val list = mutableListOf<ChatMessage>()

        val cursor = readableDatabase.rawQuery(
            """
            SELECT * FROM messages
            WHERE receiver_id=? OR sender_id=?
            ORDER BY timestamp ASC
            """.trimIndent(),
            arrayOf(userId, userId)
        )

        while (cursor.moveToNext()) {
            val msg = ChatMessage(
                messageId = cursor.getString(cursor.getColumnIndexOrThrow("message_id")),
                senderId = cursor.getString(cursor.getColumnIndexOrThrow("sender_id")),
                receiverId = cursor.getString(cursor.getColumnIndexOrThrow("receiver_id")),
                message = cursor.getString(cursor.getColumnIndexOrThrow("message")),
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path")).ifEmpty { null },
                postId = cursor.getString(cursor.getColumnIndexOrThrow("post_id")).ifEmpty { null },
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                isEdited = cursor.getInt(cursor.getColumnIndexOrThrow("is_edited")) == 1,
                isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_deleted")) == 1,
                isSent = cursor.getInt(cursor.getColumnIndexOrThrow("is_sent")) == 1
            )
            list.add(msg)
        }

        cursor.close()
        return list
    }

    /** Get unsent messages */
    fun getUnsentMessages(): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM messages WHERE is_sent=0 ORDER BY timestamp ASC",
            null
        )

        while (cursor.moveToNext()) {
            val msg = ChatMessage(
                messageId = cursor.getString(cursor.getColumnIndexOrThrow("message_id")),
                senderId = cursor.getString(cursor.getColumnIndexOrThrow("sender_id")),
                receiverId = cursor.getString(cursor.getColumnIndexOrThrow("receiver_id")),
                message = cursor.getString(cursor.getColumnIndexOrThrow("message")),
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path")).ifEmpty { null },
                postId = cursor.getString(cursor.getColumnIndexOrThrow("post_id")).ifEmpty { null },
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                isEdited = cursor.getInt(cursor.getColumnIndexOrThrow("is_edited")) == 1,
                isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_deleted")) == 1,
                isSent = false
            )
            list.add(msg)
        }
        cursor.close()
        return list
    }

    /** Delete chat history */
    fun deleteMessagesForChat(userId: String) {
        writableDatabase.delete(
            "messages",
            "receiver_id=? OR sender_id=?",
            arrayOf(userId, userId)
        )
    }
}
