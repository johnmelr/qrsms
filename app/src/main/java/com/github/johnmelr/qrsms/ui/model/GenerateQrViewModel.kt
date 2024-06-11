package com.github.johnmelr.qrsms.ui.model

import android.graphics.Bitmap
import android.graphics.Color
import android.telephony.PhoneNumberUtils
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
import com.github.johnmelr.qrsms.crypto.KeyManager
import com.github.johnmelr.qrsms.crypto.KeysRepository
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.google.android.datatransport.BuildConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

/**
 * View Model Class for the GenerateQrScreen.
 *
 * @property publicKeyString the public key value for the contact represented as base64 string
 * @property hasExistingKey boolean value that checks if an existing key already exist for the
 * contact
 */
@HiltViewModel(assistedFactory = GenerateQrViewModel.GenerateQrViewModelFactory::class)
class GenerateQrViewModel @AssistedInject constructor(
    private val keysRepository: KeysRepository,
    private val keyManager: KeyManager,

    @Assisted private val selectedContact: ContactDetails,
    @Assisted private val myPhoneNumber: String
): ViewModel() {
    private val _publicKeyString = MutableStateFlow("")
    val publicKeyString = _publicKeyString.asStateFlow()

    private val _hasExistingKey = MutableStateFlow(false)
    val hasExistingKey = _hasExistingKey.asStateFlow()

//    private val _qrCode = MutableStateFlow<ImageBitmap?>(null)
//    val qrCode = _qrCode.asStateFlow()
    @AssistedFactory
    interface GenerateQrViewModelFactory {
        fun create(selectedContact: ContactDetails, myPhoneNumber: String) : GenerateQrViewModel
    }

    // Suppressing unchecked cast warning
    @Suppress("UNCHECKED_CAST")
    companion object {

        // putting this function inside
        // companion object so that we can
        // access it from outside i.e. from fragment/activity
        fun providesFactory(
            assistedFactory: GenerateQrViewModelFactory,
            selectedContact: ContactDetails,
            myPhoneNumber: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {

                // using our ArticlesFeedViewModelFactory's create function
                // to actually create our viewmodel instance
                return assistedFactory.create(
                    selectedContact, myPhoneNumber
                ) as T
            }
        }
    }

    init {
        generate(selectedContact, myPhoneNumber)
    }

    fun generate(selectedContact: ContactDetails, myPhoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val keyExist: Boolean = keyManager.doesKeyPairExist(selectedContact
                .normalizedPhoneNumber)

            if (keyExist) {
                val pbk = keyManager.getPublicKeyForNumber(selectedContact.normalizedPhoneNumber)!!

                _hasExistingKey.value = true
                _publicKeyString.value = Base64Utils.keyToBase64(pbk)
            } else {
                generateKeyForContact(selectedContact)
            }
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
    private fun generateKeyForContact(selectedContact: ContactDetails) {
        viewModelScope.launch {
            val keyPair: KeyPair = EcKeyGen(keysRepository)
                .generateKeyPair(selectedContact.normalizedPhoneNumber)

            val publicKey = keyPair.public

            if (publicKey != null) {
                val keyInBase64 = Base64Utils.keyToBase64(publicKey)
                _publicKeyString.value = keyInBase64
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
//
///**
// * Factory Class to create new instance of GenerateQrViewModel with a parameter
// */
//class GenerateQrViewModelFactory(private val selectedContact: ContactDetails) :
//        ViewModelProvider.NewInstanceFactory() {
//
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        return GenerateQrViewModel(
//            selectedContact = selectedContact
//        ) as T
//    }
//}