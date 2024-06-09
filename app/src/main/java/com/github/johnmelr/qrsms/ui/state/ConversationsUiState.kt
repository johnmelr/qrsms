package com.github.johnmelr.qrsms.ui.state

import androidx.compose.runtime.mutableStateOf
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage

data class ConversationsUiState(
    var selectedInboxByThreadId: String = "",
    var selectedContactByAddress: String = "",

    val hasExistingKey: Boolean = false,

    val contact: ContactDetails? = null,
)
