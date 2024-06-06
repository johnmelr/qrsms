package com.github.johnmelr.qrsms.ui.model

import android.app.Application
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.provider.Telephony.Sms
import android.provider.Telephony.Sms.Conversations
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.data.contacts.ContactsRepository
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage
import com.github.johnmelr.qrsms.data.messages.QrsmsProjection.conversationsColumnProjection
import com.github.johnmelr.qrsms.data.messages.QrsmsProjection.smsColumnsProjection
import com.github.johnmelr.qrsms.data.messages.SmsObserver
import com.github.johnmelr.qrsms.data.messages.SmsProviderObserver
import com.github.johnmelr.qrsms.data.messages.SmsRepository
import com.github.johnmelr.qrsms.ui.state.QrsmsInboxUiState
import dagger.hilt.android.internal.Contexts.getApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
class InboxViewModel(
    application: Application = Application(),
): AndroidViewModel(application) {
    private val contentResolver = getApplication(application).contentResolver
    private val smsRepository = SmsRepository()

    private var _qrsmsInboxUiState = MutableStateFlow(QrsmsInboxUiState())
    val qrsmsInboxUiState: StateFlow<QrsmsInboxUiState> = _qrsmsInboxUiState.asStateFlow()

    var selectedThread by mutableStateOf("")
        private set
    var selectedAddress by mutableStateOf("")
        private set

    var messageList: StateFlow<MutableList<QrsmsMessage>> = MutableStateFlow(mutableListOf())
        private set

    var smsLiveData: Flow<List<QrsmsMessage>> = SmsProviderObserver(
        getApplication<Application>().applicationContext,
        Sms.CONTENT_URI,
        smsRepository
    ).asFlow()

    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

    init {
        viewModelScope.launch {
            getInboxFromSmsProvider()
        }
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
                contentResolver,
                inboxList,
            )

            _qrsmsInboxUiState.update { currentState ->
                currentState.copy(
                    messageList = inboxList
                )
            }
        }
    }

    fun setSelectedConversation(threadId: String, address: String) {
        selectedThread = threadId
        selectedAddress = address
    }
}