/*
package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.lang.reflect.Method

class PowerConsumptionCalculator(private val context: Context) {

    private val TAG = "PowerConsumptionCalculator"

    // Estimate power consumption during a task
    fun estimatePowerConsumption(executionTimeMs: Long): Double {
        try {
            // Load the PowerProfile class via reflection
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfileConstructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfileInstance = powerProfileConstructor.newInstance(context)

            // Get the getAveragePower method
            val getAveragePowerMethod: Method = powerProfileClass.getMethod("getAveragePower", String::class.java)

            // Fetch CPU power consumption (mAh per second)
            val cpuActivePower = getAveragePowerMethod.invoke(powerProfileInstance, "cpu.active") as Double
            Log.d(TAG, "CPU Active Power: $cpuActivePower mAh/sec")

            // Fetch power consumption for other components (optional)
            val screenPower = getAveragePowerMethod.invoke(powerProfileInstance, "screen.on") as Double
            Log.d(TAG, "Screen On Power: $screenPower mAh/sec")

            val wifiPower = getAveragePowerMethod.invoke(powerProfileInstance, "wifi.active") as Double
            Log.d(TAG, "WiFi Active Power: $wifiPower mAh/sec")

            // Convert execution time to seconds
            val executionTimeSec = executionTimeMs / 1000.0

            // Calculate total energy consumed (assuming only CPU usage here)
            val estimatedEnergy = cpuActivePower * executionTimeSec
            Log.d(TAG, "Estimated Energy Consumption: $estimatedEnergy mAh")

            return estimatedEnergy
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating power consumption", e)
        }
        return -1.0 // Return -1.0 on error
    }
}

class ChatViewModel(
    private val inferenceModel: InferenceModel,
    private val context: Context
) : ViewModel() {

    private val TAG = "ChatViewModel"

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    private val logFile: File by lazy {
        File(context.filesDir, "history.json")
    }

    private fun writeToJsonLog(logEntry: JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting JSON log write operation")
                val jsonArray = if (logFile.exists()) {
                    Log.d(TAG, "Reading existing history.json file")
                    val content = logFile.readText()
                    if (content.isNotEmpty()) {
                        JSONArray(content)
                    } else {
                        Log.d(TAG, "Empty file found, creating new JSONArray")
                        JSONArray()
                    }
                } else {
                    Log.d(TAG, "No existing file found, creating new JSONArray")
                    JSONArray()
                }

                // Only write if we have a valid response
                if (logEntry.has("response") && !logEntry.getString("response").isNullOrEmpty()) {
                    Log.d(TAG, "Adding new entry to JSON array")
                    // Log the complete response content
                    Log.d(TAG, "Response content: ${logEntry.getString("response")}")
                    jsonArray.put(logEntry)
                    logFile.writeText(jsonArray.toString(2))
                    Log.d(TAG, "Successfully wrote to history.json")
                } else if (logEntry.has("error")) {
                    Log.d(TAG, "Adding error entry to JSON array")
                    Log.d(TAG, "Error content: ${logEntry.getString("error")}")
                    jsonArray.put(logEntry)
                    logFile.writeText(jsonArray.toString(2))
                    Log.d(TAG, "Successfully wrote error to history.json")
                } else {
                    Log.w(TAG, "Skipping empty response entry")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to JSON log", e)
                _uiState.value.addMessage("Error writing to log: ${e.localizedMessage}", "System")
            }
        }
    }

    fun sendMessage(userMessage: String) {
        Log.d(TAG, "Processing new message: $userMessage")
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.addMessage(userMessage, USER_PREFIX)
            var currentMessageId: String? = _uiState.value.createLoadingMessage()
            setInputEnabled(false)

            try {
                val startTime = System.nanoTime()
                val initialThreadCount = Thread.activeCount()
                val fullPrompt = _uiState.value.fullPrompt
                var finalResponse = StringBuilder()
                val calculator = PowerConsumptionCalculator(context)
                val startTimeMs = System.currentTimeMillis()

                Log.d(TAG, "Starting inference with thread count: $initialThreadCount")
                inferenceModel.generateResponseAsync(fullPrompt)
                inferenceModel.partialResults
                    .collectIndexed { index, (partialResult, done) ->
                        currentMessageId?.let {
                            if (index == 0) {
                                Log.d(TAG, "Received first partial result: $partialResult")
                                _uiState.value.appendMessage(it, partialResult, false)
                                finalResponse.append(partialResult)
                            } else {
                                finalResponse.append(partialResult)
                                if (done) {
                                    val endTime = System.nanoTime()
                                    val executionTime = (endTime - startTime) / 1_000_000 // Convert to ms
                                    val finalThreadCount = Thread.activeCount()
                                    val completeResponse = finalResponse.toString()
                                    val endTimeMs = System.currentTimeMillis()
                                    val executionTimeMs = endTimeMs - startTimeMs
                                    val estimatedEnergy = calculator.estimatePowerConsumption(executionTimeMs)

                                    Log.d(TAG, """
                                        Inference completed:
                                        - Execution time: $executionTime ms
                                        - Initial threads: $initialThreadCount
                                        - Final threads: $finalThreadCount
                                        - Complete response: $completeResponse
                                        - Estimated energy : $estimatedEnergy mAh
                                    """.trimIndent())

                                    // Create JSON log entry with complete response
                                    val logEntry = JSONObject().apply {
                                        put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                                        put("query", userMessage)
                                        put("response", completeResponse)
                                        put("executionTime", executionTime)
                                        put("initialThreadCount", initialThreadCount)
                                        put("finalThreadCount", finalThreadCount)
                                        put("threadDelta", finalThreadCount - initialThreadCount)
                                    }
                                    writeToJsonLog(logEntry)

                                    _uiState.value.appendMessageWithMetrics(
                                        it,
                                        "",  // Don't append any more text, just update metrics
                                        executionTime,
                                        finalThreadCount,
                                        estimatedEnergy,
                                        done
                                    )
                                } else {
                                    Log.v(TAG, "Received partial result: $partialResult")
                                    _uiState.value.appendMessage(it, partialResult, done)
                                }
                            }
                            if (done) {
                                Log.d(TAG, "Message processing completed")
                                currentMessageId = null
                                setInputEnabled(true)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
                _uiState.value.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
                setInputEnabled(true)

                // Log error in JSON
                val logEntry = JSONObject().apply {
                    put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    put("query", userMessage)
                    put("error", e.localizedMessage ?: "Unknown Error")
                }
                writeToJsonLog(logEntry)
            }
        }
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                return ChatViewModel(inferenceModel, context) as T
            }
        }
    }
}
*/


