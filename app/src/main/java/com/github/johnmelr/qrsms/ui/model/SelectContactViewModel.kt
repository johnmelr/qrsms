package com.github.johnmelr.qrsms.ui.model

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.data.contacts.ContactsRepository
import com.github.johnmelr.qrsms.ui.state.SelectContactState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectContactViewModel @Inject constructor(
    @ApplicationContext application: Context,
    private val contactsRepository: ContactsRepository,
): ViewModel() {
    private val _selectContactState = MutableStateFlow(SelectContactState())
    val selectContactState = _selectContactState.asStateFlow()

    var searchInput = mutableStateOf("")

    init {
        viewModelScope.launch {
            getContactListFromRepository()
        }
    }

    fun updateSearchInput(input: String) {
        searchInput.value = input

        viewModelScope.launch(Dispatchers.IO) {
            if (input.isBlank()) {
                getContactListFromRepository()
            } else {
                searchForContact(input)
            }
        }
    }



    /**
     * Retrieves a list of all contacts in the device via the Contact Provider.
     */
    fun getContactListFromRepository() {
        val contactList = mutableListOf<ContactDetails>()

        viewModelScope.launch {
            contactsRepository.getAllContacts(
                contactList,
            )

            _selectContactState.update { currentState ->
                currentState.copy(
                    contactList = contactList
                )
            }
        }
    }

    fun searchForContact(queryString: String) {
        val contactList = mutableListOf<ContactDetails>()

        viewModelScope.launch {
            contactsRepository.searchContact(queryString, contactList)

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

    /**
     * Update value of selectedContact given the address
     *
     * @param address the normalized phone number of contact
     */
    fun setSelectedContactDetailsFromAddress(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var contactDetails = contactsRepository.getContactDetailsOfAddress(address)

            if (contactDetails == null) {
                contactDetails = ContactDetails(
                    id = "null",
                    displayName = null,
                    photoThumbUriString = null,
                    phoneNumber = null,
                    normalizedPhoneNumber = address
                )
            }

            _selectContactState.update { currentState ->
                currentState.copy(
                    selectedContact = contactDetails
                )
            }
        }
    }
}
