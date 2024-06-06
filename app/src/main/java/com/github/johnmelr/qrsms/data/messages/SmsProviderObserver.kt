package com.github.johnmelr.qrsms.data.messages

import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.johnmelr.qrsms.data.ContentProviderLiveData
import kotlin.coroutines.coroutineContext

class SmsProviderObserver(
    private val context: Context,
    private val uri: Uri,
    private val smsRepository: SmsRepository
): ContentProviderLiveData<List<QrsmsMessage>>(context, uri) {

    private val messageList = mutableListOf<QrsmsMessage>()

    override fun getContentProviderValue(): List<QrsmsMessage> {
        Log.v("SmsProviderObserver", "Getting content provider value...")
        return emptyList()
    }
}