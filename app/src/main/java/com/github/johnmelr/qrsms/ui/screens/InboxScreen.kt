package com.github.johnmelr.qrsms.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.johnmelr.qrsms.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.github.johnmelr.qrsms.QrsmsAppScreens
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage
import com.github.johnmelr.qrsms.ui.model.QrsmsAppUiState
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

        LazyColumn(
            modifier = Modifier.padding(innerPadding)
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
                modifier = Modifier.padding(
                    horizontal = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row (
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = modifier.fillMaxWidth()
                ){
                    Text(
                        text = if (message.person.isNullOrEmpty()) message.address
                            else message.person,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Text(
                        text = parseDateFromMilliToDate(message.date),
                        fontWeight = FontWeight.ExtraLight,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = message.snippet,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

