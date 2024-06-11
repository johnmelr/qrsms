package com.github.johnmelr.qrsms.ui.model

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony.Sms
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage
import com.github.johnmelr.qrsms.data.messages.SmsRepository
import com.github.johnmelr.qrsms.ui.state.QrsmsInboxUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Class: InboxViewModel
 * ViewModel class for the InboxScreen to retrieve and store SMS messages to
 * the Inbox Ui state holder.
 *
 * Parameters:
 *  application: Application - the single application instance
 *
 * Implements AndroidViewModel() instead of the regular ViewModel to allow passing
 * of the application context. Through the application class, it is possible to
 * get an instance of a contentResolver needed to query/retrieve stored SMS messages.
 **/
@HiltViewModel
class InboxViewModel @Inject constructor(
    @ApplicationContext application: Context,
    private val smsRepository: SmsRepository,
): ViewModel() {
    private val contentResolver = application.contentResolver

    private var _qrsmsInboxUiState = MutableStateFlow(QrsmsInboxUiState())
    val qrsmsInboxUiState: StateFlow<QrsmsInboxUiState> = _qrsmsInboxUiState.asStateFlow()

    var selectedThread by mutableStateOf("")
        private set
    var selectedAddress by mutableStateOf("")
        private set

    private val _messageList = MutableStateFlow<List<QrsmsMessage>>(emptyList())
    val messageList = _messageList.asStateFlow()

    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private val smsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)

            viewModelScope.launch(Dispatchers.IO) {
                val smsRawUri = "${Sms.CONTENT_URI}/raw"

                if (uri == null || uri.toString().contains(smsRawUri)) return@launch

                val newMessage: QrsmsMessage = getMessageByUri(uri) ?: return@launch

                _messageList.value = listOf(newMessage) + _messageList.value.filter {
                    it.threadId != newMessage.threadId
                }.toList()
            }
//             getInboxFromSmsProvider()
        }
    }

    init {
        viewModelScope.launch {
            getInboxFromSmsProvider()
        }
        contentResolver.registerContentObserver(Sms.CONTENT_URI, true, smsObserver)
    }

    private suspend fun getMessageByUri(uri: Uri): QrsmsMessage? {
        return smsRepository.getMessageByUri(uri)
    }
    /**
     * Function: getInboxFromSmsProvider
     *  Retrieves a list of SMS message from android's content provider through the
     *  content resolver.
     *
     *  This retrieval is done in two parts. The first part is through
     *  using the `Telephony.Sms.Conversation` class which retrieves only the most recent
     *  message from each thread/conversation.
     *
     *  Since the Conversation class only provides
     *  three columns -- namely the thread_id, message_count, and snippet, the following
     *  step is responsible for obtaining other information related to the SMS. This step
     *  uses the Sms class since the most recent message can either be stored in the Inbox,
     *  or Sent.
     *
     **/
    private fun getInboxFromSmsProvider() {
        Log.v("InboxViewModel", "Retrieving Inbox.")

        viewModelScope.launch {
            val inboxList: MutableList<QrsmsMessage> = mutableListOf()

            smsRepository.getConversations(
                inboxList,
            )

            _messageList.value = inboxList
        }
    }

    fun setSelectedConversation(threadId: String, address: String) {
        selectedThread = threadId
        selectedAddress = address
    }
}