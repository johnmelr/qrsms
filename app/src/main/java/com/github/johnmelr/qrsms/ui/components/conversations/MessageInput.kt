package com.github.johnmelr.qrsms.ui.components.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.johnmelr.qrsms.R
import com.github.johnmelr.qrsms.ui.model.MessageType

@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    messageType: MessageType,
    canSendEncryptedMessage: Boolean,
    messageInput: () -> String,
    onSendMessage: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
    toggleDialog: () -> Unit = {},
) {
    val roundedCornerShape = RoundedCornerShape(32.dp)
    val interactionSource = remember { MutableInteractionSource() }

    val input = messageInput()

    Row (
        modifier = modifier
            .padding(
                horizontal = 8.dp,
                vertical = 24.dp
            )
            .clip(roundedCornerShape)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ){
        if (canSendEncryptedMessage) {
            val imageVector = if (messageType == MessageType.REGULAR_SMS)

                ImageVector.vectorResource(R.drawable.sms_24dp_fill0_wght400_grad0_opsz24)
            else
                ImageVector.vectorResource(R.drawable.lock_24dp_fill0_wght400_grad0_opsz24)

            val circleShape = RoundedCornerShape(32.dp)

            Box(modifier = Modifier.
            clickable { toggleDialog() }){
                Icon(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(32.dp),
                    imageVector = imageVector,
                    contentDescription = "Switch message type",
                    tint = MaterialTheme.colorScheme.secondaryContainer
                )
                Icon(
                    imageVector = ImageVector.vectorResource
                        (R.drawable.swap_vertical_circle_24dp_fill1_wght400_grad0_opsz24),
                    modifier = Modifier
                        .zIndex(1f)
                        .offset(x = 20.dp, y = 16.dp)
                        .size(20.dp)
                        .border(4.dp, MaterialTheme.colorScheme.secondaryContainer, circleShape)
                        .background(MaterialTheme.colorScheme.onBackground, circleShape)
                        .clip(circleShape),
                    tint = MaterialTheme.colorScheme.secondaryContainer,
                    contentDescription = "Switch Icon"
                )
            }
        }
        Row (
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, roundedCornerShape)
                .clip(roundedCornerShape)
                .weight(1f)
                .defaultMinSize()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                modifier = Modifier,
                value = input,
                onValueChange = onValueChange
            ) { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = input,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        focusedTextColor = MaterialTheme.colorScheme.background,
                        unfocusedTextColor = MaterialTheme.colorScheme.background,
                        disabledTextColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.outline,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = true,
                    singleLine = false,
                    innerTextField = innerTextField,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    placeholder = { Text("Message") }
                )
            }

        }
        IconButton(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(32.dp)
                )
                .align(Alignment.CenterVertically)
                .size(40.dp),
            onClick = { onSendMessage() }
        ) {
            val imageVector = if (messageType == MessageType.REGULAR_SMS) Icons.Outlined.Send
            else ImageVector.vectorResource(R.drawable.encrypted_message_send_24dp)
            Icon(
                modifier = Modifier
                    .padding(4.dp)
                    .size(16.dp),
                imageVector = imageVector,
                contentDescription = "Send sms message.",
                tint = MaterialTheme.colorScheme.secondaryContainer
            )
        }
    }
}

@Preview
@Composable
fun MessageInputPreview() {
    MessageInput(
        canSendEncryptedMessage = true,
        messageType = MessageType.REGULAR_SMS,
        messageInput = { "Hello World" },
    )
}