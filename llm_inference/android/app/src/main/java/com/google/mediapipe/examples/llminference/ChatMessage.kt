/*
package com.google.mediapipe.examples.llminference

import java.util.UUID


data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val rawMessage: String = "",
    val author: String,
    val isLoading: Boolean = false
) {
    val isFromUser: Boolean
        get() = author == USER_PREFIX
    val message: String
        get() = rawMessage.trim()
}
*/

package com.google.mediapipe.examples.llminference

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val rawMessage: String = "",
    val author: String,
    val isLoading: Boolean = false,
    // Add performance metrics fields
    val executionTime: Long = 0,    // in milliseconds
    val threadCount: Int = 0,
    val estimatedEnergy: Double = 0.0
) {
    val isFromUser: Boolean
        get() = author == USER_PREFIX
    val message: String
        get() = rawMessage.trim()
}