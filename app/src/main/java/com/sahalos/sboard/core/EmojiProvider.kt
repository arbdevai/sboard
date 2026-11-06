package com.sahalos.sboard.core

import android.content.Context
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
    val tags: String
)

object EmojiProvider {
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
                        list.add(EmojiItem(emoji, category, tags))
                    }
                }
            } catch (e: Exception) {
                // Minimal safety fallback seed
                list.add(EmojiItem("😀", EmojiCategory.SMILEYS, "senyum smile gembira happy"))
                list.add(EmojiItem("😂", EmojiCategory.SMILEYS, "tertawa lol haha joy"))
                list.add(EmojiItem("👍", EmojiCategory.PEOPLE, "jempol like ok"))
                list.add(EmojiItem("❤️", EmojiCategory.SYMBOLS, "hati love heart"))
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

        return load(context).filter { item ->
            item.emoji == trimmed || item.tags.contains(trimmed)
        }.map { it.emoji }.distinct()
    }
}
