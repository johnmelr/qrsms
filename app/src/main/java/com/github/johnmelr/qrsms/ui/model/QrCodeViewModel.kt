package com.github.johnmelr.qrsms.ui.model

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.github.johnmelr.qrsms.crypto.EcdhKeyAgreement
import com.github.johnmelr.qrsms.crypto.KeyStoreManager
import com.google.firebase.components.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.PrivateKey
import javax.crypto.SecretKey

private const val TAG = "QrCodeViewModel"

class QrCodeViewModel(application: Application = Application()): AndroidViewModel(application) {
    private val buildHash = if (BuildConfig.DEBUG) "hL6eyXZVMrW" else "8K21tZymYYh"
    private val appContext = getApplication<Application>().applicationContext

    private val _scanQrUiState = MutableStateFlow(ScanQrCodeUiState(false))
    var scanQrState: StateFlow<ScanQrCodeUiState> = _scanQrUiState.asStateFlow()

    init {
        checkCameraPermission()
    }

    fun initiateExchange(barcodeRawValue: String, myPhoneNumber: String): String? {
        val valueSplit = barcodeRawValue.split(":")

        /**
         * In the QR Code generation process of the application, the encoded string should be in
         * this format
         *      {phone number in E164}:{app_build hash code}:{public key in base 64 format}
         * If the scanned QR code does not follow this format, it could be a QR Code not for
         * QRSMS
         */
        if (valueSplit.size != 3) {
            throw Error("InvalidQrsmsCodeFormat")
        }

        val phoneNumber: String = valueSplit[0]
        val hashString: String = valueSplit[1]
        val publicKeyString: String = valueSplit[2]

        if (hashString == buildHash) {
            val ecdhKeyAgreement = EcdhKeyAgreement(phoneNumber, myPhoneNumber, publicKeyString)

            try {
                ecdhKeyAgreement.performExchange()
                Toast.makeText(appContext, "Success", Toast.LENGTH_SHORT).show()
                return phoneNumber
            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceToString())
                val toast = Toast.makeText(appContext, e.message, Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        return null
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

    fun permissionResult(isGranted: Boolean) {
        _scanQrUiState.update { currentState ->
            currentState.copy (
                hasCameraPermission = isGranted
            )
        }
    }
}

data class ScanQrCodeUiState(
    val hasCameraPermission: Boolean
)