package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.lang.reflect.Method

// Keep the PowerConsumptionCalculator unchanged
class PowerConsumptionCalculator(private val context: Context) {
    private val TAG = "PowerConsumptionCalculator"

    fun estimatePowerConsumption(executionTimeMs: Long): Double {
        try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfileConstructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfileInstance = powerProfileConstructor.newInstance(context)

            val getAveragePowerMethod: Method = powerProfileClass.getMethod("getAveragePower", String::class.java)

            val cpuActivePower = getAveragePowerMethod.invoke(powerProfileInstance, "cpu.active") as Double
            Log.d(TAG, "CPU Active Power: $cpuActivePower mAh/sec")

            val screenPower = getAveragePowerMethod.invoke(powerProfileInstance, "screen.on") as Double
            Log.d(TAG, "Screen On Power: $screenPower mAh/sec")

            val wifiPower = getAveragePowerMethod.invoke(powerProfileInstance, "wifi.active") as Double
            Log.d(TAG, "WiFi Active Power: $wifiPower mAh/sec")

            val executionTimeSec = executionTimeMs / 1000.0
            val estimatedEnergy = cpuActivePower * executionTimeSec
            Log.d(TAG, "Estimated Energy Consumption: $estimatedEnergy mAh")

            return estimatedEnergy
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating power consumption", e)
        }
        return -1.0
    }
}

