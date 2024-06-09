package com.github.johnmelr.qrsms.ui.model

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.data.preferencesDataStore.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG="QrsmsAppViewModel"

/**
 * Top Level view model for the QrsmsApplication. This view model holds the logic for
 * handling of the PreferenceRepository class which is the class responsible for
 * communicating with the application's Preference DataStore
 * [https://developer.android.com/topic/libraries/architecture/datastore]
 *
 * @property preferenceRepository
 * @constructor Creates a new instance of the view model
 */
class QrsmsAppViewModel(
    application: Application,
    private val preferenceRepository: PreferencesRepository
): AndroidViewModel(application) {
    private val appContext = getApplication<Application>().applicationContext

    private val _qrsmsAppUiState = MutableStateFlow(QrsmsAppUiState())
    var qrsmsAppUiState: StateFlow<QrsmsAppUiState> = _qrsmsAppUiState.asStateFlow()

    val phoneNumberFlow = preferenceRepository.defaultPhoneNumberFlow

    init {
        getTelephonyPermissions()
    }

    fun setDefaultPhoneNumber(phoneNumber: String) {
        Log.v(TAG, "Updating phone number: $phoneNumber")
        viewModelScope.launch {
            preferenceRepository.setDefaultPhoneNumber(phoneNumber)
        }
    }

    fun clearPreferencesRepository() {
        viewModelScope.launch {
            preferenceRepository.clearDataStore()
        }
    }

    private fun getTelephonyPermissions() {
        val smsReadPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val smsSendPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val contactReadPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        _qrsmsAppUiState.update { currentState ->
            currentState.copy(
                hasSmsReadPermission = smsReadPermission,
                hasSmsSendPermission = smsSendPermission,
                hasContactReadPermission = contactReadPermission,
            )
        }
    }

    fun updateIsFirstLaunched() {
        _qrsmsAppUiState.update {
            it.copy(isFirstLaunched = false)
        }
    }

    fun updatePermission(permissionString: String, isGranted: Boolean) {
        when (permissionString) {
            Manifest.permission.READ_SMS -> {
                _qrsmsAppUiState.update { currentState ->
                    currentState.copy(
                        hasSmsReadPermission = isGranted
                    )
                }
            } Manifest.permission.SEND_SMS -> {
                _qrsmsAppUiState.update { currentState ->
                    currentState.copy(
                        hasSmsSendPermission = isGranted
                    )
                }
            } Manifest.permission.READ_CONTACTS -> {
                _qrsmsAppUiState.update { currentState ->
                    currentState.copy (
                        hasContactReadPermission = isGranted
                    )
                }
            } else -> {
                Log.e(TAG, "$permissionString not included.")
            }
        }
    }
}

data class QrsmsAppUiState(
    val isFirstLaunched: Boolean = true,
    val hasSmsReadPermission: Boolean = false,
    val hasSmsSendPermission: Boolean = false,
    val hasContactReadPermission: Boolean = false
)

/**
 * Factory Class to create new instance of GenerateQrViewModel with a parameter
 */
class QrsmsAppViewModelFactory(private val application: Application
                               ,private val preferencesRepository: PreferencesRepository) :
    ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return QrsmsAppViewModel(
            application,
            preferencesRepository
        ) as T
    }
}