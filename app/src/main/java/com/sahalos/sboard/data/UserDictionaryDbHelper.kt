package com.sahalos.sboard.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

data class WordRecord(
    val word: String,
    val frequency: Int,
    val lastUsed: Long
)

class UserDictionaryDbHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "UserDictionaryDb"
        private const val DATABASE_NAME = "sboard_user_dict.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_WORDS = "user_words"
        const val COL_WORD = "word"
        const val COL_FREQUENCY = "frequency"
        const val COL_LAST_USED = "last_used"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_WORDS (
                $COL_WORD TEXT PRIMARY KEY COLLATE NOCASE,
                $COL_FREQUENCY INTEGER NOT NULL DEFAULT 1,
                $COL_LAST_USED INTEGER NOT NULL
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_word_prefix ON $TABLE_WORDS($COL_WORD);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_word_rank ON $TABLE_WORDS($COL_FREQUENCY DESC, $COL_LAST_USED DESC);")

        seedInitialVocabulary(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade migrations
    }

    private fun seedInitialVocabulary(db: SQLiteDatabase) {
        try {
            context.assets.open("id_seed_vocabulary.txt").use { stream ->
                val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
                val now = System.currentTimeMillis()
                db.beginTransaction()
                try {
                    val stmt = db.compileStatement(
                        "INSERT OR IGNORE INTO $TABLE_WORDS ($COL_WORD, $COL_FREQUENCY, $COL_LAST_USED) VALUES (?, ?, ?)"
                    )
                    var line: String?
                    // Seed words have base frequency = 5, allowing personal typed words to naturally overtake them
                    while (reader.readLine().also { line = it } != null) {
                        val word = line?.trim()?.lowercase() ?: continue
                        if (word.length >= 2) {
                            stmt.bindString(1, word)
                            stmt.bindLong(2, 5L)
                            stmt.bindLong(3, now)
                            stmt.executeInsert()
                        }
                    }
                    db.setTransactionSuccessful()
                    Log.i(TAG, "Successfully seeded Indonesian seed vocabulary into UserDictionary database")
                } finally {
                    db.endTransaction()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding initial vocabulary: ${e.message}")
        }
    }

    fun learnWord(word: String, boost: Int = 1) {
        val clean = word.trim().lowercase()
        if (clean.length < 2 || clean.any { !it.isLetter() }) return

        val db = writableDatabase
        val now = System.currentTimeMillis()
        try {
            db.execSQL(
                """
                INSERT INTO $TABLE_WORDS ($COL_WORD, $COL_FREQUENCY, $COL_LAST_USED)
                VALUES (?, ?, ?)
                ON CONFLICT($COL_WORD) DO UPDATE SET
                    $COL_FREQUENCY = $COL_FREQUENCY + ?,
                    $COL_LAST_USED = ?;
                """.trimIndent(),
                arrayOf(clean, boost, now, boost, now)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error learning word '$clean': ${e.message}")
        }
    }

    fun loadAllActiveWords(limit: Int = 5000): List<WordRecord> {
        val result = mutableListOf<WordRecord>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_WORD, $COL_FREQUENCY, $COL_LAST_USED FROM $TABLE_WORDS ORDER BY $COL_FREQUENCY DESC, $COL_LAST_USED DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use { c ->
            val colWord = c.getColumnIndexOrThrow(COL_WORD)
            val colFreq = c.getColumnIndexOrThrow(COL_FREQUENCY)
            val colLast = c.getColumnIndexOrThrow(COL_LAST_USED)
            while (c.moveToNext()) {
                result.add(
                    WordRecord(
                        word = c.getString(colWord),
                        frequency = c.getInt(colFreq),
                        lastUsed = c.getLong(colLast)
                    )
                )
            }
        }
        return result
    }

    fun searchPrefix(prefix: String, limit: Int = 5): List<WordRecord> {
        val clean = prefix.trim().lowercase()
        if (clean.isEmpty()) return emptyList()

        val result = mutableListOf<WordRecord>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_WORD, $COL_FREQUENCY, $COL_LAST_USED FROM $TABLE_WORDS WHERE $COL_WORD LIKE ? ORDER BY $COL_FREQUENCY DESC, $COL_LAST_USED DESC LIMIT ?",
            arrayOf("$clean%", limit.toString())
        )
        cursor.use { c ->
            val colWord = c.getColumnIndexOrThrow(COL_WORD)
            val colFreq = c.getColumnIndexOrThrow(COL_FREQUENCY)
            val colLast = c.getColumnIndexOrThrow(COL_LAST_USED)
            while (c.moveToNext()) {
                result.add(
                    WordRecord(
                        word = c.getString(colWord),
                        frequency = c.getInt(colFreq),
                        lastUsed = c.getLong(colLast)
                    )
                )
            }
        }
        return result
    }
}
