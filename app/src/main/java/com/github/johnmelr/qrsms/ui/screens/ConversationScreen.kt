package com.github.johnmelr.qrsms.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.johnmelr.qrsms.R
import com.github.johnmelr.qrsms.ui.components.conversations.MessageBubble
import com.github.johnmelr.qrsms.ui.components.conversations.MessageInput
import com.github.johnmelr.qrsms.ui.model.ConversationsViewModel
import com.github.johnmelr.qrsms.ui.model.MessageType

@Composable
fun ConversationScreen(
    modifier: Modifier = Modifier,
    threadIdOfConversation: String,
    addressOfConversation: String,
    conversationsViewModel: ConversationsViewModel = hiltViewModel(),
    onConversationLoad: (String) -> Unit,
    onMessageSent: () -> Unit = {},
) {
    SideEffect {
        if (threadIdOfConversation == "" ) {
            conversationsViewModel.getInboxOfAddress(addressOfConversation)
        } else {
            conversationsViewModel.getInboxOfThreadId(threadIdOfConversation)
        }
        conversationsViewModel.getContactDetailsOfAddress(addressOfConversation)
        conversationsViewModel.checkSecretKeyExist(addressOfConversation)
    }

    val showDialog = conversationsViewModel.showDialog
    val messageType = conversationsViewModel.messageType

    ConstraintLayout(
        modifier = modifier.fillMaxSize(),
    ) {

        val conversationsUiState by conversationsViewModel
            .conversationsUiState.collectAsStateWithLifecycle()
        val smsMessages by conversationsViewModel.messageList.collectAsStateWithLifecycle()
        val messageInput = conversationsViewModel.messageInput
        val contact = conversationsUiState.contact
        val canSendEncryptedMessage = conversationsUiState.hasExistingKey
        conversationsViewModel.setAddress(addressOfConversation)
        val loading = conversationsUiState.loading

        if (contact != null) {
            run { onConversationLoad(contact.displayName ?: contact.normalizedPhoneNumber) }
        } else if (smsMessages.isNotEmpty()) {
            run { onConversationLoad(smsMessages[0].address )}
        } else {
            run { onConversationLoad(addressOfConversation) }
        }

        val (lColumn, messageInputBox) = createRefs()
        val listState = rememberLazyListState()

        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    listState.animateScrollToItem(0)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(lColumn) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(messageInputBox.top)
                    height = Dimension.fillToConstraints
                },
            reverseLayout = true,
            state = listState
        ) {
            items(
                count = smsMessages.size,
                key = {
                    smsMessages[it].id
                },
                itemContent = {index ->
                    val message = remember { smsMessages[index] }
                    val messageDecrypt = if (message.isEncrypted) {
                        conversationsViewModel.readEncryptedMessage(message.address, message.body)
                    } else {
                        null
                    }
                    MessageBubble(message = message, messageDecrypt = messageDecrypt)
                }
            )
        }

        if (loading) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        MessageInput(
            messageType = messageType,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(messageInputBox) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            canSendEncryptedMessage = canSendEncryptedMessage,
            messageInput = { conversationsViewModel.messageInput },
            onSendMessage = {
                conversationsViewModel.sendSmsMessage(
                    address = addressOfConversation,
                    text = messageInput,
                    messageType = messageType,
                )

                onMessageSent()
            },
            onValueChange = { conversationsViewModel.updateMessageInput(it) },
            toggleDialog = { conversationsViewModel.toggleDialog() }
        )

        if (showDialog) {
            SwitchMessageTypeDialog(
                onCancel = {
                  conversationsViewModel.toggleDialog()
                }
            ) {
                conversationsViewModel.updateMessageType(it)
                conversationsViewModel.toggleDialog()
            }
        }
    }
}

@Composable
fun SwitchMessageTypeDialog(
    onCancel: () -> Unit,
    onSelectMessageType: (MessageType) -> Unit = { },
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column (
                modifier = Modifier
                    .fillMaxWidth(),
            ){
                Box(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                ) {
                    Text(
                        text = "Select Message Type",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Divider()
                Column(modifier = Modifier
                    .fillMaxWidth()
                ) {
                    MessageTypeOption(
                        onClick = { onSelectMessageType(MessageType.REGULAR_SMS) }) {
                        Icon(
                            painter = painterResource(
                                R.drawable.sms_24dp_fill0_wght400_grad0_opsz24
                            ),
                            contentDescription = "SMS Icon"
                        )
                        Text("Regular SMS Message")
                    }
                    MessageTypeOption(
                        onClick = { onSelectMessageType(MessageType.ENCRYPTED_SMS) }) {
                        Icon(
                            painter = painterResource(
                                R.drawable.lock_24dp_fill0_wght400_grad0_opsz24
                            ),
                            contentDescription = "SMS Icon"
                        )
                        Text("Encrypted SMS Message")
                    }
                }
                Box(modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.End))
                {
                    TextButton(onClick = onCancel) {
                        Text(text = "Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageTypeOption (
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    internalComposable: @Composable () -> Unit
) {
    Row(modifier = modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .fillMaxWidth()
        .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        internalComposable()
    }
}
