import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.assignment1.DataClass.Story
class StoryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "stories.db", null, 2) {   // bumped version

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE stories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT,
                username TEXT,
                profile_image TEXT,
                story_image TEXT,
                status TEXT,
                timestamp INTEGER   -- FIXED (SQLite safe)
            )
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS stories")
        onCreate(db)
    }

    // Save stories EXCEPT placeholder tile
    fun insertStories(list: List<Story>) {
        val db = writableDatabase
        db.execSQL("DELETE FROM stories")

        val stmt = db.compileStatement(
            "INSERT INTO stories(user_id,username,profile_image,story_image,status,timestamp) VALUES (?,?,?,?,?,?)"
        )

        for (s in list) {

            if (s.storyImage == "PLACEHOLDER_ADD_STORY") continue // ← don't save

            stmt.bindString(1, s.userId)
            stmt.bindString(2, s.username)
            stmt.bindString(3, s.profileImage)
            stmt.bindString(4, s.storyImage)
            stmt.bindString(5, s.status)
            stmt.bindLong(6, s.timestamp)
            stmt.executeInsert()
        }
    }

    fun getAllStories(): MutableList<Story> {
        val list = mutableListOf<Story>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT user_id,username,profile_image,story_image,status,timestamp FROM stories ORDER BY timestamp DESC",
            null
        )

        while (cursor.moveToNext()) {
            list.add(
                Story(
                    userId = cursor.getString(0),
                    username = cursor.getString(1),
                    profileImage = cursor.getString(2),
                    storyImage = cursor.getString(3),
                    status = cursor.getString(4),
                    timestamp = cursor.getLong(5)
                )
            )
        }

        cursor.close()
        return list
    }
}
