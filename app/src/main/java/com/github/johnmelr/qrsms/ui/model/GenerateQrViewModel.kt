package com.github.johnmelr.qrsms.ui.model

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.crypto.EcKeyGen
import com.github.johnmelr.qrsms.crypto.KeyStoreManager
import com.github.johnmelr.qrsms.crypto.Base64Utils
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch

/**
 * View Model Class for the GenerateQrScreen.
 *
 * @property publicKeyString the public key value for the contact represented as base64 string
 * @property hasExistingKey boolean value that checks if an existing key already exist for the
 * contact
 */
class GenerateQrViewModel(selectedContact: ContactDetails): ViewModel() {
    var publicKeyString: String? by mutableStateOf(null)
    var hasExistingKey: Boolean by mutableStateOf(false)

    var qrCode: ImageBitmap? by mutableStateOf(null)

    init {
        val pbk = KeyStoreManager.getPublicKeyForNumber(selectedContact.normalizedPhoneNumber)

        if (pbk == null) {
            generateForContact(selectedContact)
        } else {
            hasExistingKey = true
            publicKeyString = Base64Utils.keyToBase64(pbk)
            qrCode = generateQrCode(Base64Utils.keyToBase64(pbk))
        }
    }

    /**
     * Make a call to generateEcKeyPairInKeyStore upon the instantiation of this viewModel.
     * This will create a new Key Pair for the selected contact inside the keystore
     * and store the public key in the publicKeyString property.
     *
     * @param selectedContact ContactDetails instance containing the information of the selected
     * contact
     */
    fun generateForContact(selectedContact: ContactDetails) {
        viewModelScope.launch {
            EcKeyGen.generateEcKeyPairInKeyStore(selectedContact.normalizedPhoneNumber ?: "")

            val publicKey = KeyStoreManager
                .getPublicKeyForNumber(selectedContact.normalizedPhoneNumber ?: "")

            if (publicKey != null) {
                val keyInBase64 = Base64Utils.keyToBase64(publicKey)
                publicKeyString = keyInBase64
                Log.d("Generate QR Screen", "Public Key: $keyInBase64")
            }
        }
    }

    /**
     * Function to encode to given text inside a QR Code using the ZXing library
     *
     * @param textToEncode string of text that will be encoded inside the QR Code.
     *
     * Code taken from https://stackoverflow.com/questions/64443791/android-qr-generator-api
     * from user Siddharth Kamaria - answered on Oct 23, 2020
     */
    fun generateQrCode(textToEncode: String): ImageBitmap {
        val size = 512 //pixels
        val hints = hashMapOf<EncodeHintType, Int>().also {
            it[EncodeHintType.MARGIN] = 1
        } // Make the QR code buffer border narrower
        val bits = QRCodeWriter().encode(textToEncode, BarcodeFormat.QR_CODE, size, size, hints)

        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    it.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }.asImageBitmap()
    }
}

/**
 * Factory Class to create new instance of GenerateQrViewModel with a parameter
 */
class GenerateQrViewModelFactory(private val selectedContact: ContactDetails) :
        ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GenerateQrViewModel(
            selectedContact = selectedContact
        ) as T
    }
}