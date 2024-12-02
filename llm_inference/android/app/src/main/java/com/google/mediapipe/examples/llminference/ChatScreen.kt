package com.google.mediapipe.examples.llminference

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun ChatRoute(
    setting: Int,
    chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.getFactory(LocalContext.current.applicationContext)
    )
) {
    if(setting == 1){
        Log.d("ChatRoute", "Normal")
        val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
        val textInputEnabled by chatViewModel.isTextInputEnabled.collectAsStateWithLifecycle()
        ChatScreen(
            uiState,
            textInputEnabled
        ) { message ->
            chatViewModel.sendMessage(message)
        }
    }
    else{
        val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
        val textInputEnabled by chatViewModel.isTextInputEnabled.collectAsStateWithLifecycle()
        ChatScreen(
            uiState,
            textInputEnabled
        ) {
//            chatViewModel.sendDefault()
        }
        Log.d("ChatRoute", "Default")
    }
}

@Composable
fun ChatScreen(
    uiState: UiState,
    textInputEnabled: Boolean = true,
    onSendMessage: (String) -> Unit
) {
    var userMessage by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(
                items = uiState.messages,
                key = { it.id } // Add key for better performance
            ) { chat ->
                ChatItem(chat)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = {
                    Text(stringResource(R.string.chat_label))
                },
                modifier = Modifier.weight(0.85f),
                enabled = textInputEnabled
            )

            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.15f),
                enabled = textInputEnabled
            ) {
                Icon(
                    Icons.AutoMirrored.Default.Send,
                    contentDescription = stringResource(R.string.action_send)
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    chatMessage: ChatMessage
) {
    val backgroundColor = if (chatMessage.isFromUser) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Column(
        horizontalAlignment = if (chatMessage.isFromUser) Alignment.End else Alignment.Start,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = if (chatMessage.isFromUser)
                stringResource(R.string.user_label)
            else
                stringResource(R.string.model_label),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row {
            BoxWithConstraints {
                Card(
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = RoundedCornerShape(
                        topStart = if (chatMessage.isFromUser) 20.dp else 4.dp,
                        topEnd = if (chatMessage.isFromUser) 4.dp else 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    modifier = Modifier.widthIn(0.dp, maxWidth * 0.9f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (chatMessage.isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text(text = chatMessage.message)

                            // Only show metrics for model responses
                            if (!chatMessage.isFromUser && chatMessage.executionTime > 0) {
                                Spacer(modifier = Modifier.size(8.dp))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                                Spacer(modifier = Modifier.size(8.dp))

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Performance Metrics",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Time: ${chatMessage.executionTime}ms",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Threads: ${chatMessage.threadCount}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Energy Consumed : ${chatMessage.estimatedEnergy} mAh",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}