class ChatViewModel(
    private val inferenceModel: InferenceModel,
    private val context: Context
) : ViewModel() {

    private val TAG = "ChatViewModel"

    private val currentUser: String
        get() {
            val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            return sharedPreferences.getString("current_username", "default") ?: "default"
        }


    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    // Get user-specific log file
    private fun getUserLogFile(): File {
        val username = currentUser
        val sanitizedUsername = username.replace("[^a-zA-Z0-9]".toRegex(), "_")
        return File(context.filesDir, "${sanitizedUsername}.json")
    }

    private fun writeToJsonLog(logEntry: JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val username = currentUser
                Log.d(TAG, "Starting JSON log write operation for user: $username")
                val userLogFile = getUserLogFile()

                val jsonArray = if (userLogFile.exists()) {
                    Log.d(TAG, "Reading existing ${userLogFile.name} file")
                    val content = userLogFile.readText()
                    if (content.isNotEmpty()) {
                        JSONArray(content)
                    } else {
                        Log.d(TAG, "Empty file found, creating new JSONArray")
                        JSONArray()
                    }
                } else {
                    Log.d(TAG, "No existing file found, creating new JSONArray")
                    JSONArray()
                }

                if (logEntry.has("response") && !logEntry.getString("response").isNullOrEmpty()) {
                    Log.d(TAG, "Adding new entry to JSON array for user: $username")
                    logEntry.put("username", username)
                    Log.d(TAG, "Response content: ${logEntry.getString("response")}")
                    jsonArray.put(logEntry)
                    userLogFile.writeText(jsonArray.toString(2))
                    Log.d(TAG, "Successfully wrote to ${userLogFile.name}")
                } else if (logEntry.has("error")) {
                    Log.d(TAG, "Adding error entry to JSON array for user: $username")
                    logEntry.put("username", username)
                    Log.d(TAG, "Error content: ${logEntry.getString("error")}")
                    jsonArray.put(logEntry)
                    userLogFile.writeText(jsonArray.toString(2))
                    Log.d(TAG, "Successfully wrote error to ${userLogFile.name}")
                } else {
                    Log.w(TAG, "Skipping empty response entry")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to JSON log", e)
                _uiState.value.addMessage("Error writing to log: ${e.localizedMessage}", "System")
            }
        }
    }

    // Function to get chat history for current user
    fun loadUserHistory(): JSONArray {
        val userLogFile = getUserLogFile()
        return if (userLogFile.exists()) {
            try {
                JSONArray(userLogFile.readText())
            } catch (e: Exception) {
                Log.e(TAG, "Error reading user history", e)
                JSONArray()
            }
        } else {
            JSONArray()
        }
    }

    fun sendMessage(userMessage: String) {
        val username = currentUser
        Log.d(TAG, "Processing new message for user $username: $userMessage")
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.addMessage(userMessage, USER_PREFIX)
            var currentMessageId: String? = _uiState.value.createLoadingMessage()
            setInputEnabled(false)

            try {
                val startTime = System.nanoTime()
                val initialThreadCount = Thread.activeCount()
                val fullPrompt = _uiState.value.fullPrompt
                var finalResponse = StringBuilder()
                val calculator = PowerConsumptionCalculator(context)
                val startTimeMs = System.currentTimeMillis()

                Log.d(TAG, "Starting inference with thread count: $initialThreadCount")
                inferenceModel.generateResponseAsync(fullPrompt)
                inferenceModel.partialResults
                    .collectIndexed { index, (partialResult, done) ->
                        currentMessageId?.let {
                            if (index == 0) {
                                Log.d(TAG, "Received first partial result: $partialResult")
                                _uiState.value.appendMessage(it, partialResult, false)
                                finalResponse.append(partialResult)
                            } else {
                                finalResponse.append(partialResult)
                                if (done) {
                                    val endTime = System.nanoTime()
                                    val executionTime = (endTime - startTime) / 1_000_000
                                    val finalThreadCount = Thread.activeCount()
                                    val completeResponse = finalResponse.toString()
                                    val endTimeMs = System.currentTimeMillis()
                                    val executionTimeMs = endTimeMs - startTimeMs
                                    val estimatedEnergy = calculator.estimatePowerConsumption(executionTimeMs)

                                    Log.d(TAG, """
                                        Inference completed for user $username:
                                        - Execution time: $executionTime ms
                                        - Initial threads: $initialThreadCount
                                        - Final threads: $finalThreadCount
                                        - Complete response: $completeResponse
                                        - Estimated energy: $estimatedEnergy mAh
                                    """.trimIndent())

                                    val logEntry = JSONObject().apply {
                                        put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                                        put("query", userMessage)
                                        put("response", completeResponse)
                                        put("executionTime", executionTime)
                                        put("initialThreadCount", initialThreadCount)
                                        put("finalThreadCount", finalThreadCount)
                                        put("threadDelta", finalThreadCount - initialThreadCount)
                                        put("estimatedEnergy", estimatedEnergy)
                                    }
                                    writeToJsonLog(logEntry)

                                    _uiState.value.appendMessageWithMetrics(
                                        it,
                                        "",
                                        executionTime,
                                        finalThreadCount,
                                        estimatedEnergy,
                                        done
                                    )
                                } else {
                                    Log.v(TAG, "Received partial result: $partialResult")
                                    _uiState.value.appendMessage(it, partialResult, done)
                                }
                            }
                            if (done) {
                                Log.d(TAG, "Message processing completed for user: $username")
                                currentMessageId = null
                                setInputEnabled(true)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message for user $username", e)
                _uiState.value.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
                setInputEnabled(true)

                val logEntry = JSONObject().apply {
                    put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    put("query", userMessage)
                    put("error", e.localizedMessage ?: "Unknown Error")
                }
                writeToJsonLog(logEntry)
            }
        }
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                return ChatViewModel(inferenceModel, context) as T
            }
        }
    }
}