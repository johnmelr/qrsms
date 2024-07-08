package com.github.johnmelr.qrsms.ui.model

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.crypto.EcKeyGen
import com.github.johnmelr.qrsms.crypto.EcdhKeyAgreement
import com.github.johnmelr.qrsms.crypto.KeyManager
import com.github.johnmelr.qrsms.crypto.KeysRepository
import com.google.firebase.components.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.InvalidKeyException
import java.security.UnrecoverableKeyException
import java.security.spec.InvalidKeySpecException
import javax.inject.Inject

private const val TAG = "QrCodeViewModel"

@HiltViewModel
class ScanQrCodeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val keyManager: KeyManager,
    private val keysRepository: KeysRepository
): ViewModel() {
    private val _scanQrUiState = MutableStateFlow(ScanQrCodeUiState())
    val scanQrState: StateFlow<ScanQrCodeUiState> = _scanQrUiState.asStateFlow()

    private var toast: Toast? = null

    init {
        checkCameraPermission()
    }

    fun initiateExchange(barcodeRawValue: String, myPhoneNumber: String) {
        val valueSplit = barcodeRawValue.split(":")

        if (_scanQrUiState.value.currentQrContent.equals(barcodeRawValue) ||
            _scanQrUiState.value.showGenerateQrDialog
        ) {
            return
        }

        updateCurrentQrContent(barcodeRawValue)

        /**
         * In the QR Code generation process of the application, the encoded string should be in
         * this format
         *      {phone number in E164}:{app_build hash code}:{public key in base 64 format}
         * If the scanned QR code does not follow this format, it could be a QR Code not for
         * QRSMS
         */
        if (valueSplit.size != 2) {
            toast = Toast.makeText(appContext,
                "Not a valid QRSMS Code",
                Toast.LENGTH_SHORT)
            toast!!.show()
            return
        }

        var phoneNumber: String = ""
        val publicKeyString: String = valueSplit[1]
        try {
            phoneNumber = PhoneNumberUtils
                .formatNumberToE164(valueSplit[0], "PH")
        } catch (e: NullPointerException) {
            showToast(appContext, "Not a valid QRSMS Code")

            return
        }

        if (phoneNumber == myPhoneNumber) {
            showToast(
                appContext,
                "Can't perform exchange with your own QR Code",
                Toast.LENGTH_SHORT
            )

            return
        }

        setLoading(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ecdhKeyAgreement = EcdhKeyAgreement(
                    phoneNumber,
                    myPhoneNumber,
                    publicKeyString,
                    keyManager)
                ecdhKeyAgreement.performExchange()

                withContext(Dispatchers.Main) {
                    setShowSuccessScreen(true)
                    setSuccess(phoneNumber)
                }
            } catch (e: UnrecoverableKeyException)  { // Non existent key
                withContext(Dispatchers.Main) {
                    showToast(appContext, e.message, Toast.LENGTH_SHORT)
                }
                setShowGenerateQrDialog(true)
                setSuccess(phoneNumber)
            } catch (e: InvalidKeySpecException) {
                withContext(Dispatchers.Main) {
                    showToast(appContext, "Not a valid QRSMS Code")
                }
            } catch (e: Exception) { // Generic Error
                Log.e(TAG, e.stackTraceToString())
                withContext(Dispatchers.Main) {
                    showToast(appContext, e.message, Toast.LENGTH_SHORT)
                }
            } finally {
                setLoading(false)
            }
        }
    }

    fun generateThenExchange(phoneNumber: String, qrContent: String, myPhoneNumber: String) {
        val keyGen = EcKeyGen(keysRepository)

        viewModelScope.launch(Dispatchers.IO) {
            keyGen.generateKeyPair(phoneNumber)
            setShowGenerateQrDialog(false)
            updateCurrentQrContent(null)
            initiateExchange(qrContent, myPhoneNumber)
        }
    }

    private fun showToast(appContext: Context, message: String?, length: Int = Toast.LENGTH_SHORT) {
        toast?.cancel()

        toast = Toast.makeText(appContext, message, length)
        toast!!.show()
    }

    private fun checkCameraPermission() {
        val cameraPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        _scanQrUiState.update { currentState ->
            currentState.copy(
                hasCameraPermission = cameraPermission
            )
        }
    }

    fun updateCurrentQrContent(qrContent: String?) {
        if (qrContent == scanQrState.value.currentQrContent) {
            return
        }

        _scanQrUiState.update {
            it.copy(currentQrContent = qrContent)
        }
    }

    fun setShowSuccessScreen(newValue: Boolean) {
        _scanQrUiState.update {
            it.copy(showSuccessScreen = newValue)
        }
    }

    fun setShowGenerateQrDialog(newValue: Boolean) {
        _scanQrUiState.update {
            it.copy(showGenerateQrDialog = newValue)
        }
    }

    fun setLoading(newValue: Boolean) {
        _scanQrUiState.update {
            it.copy(loading = newValue)
        }
    }

    fun permissionResult(isGranted: Boolean) {
        _scanQrUiState.update { currentState ->
            currentState.copy (
                hasCameraPermission = isGranted
            )
        }
    }

    fun setSuccess(phoneNumber: String) {
        _scanQrUiState.update {
            it.copy(success = phoneNumber)
        }
    }

}

data class ScanQrCodeUiState(
    val currentQrContent: String? = null,
    val hasCameraPermission: Boolean = false,
    val loading: Boolean = false,
    val error: Exception? = null,
    val success: String = "",
    val showGenerateQrDialog: Boolean = false,
    val showSuccessScreen: Boolean = false,
)