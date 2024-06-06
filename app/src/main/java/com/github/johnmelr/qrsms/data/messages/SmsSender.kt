package com.github.johnmelr.qrsms.data.messages

import android.content.Context
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import com.github.johnmelr.qrsms.crypto.Base64Utils
import com.github.johnmelr.qrsms.crypto.QrsmsCipher
import com.github.johnmelr.qrsms.data.utils.Flags
import com.github.johnmelr.qrsms.dataStore
import java.security.MessageDigest



/**
 * Class responsible for sending Sms Message via the Sms Manager
 */
class SmsSender(
    appContext: Context,
) {
    private val smsService = appContext.getSystemService(SmsManager::class.java)
    private val defaultSubId = smsService?.subscriptionId ?: SmsManager.getDefaultSmsSubscriptionId()

    private val smsManager = createSmsManagerInstance()

    /**
     * Creates an instance of SmsManager for the default subscription ID
     *
     * @return instance of SmsManager to enable capabilities to send message
     *  https://developer.android.com/reference/android/telephony/SmsManager
     */
    private fun createSmsManagerInstance(): SmsManager {
        // Else clause is deprecated starting from Api 31 and above but is still required
        // since createForSubscriptionId is added for Api 31 only.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsService.createForSubscriptionId(defaultSubId)
        } else {
            SmsManager.getSmsManagerForSubscriptionId(defaultSubId)
        }
    }

    /**
     * Send message through the SmsManager. This function is also responsible for
     * the given message and dividing it to multiple parts if length of the message is greater than
     * the minimum length allowed for a single SMS message which is 160 characters.
     */
    fun sendMessage(recipient: String, message: String) {
        // 160 is the maximum message length
        if (message.length <= 160) {
            // No need for further processing
            smsManager.sendTextMessage(
                recipient,
                null,
                message,
                null,
                null
            )
        } else {
            val multiPartMessage: ArrayList<String> = smsManager.divideMessage(message)

            smsManager.sendMultipartTextMessage(
                recipient,
                null,
                multiPartMessage,
                null,
                null,
            )
        }
    }

    /**
     * Send Encrypted message through the SmsManager. This function is also responsible for
     * the given message and dividing it to multiple parts if length of the message is greater than
     * the minimum length allowed for a single SMS message which is 160 bytes (for
     * the regular Latin-1 encoding).
     */
    fun sendEncryptedMessage(recipient: String, message: String) {
        // normalized phone number format is preferred to ensure that all keystore aliases
        // have a consistent format.
        val normalizedPhoneNumber = PhoneNumberUtils.formatNumberToE164(recipient, "PH")

        val cipher = QrsmsCipher(normalizedPhoneNumber)
        val cipherText = cipher.encrypt(message)

        val finalMessage = Base64Utils
            .stringToBase64(Flags.CIPHER_START)  +
            cipherText +
            Base64Utils.stringToBase64(Flags.CIPHER_END)

        sendMessage(
            recipient,
            finalMessage
        )
    }
}