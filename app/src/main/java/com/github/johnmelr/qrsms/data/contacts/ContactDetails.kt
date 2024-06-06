package com.github.johnmelr.qrsms.data.contacts

import android.provider.ContactsContract

data class ContactDetails (
    val id: String,
    val displayName: String?,
    val photoThumbUriString: String?,
    val phoneNumber: String?,
    val normalizedPhoneNumber: String
)

object ContactsProjection {
    val contactsProjection = arrayOf(
        ContactsContract.CommonDataKinds.Phone._ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
    )
}
