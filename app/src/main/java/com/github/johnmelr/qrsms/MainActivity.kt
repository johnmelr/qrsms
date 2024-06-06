package com.github.johnmelr.qrsms

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.asLiveData
import com.github.johnmelr.qrsms.data.preferencesDataStore.PreferencesRepository
import com.github.johnmelr.qrsms.ui.model.QrsmsAppViewModel
import com.github.johnmelr.qrsms.ui.model.QrsmsAppViewModelFactory
import com.github.johnmelr.qrsms.ui.theme.QRSMSTheme
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val TAG: String = "Main Activity"

const val USER_PREFERENCE_KEY = "user_preference"

val Context.dataStore by preferencesDataStore(name = USER_PREFERENCE_KEY)

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var preferencesRepository: PreferencesRepository

    private val qrsmsAppViewModel by viewModels<QrsmsAppViewModel> {
        QrsmsAppViewModelFactory(this.application, preferencesRepository)
    }

    private val permissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Manifest.permission.READ_PHONE_NUMBERS
        else
            Manifest.permission.READ_PHONE_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        preferencesRepository = PreferencesRepository(this.dataStore)

        val hasTelephonyFeature: Boolean = this.packageManager.hasSystemFeature(
            "android.hardware.telephony"
        )

        if (!hasTelephonyFeature) {
            finishAndRemoveTask()
        }

        Log.v(TAG, "has feature: $hasTelephonyFeature")

        var subId = -1
        val smsService = this.getSystemService(SmsManager::class.java)
        subId = smsService?.subscriptionId ?: SmsManager.getDefaultSmsSubscriptionId()

        Log.v(TAG, subId.toString())

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            qrsmsAppViewModel.phoneNumberFlow.asLiveData(Dispatchers.IO)
                .observe(this@MainActivity) { defaultPhoneNumber ->
                    if (defaultPhoneNumber != "none") return@observe

                    if (hasTelephonyFeature) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val hasReadPhoneNumbersPermissions =
                                permissions[Manifest.permission.READ_PHONE_NUMBERS] ?: false

                            Log.v(TAG, "Permission $hasReadPhoneNumbersPermissions")

                            if (hasReadPhoneNumbersPermissions) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val subscriptionManager = this
                                        .getSystemService(SubscriptionManager::class.java)
                                    val phoneNumber = subscriptionManager.getPhoneNumber(subId)
                                    val phoneNumberFormatted = PhoneNumberUtils.formatNumberToE164(
                                        phoneNumber, "PH"
                                    )
                                    qrsmsAppViewModel.setDefaultPhoneNumber(phoneNumberFormatted)

                                    Log.d(TAG, "Default Phone Number: $phoneNumber")
                                } else {
                                    val telephonyManager = this
                                        .getSystemService(TelephonyManager::class.java)

                                    @SuppressLint("HardwareIds")
                                    val phoneNumber = telephonyManager.line1Number
                                    if (!phoneNumber.isNullOrEmpty()) {
                                        val phoneNumberFormatted = PhoneNumberUtils.formatNumberToE164(
                                            phoneNumber, "PH"
                                        )
                                        qrsmsAppViewModel.setDefaultPhoneNumber(phoneNumberFormatted)
                                    }
                                    Log.d(TAG, "Default Phone Number Line 1: $phoneNumber")
                                }
                            } else {
                                Log.e(TAG, "No Read Phone Number Permission")
                            }
                        } else {
                            val hasReadPhoneStatePermissions =
                                permissions[Manifest.permission.READ_PHONE_STATE] ?: false

                            if (hasReadPhoneStatePermissions) {
                                val telephonyManager = this.getSystemService(TelephonyManager::class.java)

                                @SuppressLint("HardwareIds")
                                val phoneNumber = telephonyManager.line1Number
                                if (!phoneNumber.isNullOrEmpty()) {
                                    val phoneNumberFormatted = PhoneNumberUtils.formatNumberToE164(
                                        phoneNumber, "PH"
                                    )
                                    qrsmsAppViewModel.setDefaultPhoneNumber(phoneNumberFormatted)
                                }
                                Log.d(TAG, "Default Phone Number: $phoneNumber")
                            } else {
                                Log.e(TAG, "No Read Phone State Permission")
                            }
                        }
                    }
                }


            val readSmsPermission = permissions[Manifest.permission.READ_SMS] ?: false
            val sendSmsPermission = permissions[Manifest.permission.SEND_SMS] ?: false
            val readContactPermission = permissions[Manifest.permission.READ_CONTACTS] ?: false
            if (readSmsPermission)
                qrsmsAppViewModel.updatePermission(Manifest.permission.READ_SMS, true)
            if (sendSmsPermission)
                qrsmsAppViewModel.updatePermission(Manifest.permission.SEND_SMS, true)
            if (readContactPermission)
                qrsmsAppViewModel.updatePermission(Manifest.permission.READ_CONTACTS, true)
        }

        var shouldShowPermissionRationale = false

        // Check list of permissions
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED) {
                Log.d("$TAG: Permission Check", "Permission granted for: $permission")
            }

            // Look for permissions that needs the permission rationale to be displayed
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                shouldShowPermissionRationale = true
            }
        }

        if (shouldShowPermissionRationale) {
            requestPermissionLauncher.launch(permissions)
        }

        setContent {
            QRSMSTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                )   {
                    QrsmsApp(
                        qrsmsAppViewModel = qrsmsAppViewModel
                    )
                }
            }
        }
    }
}

