package com.github.johnmelr.qrsms.ui.model

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Picture
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.crypto.Base64Utils
import com.github.johnmelr.qrsms.crypto.EcKeyGen
import com.github.johnmelr.qrsms.crypto.KeyManager
import com.github.johnmelr.qrsms.crypto.KeysRepository
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.utils.ImageHandler
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyPair

/**
 * View Model Class for the GenerateQrScreen.
 *
 * @property publicKeyString the public key value for the contact represented as base64 string
 * @property hasExistingKey boolean value that checks if an existing key already exist for the
 * contact
 */
@HiltViewModel(assistedFactory = GenerateQrViewModel.GenerateQrViewModelFactory::class)
class GenerateQrViewModel @AssistedInject constructor(
    @ApplicationContext private val appContext: Context,
    private val keysRepository: KeysRepository,
    private val keyManager: KeyManager,

    @Assisted private val selectedContact: ContactDetails,
    @Assisted private val myPhoneNumber: String
): ViewModel() {
    private val _publicKeyString = MutableStateFlow("")
    val publicKeyString = _publicKeyString.asStateFlow()

    private val _hasExistingKey = MutableStateFlow(false)
    val hasExistingKey = _hasExistingKey.asStateFlow()

    private val normalizedPhone: String = PhoneNumberUtils.formatNumberToE164(
        myPhoneNumber, "PH" )


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

    fun shareQrCode(picture: Picture): Uri? {
        val bitmap = createBitmapFromPicture(picture)
        return saveBitmapToPng(bitmap)
    }

    fun saveQrCode(picture: Picture) {
        if(
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && // Checking of storage permissions is only necessary for android 9 and below. Since we are only accessing app specific media, we don't need this permissions for android 10 and above.
            appContext
                .checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED
        ) {
            Toast.makeText(appContext, "Storage write permission denied",
                Toast.LENGTH_SHORT).show()
        }

        val imagesFolder = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ), "QRSMS")

        val imageBitmap = createBitmapFromPicture(picture)
        // Generate Filename
        val number = selectedContact.normalizedPhoneNumber.substringAfter("+")
        val timestamp = System.currentTimeMillis()

        val filename = "QRSMS-$number-$timestamp.png"
        val handler = ImageHandler(appContext, imageBitmap, imagesFolder, filename)

        try {
            handler.saveImage()
            Toast.makeText(appContext,
                "QR Code saved successfully",
                Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(appContext, e.message.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun createBitmapFromPicture(picture: Picture): Bitmap {
        val bitmap = Bitmap.createBitmap(
            picture.width,
            picture.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawPicture(picture)
        return bitmap
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
    fun generateQrCode(textToEncode: String): Bitmap {
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
        }
    }

    /**
     * Saves the bitmap image as PNG to app's cache
     *
     * @param image bitmap image to be saved
     * @return Uri of the saved file in the FileProvider
     */
    private fun saveBitmapToPng(image: Bitmap): Uri? {
        val imagesFolder: File = File(appContext.cacheDir, "images")
        var uri: Uri? = null


        try {
            imagesFolder.mkdirs()

            val number = selectedContact.normalizedPhoneNumber.substringAfter("+")
            val timestamp = System.currentTimeMillis()

            val filename = "QRSMS-$number-$timestamp.png"
            val file = File(imagesFolder, filename)

            val stream: FileOutputStream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 90, stream)

            stream.flush()
            stream.close()

            uri = FileProvider.getUriForFile(
                appContext,
                "com.github.johnmelr.qrsms.fileprovider",
                file
            )

            appContext.grantUriPermission(
                appContext.packageName.toString(),
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            Log.d("GenerateViewModel", "$uri")
        } catch (e: IOException)  {
           Log.e("GenerateQrViewModel", "IOException")
        }

        return uri
    }

    @AssistedFactory
    interface GenerateQrViewModelFactory {
        fun create(selectedContact: ContactDetails, myPhoneNumber: String) : GenerateQrViewModel
    }

    // Suppressing unchecked cast warning
    @Suppress("UNCHECKED_CAST")
    companion object {
        // Factory to allow the ViewModel take extra class parameters that cannot be provided by
        // the AppModule
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
}