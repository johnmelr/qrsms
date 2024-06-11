package com.github.johnmelr.qrsms.ui.model

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.crypto.KeyStoreManager
import com.github.johnmelr.qrsms.crypto.QrsmsCipher
import com.github.johnmelr.qrsms.data.contacts.ContactsRepository
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage
import com.github.johnmelr.qrsms.data.messages.SmsRepository
import com.github.johnmelr.qrsms.data.messages.SmsSender
import com.github.johnmelr.qrsms.ui.state.ConversationsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ConversationsViewModel"

enum class MessageType {
    REGULAR_SMS,
    ENCRYPTED_SMS
}

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val smsRepository: SmsRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel() {
    // Ui state holders
    private var _conversationsUiState = MutableStateFlow(ConversationsUiState())
    val conversationsUiState: StateFlow<ConversationsUiState> = _conversationsUiState

    // Get an instance of the content resolver
    private val contentResolver: ContentResolver = application.contentResolver

    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private val conversationObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)

            val smsRawUri = "${Telephony.Sms.CONTENT_URI}/raw"

            if (uri == null || uri.toString().contains(smsRawUri)) return

            val address = _conversationsUiState.value.selectedContactByAddress
            val threadId = _conversationsUiState.value.selectedInboxByThreadId

            if (threadId.isBlank())
               getInboxOfAddress(address)
            else
               getInboxOfThreadId(threadId)
        }
    }

    private val _messageList = MutableStateFlow<List<QrsmsMessage>>(emptyList())
    val messageList = _messageList.asStateFlow()

    private val smsSender: SmsSender = SmsSender(application)
    var messageInput: String by mutableStateOf("")
        private set

    // Should be in UI Level State Holder
    var showDialog: Boolean by mutableStateOf(false)
        private set

    // UI Level State Holder
    var messageType: MessageType by mutableStateOf(MessageType.REGULAR_SMS)
        private set

    init {
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            conversationObserver)
    }

    override fun onCleared() {
        super.onCleared()
        contentResolver.unregisterContentObserver(conversationObserver)
    }

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

    fun setThreadId(threadId: String) {
        _conversationsUiState.update {
            it.copy(selectedInboxByThreadId = threadId)
        }
    }

    fun setAddress(address: String) {
        _conversationsUiState.update {
            it.copy(selectedContactByAddress = address)
        }
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
            smsRepository.getInboxOfThreadId(
                smsMessages,
                selectedThreadId
            )

            _messageList.value = smsMessages.toList()
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
            smsRepository.getInboxOfAddress(
                smsMessage,
                address
            )

            _messageList.value = smsMessage.toList()
        }
    }

    /**
     * Retrieves contact information of the given address
     *
     * @param address address of the other party
     */
    fun getContactDetailsOfAddress(address: String) {
        viewModelScope.launch {
            val contact = contactsRepository.getContactDetailsOfAddress(
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

            val keyExist = KeyStoreManager().doesSecretKeyExist(phoneNumber)

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
            Toast.makeText(application, "Message is empty!", Toast.LENGTH_SHORT).show()
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
            )
        }
        _messageList.value = emptyList()
    }
}