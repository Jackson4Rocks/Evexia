package com.jackson4rocks.evexia.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.flowAiHistoryDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "flow_ai_history")

private const val MAX_CONVERSATIONS = 20
private const val MAX_MESSAGES = 120
private const val HISTORY_VERSION = 1

data class FlowAiConversationSummary(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messageCount: Int,
)

data class FlowAiConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<FlowAiMessage>,
)

data class FlowAiHistorySnapshot(
    val activeId: String?,
    val conversations: List<FlowAiConversation>,
)

/**
 * A small local store for Evexia AI conversations. It deliberately uses a
 * separate DataStore so chat history never touches the rest of Evexia's
 * preferences or the Room database.
 */
class FlowAiHistoryStore(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(): FlowAiHistorySnapshot {
        val raw = try {
            appContext.flowAiHistoryDataStore.data.first()[Keys.history]
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            null
        }.orEmpty()
        if (raw.isBlank()) return FlowAiHistorySnapshot(null, emptyList())
        return try {
            decode(raw)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            FlowAiHistorySnapshot(null, emptyList())
        }
    }

    suspend fun save(snapshot: FlowAiHistorySnapshot) {
        appContext.flowAiHistoryDataStore.edit { preferences ->
            preferences[Keys.history] = encode(snapshot)
        }
    }

    private object Keys {
        val history = stringPreferencesKey("snapshot")
    }

    private fun encode(snapshot: FlowAiHistorySnapshot): String {
        val conversations = JSONArray()
        snapshot.conversations
            .filter { it.messages.isNotEmpty() }
            .sortedByDescending { it.updatedAt }
            .take(MAX_CONVERSATIONS)
            .forEach { conversation ->
                val messages = JSONArray()
                conversation.messages
                    .takeLast(MAX_MESSAGES)
                    .forEach { message ->
                        messages.put(
                            JSONObject()
                                .put("id", message.id)
                                .put("role", message.role.wire)
                                .put("text", message.text)
                                .put("error", message.error),
                        )
                    }
                conversations.put(
                    JSONObject()
                        .put("id", conversation.id)
                        .put("title", conversation.title)
                        .put("createdAt", conversation.createdAt)
                        .put("updatedAt", conversation.updatedAt)
                        .put("messages", messages),
                )
            }
        return JSONObject()
            .put("version", HISTORY_VERSION)
            .put("activeId", snapshot.activeId ?: JSONObject.NULL)
            .put("conversations", conversations)
            .toString()
    }

    private fun decode(raw: String): FlowAiHistorySnapshot {
        val root = JSONObject(raw)
        if (root.optInt("version", 1) > HISTORY_VERSION) {
            return FlowAiHistorySnapshot(null, emptyList())
        }
        val activeId = root.optString("activeId").takeIf { it.isNotBlank() && it != "null" }
        val array = root.optJSONArray("conversations") ?: JSONArray()
        val conversations = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val messageArray = item.optJSONArray("messages") ?: JSONArray()
                val messages = buildList {
                    for (messageIndex in 0 until messageArray.length()) {
                        val message = messageArray.optJSONObject(messageIndex) ?: continue
                        val text = message.optString("text")
                        if (text.isBlank()) continue
                        add(
                            FlowAiMessage(
                                id = message.optLong("id", messageIndex.toLong()),
                                role = if (message.optString("role") == FlowAiRole.ASSISTANT.wire) {
                                    FlowAiRole.ASSISTANT
                                } else {
                                    FlowAiRole.USER
                                },
                                text = text,
                                error = message.optBoolean("error", false),
                            ),
                        )
                    }
                }.takeLast(MAX_MESSAGES)
                val createdAt = item.optLong("createdAt", item.optLong("updatedAt", 0L))
                val updatedAt = item.optLong("updatedAt", createdAt)
                add(
                    FlowAiConversation(
                        id = id,
                        title = item.optString("title").ifBlank { "Saved chat" },
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        messages = messages,
                    ),
                )
            }
        }
            .sortedByDescending { it.updatedAt }
            .take(MAX_CONVERSATIONS)
        return FlowAiHistorySnapshot(activeId, conversations)
    }
}
