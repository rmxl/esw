package com.google.mediapipe.examples.llminference

import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.sync.Mutex

const val USER_PREFIX = "user"
const val MODEL_PREFIX = "model"

interface UiState {
    val messages: List<ChatMessage>
    val fullPrompt: String
    fun createLoadingMessage(): String
    fun appendMessage(id: String, text: String, done: Boolean = false)
    fun addMessage(text: String, author: String): String
    fun appendMessageWithMetrics(
        id: String,
        text: String,
        executionTime: Long,
        threadCount: Int,
        estimatedEnergy: Double,
        done: Boolean
    )
}

class ChatUiState(
    messages: List<ChatMessage> = emptyList()
) : UiState {
    private val mutex = Mutex()
    private val _messages: MutableList<ChatMessage> = messages.toMutableStateList()
    private var reversedMessages: List<ChatMessage> = emptyList()

    override val messages: List<ChatMessage>
        get() = synchronized(this) {
            if (_messages.size != reversedMessages.size) {
                reversedMessages = _messages.reversed()
            }
            reversedMessages
        }

    override val fullPrompt: String
        get() = synchronized(this) {
            _messages.joinToString(separator = "\n") { it.rawMessage }
        }

    override fun createLoadingMessage(): String {
        return synchronized(this) {
            val chatMessage = ChatMessage(author = MODEL_PREFIX, isLoading = true)
            _messages.add(chatMessage)
            chatMessage.id
        }
    }

    override fun appendMessage(id: String, text: String, done: Boolean) {
        synchronized(this) {
            val index = _messages.indexOfFirst { it.id == id }
            if (index != -1) {
                val newText = _messages[index].rawMessage + text
                _messages[index] = _messages[index].copy(
                    rawMessage = newText,
                    isLoading = !done
                )
                // Invalidate reversed cache
                reversedMessages = emptyList()
            }
        }
    }

    override fun addMessage(text: String, author: String): String {
        return synchronized(this) {
            val chatMessage = ChatMessage(
                rawMessage = text,
                author = author
            )
            _messages.add(chatMessage)
            // Invalidate reversed cache
            reversedMessages = emptyList()
            chatMessage.id
        }
    }

    override fun appendMessageWithMetrics(
        id: String,
        text: String,
        executionTime: Long,
        threadCount: Int,
        estimatedEnergy: Double,
        done: Boolean
    ) {
        synchronized(this) {
            val index = _messages.indexOfFirst { it.id == id }
            if (index != -1) {
                val newText = _messages[index].rawMessage + text
                _messages[index] = _messages[index].copy(
                    rawMessage = newText,
                    isLoading = !done,
                    executionTime = executionTime,
                    threadCount = threadCount,
                    estimatedEnergy = estimatedEnergy
                )
                // Invalidate reversed cache
                reversedMessages = emptyList()
            }
        }
    }
}