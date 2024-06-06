package com.github.johnmelr.qrsms.ui.components.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage
import com.github.johnmelr.qrsms.utils.DateUtils

@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    message: QrsmsMessage,
    messageDecrypt: String?,
) {
    val color : Color
    val alignment: Alignment.Horizontal
    val padding: PaddingValues
    val textAlign: TextAlign

    var shouldShowDate by remember { mutableStateOf(false) }

    if (message.type == 2) {
        color = MaterialTheme.colorScheme.primaryContainer
        alignment = Alignment.End
        padding = PaddingValues(start = 48.dp)
        textAlign = TextAlign.End
    } else {
        color = MaterialTheme.colorScheme.secondaryContainer
        alignment = Alignment.Start
        padding = PaddingValues(end = 48.dp)
        textAlign = TextAlign.Start
    }

    Column (
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = if (shouldShowDate) 12.dp else 1.dp)
            .fillMaxWidth()
            .clickable {
                shouldShowDate = !shouldShowDate
            }
    ) {
        val cardShape = RoundedCornerShape(16.dp)
        Card (
            modifier
                .clip(cardShape)
                .align(alignment)
                .padding(padding),
            colors = CardDefaults.cardColors(color)
        ){
            var body = message.body

            if (message.isEncrypted) {
                body = messageDecrypt ?: " "
            }

            Text(
                modifier = modifier.padding(12.dp),
                text = body,
            )
        }
        if (shouldShowDate) {
            Column(
                modifier = modifier.fillMaxWidth(),
            ) {
                Text(
                    modifier = modifier.fillMaxWidth(),
                    text = DateUtils.parseDateFromMilliToDate(message.date),
                    fontWeight = FontWeight.ExtraLight,
                    fontSize = 12.sp,
                    textAlign = textAlign
                )
            }
        }
    }
}

/**
 * Preview composable.
 */
@Preview
@Composable
fun MessageBubblePreview() {
    val sampleMessage = QrsmsMessage(
        id = "123",
        address = "09123112312",
        body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras eget sapien quis mauris dapibus elementum. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas",
        date = "1715172000990".toLong(),
        dateSent = "1715172000990".toLong(),
        messageCount = null,
        person = null,
        read = 0,
        seen = 0,
        snippet = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras eget sapien quis mauris dapibus elementum. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas",
        threadId = "123",
        subscriptionId = "123123123123123123".toLong(),
        replyPathPresent = false,
        type = 1,
        isEncrypted = false,
    )
    MessageBubble(message = sampleMessage, messageDecrypt = "")
}

