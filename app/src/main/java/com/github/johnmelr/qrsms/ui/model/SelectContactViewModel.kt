package com.github.johnmelr.qrsms.ui.model

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.data.contacts.ContactsRepository
import com.github.johnmelr.qrsms.ui.state.SelectContactState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectContactViewModel(application: Application): AndroidViewModel(application) {
    private val contentResolver:ContentResolver = getApplication<Application>().contentResolver

    private val _selectContactState = MutableStateFlow(SelectContactState())
    val selectContactState = _selectContactState.asStateFlow()

    /**
     * Retrieves a list of all contacts in the device via the Contact Provider.
     */
    fun getContactListFromRepository() {
        val contactList = mutableListOf<ContactDetails>()

        viewModelScope.launch {
            ContactsRepository.getAllContacts(
                contactList,
                contentResolver
            )

            _selectContactState.update { currentState ->
                currentState.copy(
                    contactList = contactList
                )
            }
        }
    }

    /**
     * Updates the value of selectedContact in the UI state holder.
     *
     * @param selectedContact the contact selected from the contact list
     */
    fun setSelectedContactDetails(selectedContact: ContactDetails) {
        _selectContactState.update { currentState ->
            currentState.copy(
                selectedContact = selectedContact
            )
        }
    }


}
