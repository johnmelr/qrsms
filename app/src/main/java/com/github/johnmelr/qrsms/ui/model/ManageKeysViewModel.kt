package com.github.johnmelr.qrsms.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.johnmelr.qrsms.crypto.KeyManager
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.data.contacts.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model class for the Key Management screen.
 */
@HiltViewModel
class ManageKeysViewModel @Inject constructor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val keyManager: KeyManager,
    private val contactsRepository: ContactsRepository
): ViewModel() {

    // Holds list of aliases
    private val _aliases = MutableStateFlow(emptyMap<String, ContactDetails?>())
    val aliases = _aliases.asStateFlow()

    init {
        getAllKeys()
    }

    /**
     * Retrieves all key pair and secret key entries from the keystore/key pair database.
     */
    private fun getAllKeys() {
        viewModelScope.launch(dispatcher) {
            // returns a list of aliases
            val aliases: List<String> = keyManager.getAllKeyPair()
            // Maps alias/contact number to the contact person
            val aliasMapping = mutableMapOf<String, ContactDetails?>()

            aliases.forEach {
                aliasMapping[it] = contactsRepository.getContactDetailsOfAddress(it)
            }

            _aliases.value = aliasMapping
        }
    }

    /**
     * Checks if the the given phone number has an existing secret key.
     *
     * @param phoneNumber the normalized phone number of the contact used as a keystore alias/
     * database id
     * @return true if there is a secret key associated with the given entry.
     * Otherwise, return false
     */
    fun phoneNumberHasSecretKey(phoneNumber: String): Boolean {
        return keyManager.doesSecretKeyExist(phoneNumber)
    }

    /**
     * Delete all key entries (keypair and secret key) associated with the given contact.
     *
     * @param keyAlias the normalized phone number of the contact used as a keystore alias/
     * database id
     */
    fun deleteKey(keyAlias: String) {
        viewModelScope.launch(dispatcher) {
            _aliases.value = _aliases.value.filterKeys {
                it != keyAlias
            }

            keyManager.deleteKeyPair(keyAlias)
            keyManager.deleteSecretKeyEntry(keyAlias)
        }
    }
}