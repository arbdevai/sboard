package com.sahalos.sboard.core

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

enum class EmojiCategory(val label: String, val icon: String) {
    RECENT("Terakhir", "🕒"),
    SMILEYS("Senyum", "😊"),
    PEOPLE("Orang", "🧑"),
    ANIMALS("Hewan", "🐶"),
    FOOD("Makanan", "🍔"),
    ACTIVITIES("Aktivitas", "⚽"),
    TRAVEL("Tempat", "🚗"),
    OBJECTS("Objek", "💡"),
    SYMBOLS("Simbol", "🔣"),
    FLAGS("Bendera", "🏁")
}

data class EmojiItem(
    val emoji: String,
    val category: EmojiCategory,
    val tokens: List<String>
)

object EmojiProvider {
    private const val TAG = "EmojiProvider"

    @Volatile
    private var cachedEmojis: List<EmojiItem>? = null
    private val categoryMap = mutableMapOf<EmojiCategory, List<String>>()

    fun load(context: Context): List<EmojiItem> {
        val existing = cachedEmojis
        if (existing != null) return existing

        synchronized(this) {
            cachedEmojis?.let { return it }

            val list = mutableListOf<EmojiItem>()
            try {
                context.assets.open("emojis_catalog.json").use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
                    val jsonString = reader.readText()
                    val array = JSONArray(jsonString)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val emoji = obj.getString("emoji")
                        val catStr = obj.getString("category")
                        val tags = obj.optString("tags", "")
                        val category = try {
                            EmojiCategory.valueOf(catStr)
                        } catch (e: Exception) {
                            EmojiCategory.SMILEYS
                        }
                        val tokens = tags.split(" ").filter { it.isNotEmpty() }
                        list.add(EmojiItem(emoji, category, tokens))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading emojis_catalog.json: ${e.message}")
                // Fallback seed
                list.add(EmojiItem("😀", EmojiCategory.SMILEYS, listOf("senyum", "smile", "gembira", "happy")))
                list.add(EmojiItem("😂", EmojiCategory.SMILEYS, listOf("tertawa", "lol", "haha", "joy", "ngakak")))
                list.add(EmojiItem("👍", EmojiCategory.PEOPLE, listOf("jempol", "like", "ok", "bagus")))
                list.add(EmojiItem("❤️", EmojiCategory.SYMBOLS, listOf("hati", "love", "heart", "cinta")))
            }

            for (cat in EmojiCategory.values()) {
                if (cat != EmojiCategory.RECENT) {
                    categoryMap[cat] = list.filter { it.category == cat }.map { it.emoji }
                }
            }

            cachedEmojis = list
            return list
        }
    }

    fun getEmojisForCategory(context: Context, category: EmojiCategory): List<String> {
        load(context)
        return categoryMap[category] ?: emptyList()
    }

    fun search(context: Context, query: String): List<String> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val items = load(context)
        val results = mutableListOf<String>()

        // 1. Exact match / token prefix match (Unicode CLDR tokenized)
        for (item in items) {
            if (item.emoji == trimmed || item.tokens.any { it.startsWith(trimmed) }) {
                results.add(item.emoji)
            }
        }

        return results.distinct()
    }
}
