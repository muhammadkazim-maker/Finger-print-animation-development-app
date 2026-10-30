package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SavedAiAnimation(
    val id: String,
    val title: String,
    val prompt: String,
    val styleBadge: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val createdAtMs: Long,
    val lottieJson: String
)

object MyCollectionStore {
    private const val FILE_NAME = "my_collection.json"

    @Synchronized
    fun loadAll(context: Context): List<SavedAiAnimation> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            val list = ArrayList<SavedAiAnimation>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedAiAnimation(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        prompt = obj.optString("prompt", ""),
                        styleBadge = obj.optString("styleBadge", "AI Custom"),
                        primaryHex = obj.optLong("primaryHex", 0xFF00E5FFL),
                        secondaryHex = obj.optLong("secondaryHex", 0xFF7C4DFFL),
                        createdAtMs = obj.optLong("createdAtMs", 0L),
                        lottieJson = obj.getString("lottieJson")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveNew(context: Context, item: SavedAiAnimation): List<SavedAiAnimation> {
        val current = loadAll(context).toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item) // newest first
        writeList(context, current)
        return current
    }

    @Synchronized
    fun delete(context: Context, id: String): List<SavedAiAnimation> {
        val current = loadAll(context).filterNot { it.id == id }
        writeList(context, current)
        return current
    }

    private fun writeList(context: Context, list: List<SavedAiAnimation>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("prompt", item.prompt)
                put("styleBadge", item.styleBadge)
                put("primaryHex", item.primaryHex)
                put("secondaryHex", item.secondaryHex)
                put("createdAtMs", item.createdAtMs)
                put("lottieJson", item.lottieJson)
            }
            arr.put(obj)
        }
        File(context.filesDir, FILE_NAME).writeText(arr.toString())
    }
}
