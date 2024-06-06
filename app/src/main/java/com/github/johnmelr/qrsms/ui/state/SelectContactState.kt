package com.github.johnmelr.qrsms.ui.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.johnmelr.qrsms.data.contacts.ContactDetails

data class SelectContactState(
    val contactList:MutableList<ContactDetails> = mutableListOf(),
    val selectedContact: ContactDetails? = null
) 
