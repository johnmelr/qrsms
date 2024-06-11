package com.github.johnmelr.qrsms.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
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
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.asLiveData
import androidx.room.Room
import com.github.johnmelr.qrsms.data.preferencesDataStore.PreferencesRepository
import com.github.johnmelr.qrsms.data.room.DatabasePassphrase
import com.github.johnmelr.qrsms.data.room.KeyPairDatabase
import com.github.johnmelr.qrsms.data.room.KeyPairDatabaseFactory
import com.github.johnmelr.qrsms.ui.model.QrsmsAppViewModel
import com.github.johnmelr.qrsms.ui.model.QrsmsAppViewModelFactory
import com.github.johnmelr.qrsms.ui.theme.QRSMSTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import java.net.PasswordAuthentication
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.crypto.SecretKey
import javax.inject.Inject

const val TAG: String = "Main Activity"

const val USER_PREFERENCE_KEY = "user_preference"

val Context.dataStore by preferencesDataStore(name = USER_PREFERENCE_KEY)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val qrsmsAppViewModel by viewModels<QrsmsAppViewModel>()

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

        // Close the app if the hardware does not support telephony
        if (!this.packageManager.hasSystemFeature( "android.hardware.telephony" ))
            finishAndRemoveTask()

        // initializeSQLCipher()
        System.loadLibrary("sqlcipher")

        cameraExecutor = Executors.newSingleThreadExecutor()
        preferencesRepository = PreferencesRepository(this.dataStore)

        // Request runtime permission
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val smsService = this.getSystemService(SmsManager::class.java)
            val subId = smsService?.subscriptionId ?: SmsManager.getDefaultSmsSubscriptionId()

            qrsmsAppViewModel.phoneNumberFlow.asLiveData(Dispatchers.Main)
                .observe(this@MainActivity) { defaultPhoneNumber ->
                    if (defaultPhoneNumber != "none") return@observe

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val hasReadPhoneNumbersPermissions =
                            permissions[Manifest.permission.READ_PHONE_NUMBERS] ?: false

                        if (hasReadPhoneNumbersPermissions) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val subscriptionManager = this
                                    .getSystemService(SubscriptionManager::class.java)
                                val phoneNumber = subscriptionManager.getPhoneNumber(subId)
                                val phoneNumberFormatted = PhoneNumberUtils.formatNumberToE164(
                                    phoneNumber, "PH"
                                )
                                qrsmsAppViewModel.setDefaultPhoneNumber(phoneNumberFormatted)
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
                            }
                        } else {
                            Log.e(TAG, "Unable to read. No Read Phone Number Permission")
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
                        } else {
                            Log.e(TAG, "No Read Phone State Permission")
                        }
                    }
                }


            val readSmsPermission = permissions[Manifest.permission.READ_SMS] ?: false
            val sendSmsPermission = permissions[Manifest.permission.SEND_SMS] ?: false
            val readContactPermission = permissions[Manifest.permission.READ_CONTACTS] ?: false
            qrsmsAppViewModel.updatePermission(Manifest.permission.READ_SMS, readSmsPermission)
            qrsmsAppViewModel.updatePermission(Manifest.permission.SEND_SMS, sendSmsPermission)
            qrsmsAppViewModel.updatePermission(Manifest.permission.READ_CONTACTS, readContactPermission)
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

