package com.github.johnmelr.qrsms.data.contacts

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.PhoneNumberUtils
import androidx.core.database.getStringOrNull
import com.github.johnmelr.qrsms.data.contacts.ContactsProjection.contactsProjection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.johnmelr.qrsms.data.contacts.ContactsProjection.contactsProjection as projection


class ContactsRepository(
    private val dispatcherIO: CoroutineDispatcher,
    private val contentResolver: ContentResolver,
) {
    private val contactsUri: Uri = Phone.CONTENT_URI

    suspend fun getAllContacts(
        contactList: MutableList<ContactDetails>,
    ) {
        withContext(dispatcherIO) {
            val contactCursor: Cursor = contentResolver.query(
                contactsUri,
                projection,
                null,
                null,
                null
            ) ?: return@withContext

            if (contactCursor.count == 0) return@withContext

            while (contactCursor.moveToNext() && contactCursor.count > contactCursor.position) {
                val idIndex: Int = contactCursor.getColumnIndex(Phone._ID)
                val displayNameIndex: Int = contactCursor.getColumnIndex(Phone.DISPLAY_NAME)
                val photoUriIndex: Int = contactCursor.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
                val phoneNumberIndex: Int = contactCursor.getColumnIndex(Phone.NUMBER)
                val normalizedPhoneNumberIndex: Int = contactCursor.getColumnIndex(Phone.NORMALIZED_NUMBER)

                val contact = ContactDetails(
                    id = contactCursor.getString(idIndex),
                    displayName = contactCursor.getString(displayNameIndex),
                    photoThumbUriString = contactCursor.getString(photoUriIndex),
                    phoneNumber = contactCursor.getString(phoneNumberIndex),
                    normalizedPhoneNumber = contactCursor.getString(normalizedPhoneNumberIndex) ?: ""
                )

                contactList.add(contact)
            }

            contactCursor.close()
        }
    }

    suspend fun getContactDetailsOfAddress(address: String): ContactDetails? {
        return withContext(dispatcherIO) {
            val normalizedAddress = PhoneNumberUtils.formatNumberToE164(address, "PH")

            val contactCursor: Cursor = contentResolver.query(
                contactsUri,
                projection,
                "${Phone.NORMALIZED_NUMBER}=?",
                arrayOf(if (normalizedAddress.isNullOrEmpty()) address else normalizedAddress),
                null
            ) ?: return@withContext null

            if (contactCursor.count == 0) {
                contactCursor.close()
                return@withContext null
            }

            contactCursor.moveToFirst()
            val idIndex: Int = contactCursor.getColumnIndex(Phone._ID)

            val displayNameIndex: Int = contactCursor.getColumnIndex(Phone.DISPLAY_NAME)
            val photoUriIndex: Int = contactCursor.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
            val phoneNumberIndex: Int = contactCursor.getColumnIndex(Phone.NUMBER)
            val normalizedPhoneNumberIndex: Int = contactCursor.getColumnIndex(Phone.NORMALIZED_NUMBER)

            val id: String = contactCursor.getString(idIndex)
            val displayName: String? = contactCursor.getStringOrNull(displayNameIndex)
            val photoThumbUri: String? = contactCursor.getString(photoUriIndex) ?: null
            val phoneNumber: String? = contactCursor.getString(phoneNumberIndex) ?: null
            val normalizedPhoneNumber: String = contactCursor.getString(normalizedPhoneNumberIndex)

            val contact = ContactDetails(
                id = id,
                displayName = displayName,
                photoThumbUriString = photoThumbUri,
                phoneNumber = phoneNumber,
                normalizedPhoneNumber = normalizedPhoneNumber
            )

            contactCursor.close()
            return@withContext contact
        }
    }

    suspend fun searchContact(queryString: String, listBuffer: MutableList<ContactDetails>) {
        withContext(dispatcherIO) {
            val searchCursor: Cursor = contentResolver.query(
                contactsUri,
                contactsProjection,
                "${Phone.DISPLAY_NAME} LIKE ? OR ${Phone.NORMALIZED_NUMBER} LIKE ? OR ${Phone.NUMBER} LIKE ?",
                arrayOf(
                    "%$queryString%",
                    "%$queryString%",
                    "%$queryString%"
                ),
                null,
            ) ?: return@withContext

            if (searchCursor.count == 0) {
                searchCursor.close()
                return@withContext
            }

            while (searchCursor.moveToNext() && searchCursor.count > searchCursor.position) {
                val idIndex: Int = searchCursor.getColumnIndex(Phone._ID)
                val displayNameIndex: Int = searchCursor.getColumnIndex(Phone.DISPLAY_NAME)
                val photoUriIndex: Int = searchCursor.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
                val phoneNumberIndex: Int = searchCursor.getColumnIndex(Phone.NUMBER)
                val normalizedPhoneNumberIndex: Int = searchCursor.getColumnIndex(Phone.NORMALIZED_NUMBER)

                val contact = ContactDetails(
                    id = searchCursor.getString(idIndex),
                    displayName = searchCursor.getString(displayNameIndex),
                    photoThumbUriString = searchCursor.getString(photoUriIndex),
                    phoneNumber = searchCursor.getString(phoneNumberIndex),
                    normalizedPhoneNumber = searchCursor.getString(normalizedPhoneNumberIndex) ?: ""
                )

                listBuffer.add(contact)
            }
            searchCursor.close()
        }
    }
}