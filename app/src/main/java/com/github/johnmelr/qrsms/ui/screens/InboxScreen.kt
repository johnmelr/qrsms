package com.github.johnmelr.qrsms.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.github.johnmelr.qrsms.ui.QrsmsAppScreens
import com.github.johnmelr.qrsms.R
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage
import com.github.johnmelr.qrsms.ui.model.InboxUiState
import com.github.johnmelr.qrsms.utils.DateUtils.parseDateFromMilliToDate

@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    onNavigateToConversations: (String) -> Unit = {},
    onSelectGenerateQr: (String) -> Unit = {},
    onSelectScanQr: (String) -> Unit = {},
    onSelectNewMessage: (String) -> Unit = {},
    onSelectMessage: (String, String) -> Unit = { _: String, _: String -> },
    messageList: List<QrsmsMessage>,
    inboxUiState: InboxUiState
) {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSelectGenerateQr(QrsmsAppScreens.GenerateForContact.name)
                            }
                        ) {
                            Text(text = "Generate QR")
                        }
                        OutlinedButton(
                            onClick = {
                                onSelectScanQr(QrsmsAppScreens.ScanQR.name)
                            }
                        ) {
                            Text(text = "Scan QR")
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        onSelectNewMessage(QrsmsAppScreens.NewConversation.name)
                    }) {
                        Icon(Icons.Filled.Create, contentDescription = null)
                    }
                },
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column {
            val listState = rememberLazyListState()

            LaunchedEffect(listState) {
                snapshotFlow { listState.firstVisibleItemIndex }
                    .collect {
                        if (it <= 1)
                            listState.animateScrollToItem(0)
                    }
            }

            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                state = listState
            ) {
                items(
                    count = messageList.size,
                    key = {
                        messageList[it].id
                    },
                    itemContent = { index ->
                        val message = remember { messageList[index] }

                        MessageCard(
                            message = message,
                            onSelectMessage = onSelectMessage,
                            onNavigateToConversations = onNavigateToConversations
                        )
                    }
                )
            }
            if (inboxUiState.loading) {
                Column (
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun MessageCard(
    modifier: Modifier = Modifier,
    message: QrsmsMessage,
    onNavigateToConversations: (String) -> Unit = {},
    onSelectMessage: (String, String) -> Unit = { _: String, _: String -> },
    ) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable {
                onNavigateToConversations(QrsmsAppScreens.Conversation.name)
                onSelectMessage(message.threadId, message.address)
            }
    ) {
        Row (
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.teal_200))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (message.person.isNullOrEmpty()) message.address
                    else message.person,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (!message.isEncrypted) message.snippet
                    else "Message is Encrypted. Open to view its content."
                    ,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = parseDateFromMilliToDate(message.date),
                    fontWeight = FontWeight.ExtraLight,
                    fontSize = 12.sp,
                )
                if (message.isEncrypted) {
                    Icon(
                        modifier = modifier.size(16.dp),
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Encrypted Message",
                        tint = Color.Green,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun MessageCardPreview(){
    val message = QrsmsMessage(
        id = "123",
        threadId = "123",
        person = "John Dela Cruz",
        address = "+63915 123 1234",
        snippet = "Hello World",
        body = "Very secret message",
        date = "1231231232312".toLong(),
        dateSent = "0".toLong(),
        seen = 0,
        read = 1,
        subscriptionId = 1,
        replyPathPresent = false,
        type = 1,
        messageCount = null,
        isEncrypted = true
    )
    MessageCard(message = message)
}

