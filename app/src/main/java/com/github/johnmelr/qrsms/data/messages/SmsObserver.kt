package com.github.johnmelr.qrsms.data.messages

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Telephony
import android.util.Log

private const val TAG = "SmsObserver"

class SmsObserver(
    handler: Handler?,
): ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        Log.v("SmsObserver", "$uri")
        super.onChange(selfChange, uri)

        val raw = "${Telephony.Sms.CONTENT_URI}/raw"

        if (uri == null) return
        if (uri.toString().contains(raw)) return
    }
}