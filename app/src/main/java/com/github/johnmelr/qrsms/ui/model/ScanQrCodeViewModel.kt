package com.github.johnmelr.qrsms.ui.model

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.crypto.EcdhKeyAgreement
import com.github.johnmelr.qrsms.crypto.KeyManager
import com.google.firebase.components.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "QrCodeViewModel"

@HiltViewModel
class ScanQrCodeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val keyManager: KeyManager
): ViewModel() {
    private val buildHash = if (BuildConfig.DEBUG) "hL6eyXZVMrW" else "8K21tZymYYh"

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
        if (valueSplit.size != 2) {
            throw Error("InvalidQrsmsCodeFormat")
        }

        val phoneNumber: String = valueSplit[0]
        val publicKeyString: String = valueSplit[1]

        val ecdhKeyAgreement = EcdhKeyAgreement(
            phoneNumber,
            myPhoneNumber,
            publicKeyString,
            keyManager)

        try {
            viewModelScope.launch(Dispatchers.IO) {
                ecdhKeyAgreement.performExchange()
//                Toast.makeText(appContext, "Success", Toast.LENGTH_SHORT).show()
            }
            return phoneNumber
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
            val toast = Toast.makeText(appContext, e.message, Toast.LENGTH_SHORT)
            toast.show()
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