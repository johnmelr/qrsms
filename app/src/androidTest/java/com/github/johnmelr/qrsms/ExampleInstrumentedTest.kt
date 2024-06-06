package com.github.johnmelr.qrsms

import android.telephony.PhoneNumberUtils
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.lang.StringBuilder
import java.security.MessageDigest

const val MY_PHONE_NUMBER = "+63 999 123 4567"
const val OTHER_PHONE_NUMBER = "+63 999 123 1234"

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.github.johnmelr.qrsms", appContext.packageName)
    }

    @Test
    fun testPhoneUtils() {
        val myFormatted = PhoneNumberUtils.formatNumberToE164(MY_PHONE_NUMBER, "PH")
        val otherFormatted = PhoneNumberUtils.formatNumberToE164(OTHER_PHONE_NUMBER, "PH")

        Log.d("InstrumentedTest", myFormatted)
        Log.d("InstrumentedTest", otherFormatted)

        assert(myFormatted == "+639991234567")
        assert(otherFormatted == "+639991231234")
    }

    @Test
    fun testHashCode() {
        val hello = "Hello World"
        val helloHash = MessageDigest.getInstance("SHA-256")
            .digest(hello.toByteArray())
            .fold(StringBuilder()) { sb, it -> sb.append("%02x".format(it)) }
            .toString()

        Log.v("InstrumentedTest", helloHash)
    }
}