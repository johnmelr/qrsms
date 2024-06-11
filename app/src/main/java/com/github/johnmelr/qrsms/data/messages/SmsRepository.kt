package com.github.johnmelr.qrsms.data.messages

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony.Sms
import android.provider.Telephony.Sms.Conversations
import android.provider.Telephony.Sms.Inbox
import android.util.Log
import com.github.johnmelr.qrsms.crypto.Base64Utils
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.data.contacts.ContactsRepository
import com.github.johnmelr.qrsms.data.messages.QrsmsProjection.smsColumnsProjection
import com.github.johnmelr.qrsms.data.utils.Flags
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "SmsRepository"

class SmsRepository(
    private val dispatcherIO: CoroutineDispatcher,
    private val contentResolver: ContentResolver,
    private val contactsRepository: ContactsRepository
) {
    suspend fun getInboxOfThreadId(
        smsMessages: MutableList<QrsmsMessage>,
        threadIdOfConversation: String
    ) {
        withContext(dispatcherIO) {
            Log.d("SmsRepository", "Retrieving conversation for id: $threadIdOfConversation")

            val inboxCursor: Cursor = contentResolver.query(
                Sms.CONTENT_URI,
                smsColumnsProjection,
                "${Sms.THREAD_ID}=${threadIdOfConversation}",
                null,
                null,
            ) ?: return@withContext

            // The Cursor is not null but empty
            if (inboxCursor.count == 0) return@withContext

            while (inboxCursor.moveToNext() && inboxCursor.count > inboxCursor.position) {
                val columnIndexMapping = mutableMapOf<String, Int>()

                for (columnName in smsColumnsProjection) {
                    val index = inboxCursor.getColumnIndex(columnName)
                    columnIndexMapping[columnName] = index
                }

                val id: String = inboxCursor.getString(columnIndexMapping[Sms._ID]!!)
                val threadId: String = threadIdOfConversation
                val person: String? = inboxCursor.getString(columnIndexMapping[Sms.PERSON]!!)
                val address: String = inboxCursor.getString(columnIndexMapping[Sms.ADDRESS]!!)
                val body: String = inboxCursor.getString(columnIndexMapping[Sms.BODY]!!)
                val date: Long = inboxCursor.getLong(columnIndexMapping[Sms.DATE]!!)
                val dateSent: Long = inboxCursor.getLong(columnIndexMapping[Sms.DATE_SENT]!!)
                val seen: Int = inboxCursor.getInt(columnIndexMapping[Sms.SEEN]!!)
                val read: Int = inboxCursor.getInt(columnIndexMapping[Sms.READ]!!)
                val subscriptionId: Long = inboxCursor
                    .getLong(columnIndexMapping[Sms.SUBSCRIPTION_ID]!!)
                val replyPathPresent: Boolean = inboxCursor
                    .getString(columnIndexMapping[Sms.REPLY_PATH_PRESENT]!!).toBoolean()
                val messageType: Int = inboxCursor.getInt(columnIndexMapping[Sms.TYPE]!!)

                var isEncrypted = false
                val start = Base64Utils.stringToBase64(Flags.CIPHER_START)
                val end = Base64Utils.stringToBase64(Flags.CIPHER_END)

                if (body.startsWith(start) && body.endsWith(end)) {
                    isEncrypted = true
                }

                val qrsmsMessage = QrsmsMessage(
                    id = id,
                    threadId = threadId,
                    person = person,
                    address = address,
                    snippet = body,
                    body = body,
                    date = date,
                    dateSent = dateSent,
                    seen = seen,
                    read = read,
                    subscriptionId = subscriptionId,
                    replyPathPresent = replyPathPresent,
                    type = messageType,
                    messageCount = null,
                    isEncrypted = isEncrypted
                )

                smsMessages.add(qrsmsMessage)
            }

            inboxCursor.close()
        }
    }

    suspend fun getInboxOfAddress(
        smsMessages: MutableList<QrsmsMessage>,
        addressOfConversation: String
    ) {
        withContext(dispatcherIO) {
            // Find a message in the content provider where the address is the address of the
            // selected contact so that the thread_id can be obtained
            val threadIdCursor: Cursor = contentResolver.query(
                Sms.CONTENT_URI,
                smsColumnsProjection,
                "instr(${Sms.ADDRESS}, ${addressOfConversation.substringAfter("+63")}) > 0",
                null,
                "${Sms.DATE} DESC LIMIT 1"
            ) ?: return@withContext

            // Return if cursor is empty
            if (threadIdCursor.count == 0) return@withContext

            threadIdCursor.moveToFirst()

            var contactThreadId = ""

            contactThreadId = threadIdCursor.getString(
                threadIdCursor.getColumnIndexOrThrow(Sms.THREAD_ID)
            )


            threadIdCursor.close()

            getInboxOfThreadId(
                smsMessages,
                contactThreadId
            )
        }
    }

    suspend fun getConversations(
        inboxList: MutableList<QrsmsMessage>
    ) {
        Log.v("SmsRepository", "Retrieving Conversations.")

        withContext(dispatcherIO) {
            val conversationsCursor: Cursor = contentResolver.query(
                Conversations.CONTENT_URI,
                QrsmsProjection.conversationsColumnProjection,
                null,
                null,
                "date DESC"
            ) ?: return@withContext

            val conversationsCount = conversationsCursor.count ?: 0
            if (conversationsCount == 0) {
                conversationsCursor.close()
                return@withContext
            }

             val conversationIndexMapping = mutableMapOf<String, Int>()

            for (columnName in QrsmsProjection.conversationsColumnProjection) {
                val index = conversationsCursor.getColumnIndex(columnName)
                conversationIndexMapping[columnName] = index
            }

            while (conversationsCursor.moveToNext()
                && conversationsCursor.position < conversationsCount
            ) {
                val threadId = conversationsCursor.getString(
                    conversationIndexMapping[Conversations.THREAD_ID]!!
                )
                val snippet = conversationsCursor.getString(
                    conversationIndexMapping[Conversations.SNIPPET]!!
                )
                val messageCount = conversationsCursor.getString(
                    conversationIndexMapping[Conversations.MESSAGE_COUNT]!!
                )

                val inboxCursor: Cursor = contentResolver.query(
                    Sms.CONTENT_URI,
                    smsColumnsProjection,
                    "${Sms.THREAD_ID}=?",
                    arrayOf(threadId),
                    "date DESC LIMIT 1"
                ) ?: return@withContext


                if (inboxCursor.count == 0) {
                    inboxCursor.close()
                    return@withContext
                }

                val inboxIndexMapping = mutableMapOf<String, Int>()

                for (columnName in smsColumnsProjection) {
                    val index = inboxCursor.getColumnIndex(columnName)
                    inboxIndexMapping[columnName] = index
                }

                inboxCursor.moveToFirst()

                val id: String = inboxCursor.getString(inboxIndexMapping[Sms._ID]!!)
                var person: String? = inboxCursor.getString(inboxIndexMapping[Sms.PERSON]!!)
                val address: String = inboxCursor.getString(inboxIndexMapping[Sms.ADDRESS]!!)
                val body: String = inboxCursor.getString(inboxIndexMapping[Sms.BODY]!!)
                val date: Long = inboxCursor.getLong(inboxIndexMapping[Sms.DATE]!!)
                val dateSent: Long = inboxCursor.getLong(inboxIndexMapping[Sms.DATE_SENT]!!)
                val seen: Int = inboxCursor.getInt(inboxIndexMapping[Sms.SEEN]!!)
                val read: Int = inboxCursor.getInt(inboxIndexMapping[Sms.READ]!!)
                val subscriptionId: Long = inboxCursor
                    .getLong(inboxIndexMapping[Sms.SUBSCRIPTION_ID]!!)
                val replyPathPresent: Boolean = inboxCursor
                    .getString(inboxIndexMapping[Sms.REPLY_PATH_PRESENT]!!).toBoolean()
                val messageType: Int = inboxCursor.getInt(inboxIndexMapping[Sms.TYPE]!!)

                inboxCursor.close()

                val contact: ContactDetails? = contactsRepository
                    .getContactDetailsOfAddress(address)

                if (contact != null) person = contact.displayName

                val messageSnippet = QrsmsMessage(
                    id,
                    threadId,
                    person,
                    address,
                    snippet,
                    body,
                    date,
                    dateSent,
                    seen,
                    read,
                    subscriptionId,
                    replyPathPresent,
                    messageType,
                    messageCount.toInt(),
                    isEncrypted = false,
                )

                inboxList.add(messageSnippet)
            }
            conversationsCursor.close()
        }
    }

    suspend fun getMessageByUri(uri: Uri): QrsmsMessage? {
        val messageCursor: Cursor = contentResolver.query(
            uri,
            smsColumnsProjection,
            null,
            null,
            null
        ) ?: return null

        if (messageCursor.count == 0) {
            messageCursor.close()
            return null
        }

        val columnMapping: MutableMap<String, Int> = emptyMap<String, Int>().toMutableMap()

        for (columnName in smsColumnsProjection) {
            columnMapping[columnName] = messageCursor.getColumnIndex(columnName)
        }

        messageCursor.moveToFirst()

        val id = messageCursor.getString(columnMapping[Sms._ID]!!)
        val threadId = messageCursor.getString(columnMapping[Sms.THREAD_ID]!!)
        var person = messageCursor.getString(columnMapping[Sms.PERSON]!!)
        val address = messageCursor.getString(columnMapping[Sms.ADDRESS]!!)
        val body = messageCursor.getString(columnMapping[Sms.BODY]!!)
        val date = messageCursor.getLong(columnMapping[Sms.DATE]!!)
        val dateSent = messageCursor.getLong(columnMapping[Sms.DATE_SENT]!!)
        val seen = messageCursor.getInt(columnMapping[Sms.SEEN]!!)
        val read = messageCursor.getInt(columnMapping[Sms.READ]!!)
        val subscriptionId = messageCursor.getLong(columnMapping[Sms.SUBSCRIPTION_ID]!!)
        val replyPathPresent =
            messageCursor.getString(columnMapping[Sms.REPLY_PATH_PRESENT]!!).toBoolean()
        val messageType = messageCursor.getInt(columnMapping[Sms.TYPE]!!)

        messageCursor.close()

        withContext(dispatcherIO) {
            val contact: ContactDetails? = contactsRepository.getContactDetailsOfAddress(address)

            if (contact != null) person = contact.displayName
        }

        return QrsmsMessage(
            id,
            threadId,
            person,
            address,
            body,
            body,
            date,
            dateSent,
            seen,
            read,
            subscriptionId,
            replyPathPresent,
            messageType,
            0,
            isEncrypted = false,
        )
    }
}
