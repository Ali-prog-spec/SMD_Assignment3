package com.example.assignment1.helper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.assignment1.DataClass.ChatIttem

class UserDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "users.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT,
                username TEXT,
                dp_url TEXT
            )
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun saveUsers(list: List<ChatIttem>) {
        val db = writableDatabase
        db.execSQL("DELETE FROM users")

        val stmt = db.compileStatement(
            "INSERT INTO users(user_id,username,dp_url) VALUES(?,?,?)"
        )

        for (u in list) {
            stmt.bindString(1, u.userId)
            stmt.bindString(2, u.username)
            stmt.bindString(3, u.imageUrl) // absolute path
            stmt.executeInsert()
        }
    }

    fun getUsers(): MutableList<ChatIttem> {
        val list = mutableListOf<ChatIttem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users", null)

        while (cursor.moveToNext()) {
            list.add(
                ChatIttem(
                    userId = cursor.getString(1),
                    username = cursor.getString(2),
                    message = "Start Chat",
                    imageUrl = cursor.getString(3),
                    time = ""
                )
            )
        }
        cursor.close()
        return list
    }
}
