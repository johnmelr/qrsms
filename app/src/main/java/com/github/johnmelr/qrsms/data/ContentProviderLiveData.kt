package com.github.johnmelr.qrsms.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.github.johnmelr.qrsms.data.messages.SmsObserver

abstract class ContentProviderLiveData<T>(
    private val context: Context,
    private val uri: Uri
): MutableLiveData<T>() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var observer: SmsObserver

    override fun onActive() {
        Log.v("Live Data", "Registered Content observer!")

        observer = SmsObserver(handler)

        postValue(getContentProviderValue())

        context.contentResolver.registerContentObserver(uri, true, observer)
    }

    override fun onInactive() {
        Log.v("Live Data", "Unregistered Content observer!")

        context.contentResolver.unregisterContentObserver(observer)
    }

    abstract fun getContentProviderValue(): T
}