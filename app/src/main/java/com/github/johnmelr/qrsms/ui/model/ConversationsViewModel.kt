package com.github.johnmelr.qrsms.ui.model

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.crypto.KeyStoreManager
import com.github.johnmelr.qrsms.crypto.QrsmsCipher
import com.github.johnmelr.qrsms.data.contacts.ContactsRepository
import com.github.johnmelr.qrsms.ui.state.ConversationsUiState
import com.github.johnmelr.qrsms.data.messages.SmsRepository
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage
import com.github.johnmelr.qrsms.data.messages.SmsProviderObserver
import com.github.johnmelr.qrsms.data.messages.SmsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ConversationsViewModel"

enum class MessageType {
    REGULAR_SMS,
    ENCRYPTED_SMS
}

class ConversationsViewModel(
    application: Application = Application()
) : AndroidViewModel(application) {

    // Ui state holders
    private var _conversationsUiState = MutableStateFlow(ConversationsUiState())
    val conversationsUiState: StateFlow<ConversationsUiState> = _conversationsUiState

    // Get an instance of the content resolver
    private val contentResolver: ContentResolver = getApplication<Application>().contentResolver

    private val uri: Uri = Uri.Builder()
        .path(Telephony.Sms.CONTENT_URI.toString())
        .appendQueryParameter(Telephony.Sms.THREAD_ID, "169")
        .build()

    private val observer: MutableLiveData<List<QrsmsMessage>> = SmsProviderObserver(
        getApplication<Application>().applicationContext,
        Telephony.Sms.CONTENT_URI,
        SmsRepository()
    )

    fun getObserver(): MutableLiveData<List<QrsmsMessage>> {
        return observer
    }

    private val smsSender: SmsSender = SmsSender(
        getApplication<Application>().applicationContext,
    )

    var messageInput: String by mutableStateOf("")
        private set

    // Should be in UI Level State Holder
    var showDialog: Boolean by mutableStateOf(false)
        private set

    // UI Level State Holder
    var messageType: MessageType by mutableStateOf(MessageType.REGULAR_SMS)
        private set

    /**
     * Update message string from the Text Input
     *
     * @param newMessage the value of the updated string
     */
    fun updateMessageInput(newMessage: String) {
        messageInput = newMessage
    }

    /**
     * Toggle showDialog value
     */
    fun toggleDialog() {
        showDialog = !showDialog
    }

    fun updateMessageType(newType: MessageType) {
        messageType = newType
    }

    /**
     * Function: getInboxFromRepository
     *  - Retrieves all messages from the InboxRepository given the selected
     *      message's threadId. The list of messages is then stored in the state holder.
     *
     *  Parameters:
     *  * selectedThreadId: String
     *      - threadId of the selected conversation.
     */
    fun getInboxOfThreadId(selectedThreadId: String) {
        Log.v(TAG, "Retrieving Inbox for thread id: $selectedThreadId")

        viewModelScope.launch {
            val smsMessages: MutableList<QrsmsMessage> = mutableListOf()
            SmsRepository().getInboxOfThreadId(
                contentResolver,
                smsMessages,
                selectedThreadId
            )

            _conversationsUiState.update { currentState ->
                currentState.copy(
                    smsMessages = smsMessages
                )
            }
        }
    }

    /**
     * Retrieves messages from the InboxRepository given the address of the selected contact
     *
     * @param address address of the conversation
     */
    fun getInboxOfAddress(address: String) {
        Log.d(TAG, "Retrieving Inbox for address: $address")

        viewModelScope.launch {
            val smsMessage: MutableList<QrsmsMessage> = mutableListOf()
            SmsRepository().getInboxOfAddress(
                contentResolver,
                smsMessage,
                address
            )

            _conversationsUiState.update { currentState ->
                currentState.copy(
                    smsMessages = smsMessage
                )
            }
        }
    }

    /**
     * Retrieves contact information of the given address
     *
     * @param address address of the other party
     */
    fun getContactDetailsOfAddress(address: String) {
        viewModelScope.launch {
            val contact = ContactsRepository.getContactDetailsOfAddress(
                contentResolver,
                address
            )

            _conversationsUiState.update { currentState ->
                currentState.copy(
                    contact = contact
                )
            }
        }
    }

    /**
     * Check the keystore if a key exist for the given address
     */
    fun checkSecretKeyExist(address: String) {
        viewModelScope.launch {
            val phoneNumber = PhoneNumberUtils.formatNumberToE164(address, "PH")
            if (phoneNumber.isNullOrEmpty()) {
                return@launch
            }

            val keyExist = KeyStoreManager.doesSecretKeyExist(phoneNumber)

            _conversationsUiState.update { currentState ->
                currentState.copy(
                    hasExistingKey = keyExist
                )
            }
        }
    }

    /**
     * Send SMS message to the given address
     *
     * @param address the address (Phone Number) where the SMS should be sent.
     * @param text the content of the sms message
     */
    fun sendSmsMessage(
        address: String,
        text: String,
        messageType: MessageType = MessageType.REGULAR_SMS,
    ) {
        if (text == "") {
            Toast.makeText(getApplication(), "Message is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        messageInput = ""

        if (messageType == MessageType.REGULAR_SMS)
            smsSender.sendMessage(address, text)
        else
            smsSender.sendEncryptedMessage(address, text)
    }

    fun readEncryptedMessage(
        address: String,
        cipherText: String,
    ): String {
        val cipher = QrsmsCipher(address)
        return cipher.decrypt(cipherText.substring(4, cipherText.length - 4))
    }

    fun resetConversationUiState() {
        _conversationsUiState.update { currentState ->
            currentState.copy(
                selectedInboxByThreadId = "",
                selectedContactByAddress = "",
                smsMessages = mutableListOf<QrsmsMessage>()
            )
        }
    }
}