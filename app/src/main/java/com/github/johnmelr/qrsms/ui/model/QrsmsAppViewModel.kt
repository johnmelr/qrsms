package com.github.johnmelr.qrsms.ui.model

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.johnmelr.qrsms.data.preferencesDataStore.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
@HiltViewModel
class QrsmsAppViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val preferenceRepository: PreferencesRepository
): ViewModel() {
    private val _qrsmsAppUiState = MutableStateFlow(QrsmsAppUiState())
    var qrsmsAppUiState: StateFlow<QrsmsAppUiState> = _qrsmsAppUiState.asStateFlow()

    val phoneNumberFlow = preferenceRepository.defaultPhoneNumberFlow

    init {
        getTelephonyPermissions()
    }

    fun setDefaultPhoneNumber(phoneNumber: String) {
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
            application,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val smsSendPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val contactReadPermission = ContextCompat.checkSelfPermission(
            application,
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