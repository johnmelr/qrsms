package com.github.johnmelr.qrsms.data.messages

/**
 * Data class for holding conversation data. QrsmsConversation is slightly different to
 * QrsmsMessage in that this class holds the data that will be displayed in the InboxScreen.
 * It holds only the most recent message for each given contact so that it can give a preview
 * of its content in the Inbox while also holding an unread message counter.
 */
data class QrsmsConversation(
    val id: String,
    val threadId: String,
    val person: String?,
    val address: String,
    val snippet: String,
    val body: String,
    val date: Long,
    val seen: Int,
    val read: Int,
    val type: Int,
    val messageCount: Int?,
    val unreadCount: Int
)
