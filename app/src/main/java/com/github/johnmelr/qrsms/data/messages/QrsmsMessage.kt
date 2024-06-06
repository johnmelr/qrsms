package com.github.johnmelr.qrsms.data.messages

import android.provider.Telephony

data class QrsmsMessage(
    val id: String,
    val threadId: String,
    val person: String?,
    val address: String,
    val snippet: String,
    val body: String,
    val date: Long,
    val dateSent: Long,
    val seen: Int,
    val read: Int,
    val subscriptionId: Long,
    val replyPathPresent: Boolean,
    val type: Int,
    val messageCount: Int?,

    val isEncrypted: Boolean
)

/**
 * Singleton Class Instance hold that Column Projections
 * for Telephony Related queries.
 */
object QrsmsProjection {
    val smsColumnsProjection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.DATE_SENT,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.PERSON,
        Telephony.Sms.READ,
        Telephony.Sms.SEEN,
        Telephony.Sms.THREAD_ID,
        Telephony.Sms.SUBSCRIPTION_ID,
        Telephony.Sms.REPLY_PATH_PRESENT,
        Telephony.Sms.TYPE
    )

    val conversationsColumnProjection = arrayOf(
        Telephony.Sms.Conversations.THREAD_ID,
        Telephony.Sms.Conversations.MESSAGE_COUNT,
        Telephony.Sms.Conversations.SNIPPET
    )
}