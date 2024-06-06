package com.github.johnmelr.qrsms.ui.state

import com.github.johnmelr.qrsms.data.messages.QrsmsMessage

data class QrsmsInboxUiState(
    val messageList: MutableList<QrsmsMessage> = mutableListOf(),
    val selectedMessageThreadId: String = ""
)