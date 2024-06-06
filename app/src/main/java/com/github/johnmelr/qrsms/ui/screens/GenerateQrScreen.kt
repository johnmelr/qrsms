package com.github.johnmelr.qrsms.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.johnmelr.qrsms.R
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.ui.model.GenerateQrViewModel
import com.github.johnmelr.qrsms.ui.model.GenerateQrViewModelFactory
import com.google.firebase.components.BuildConfig

private val buildHash = if (BuildConfig.DEBUG) "hL6eyXZVMrW" else "8K21tZymYYh"

@Composable
fun GenerateQrScreen(
    modifier: Modifier = Modifier,
    selectedContact: ContactDetails?,
    generateQrViewModel: GenerateQrViewModel = viewModel(
        factory = GenerateQrViewModelFactory(selectedContact!!)
    ),
    defaultPhoneNumber: String,
) {
    Log.v("GenerateQrScreen", "Testing Composition")

    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= 33
        && context
            .packageManager
            .hasSystemFeature(PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION)
        && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
            == PackageManager.PERMISSION_GRANTED
    ) {
        val systemService = context.getSystemService(SmsManager::class.java)
        val subId = systemService.subscriptionId

        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        val phoneNumber = subscriptionManager.getPhoneNumber(subId)

        Log.v("GenerateQrScreen", phoneNumber)
    }

    Column (
        modifier = modifier
            .padding(12.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Share this QR code to ${selectedContact!!.displayName 
                ?: selectedContact.normalizedPhoneNumber} and ask for their code to" +
                    " start end-to-end encrypted messaging",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        QrImage {
            generateQrViewModel.generateQrCode(
                "${defaultPhoneNumber}:$buildHash:" +
                        "${generateQrViewModel.publicKeyString}"
            )
        }
        if (generateQrViewModel.hasExistingKey) {
            Column {
                Text(
                    text = "Showing existing key for ${selectedContact.displayName
                        ?: selectedContact.normalizedPhoneNumber}"
                )
                Button(
                    onClick = { generateQrViewModel.generateForContact(selectedContact) }) {

                    Text(text = "Generate New Key")
                }
            }
        }
    }
}



@Composable
fun QrImage(
    modifier: Modifier = Modifier,
    bitmapProvider: () -> ImageBitmap
) {
    val imageBitmap = bitmapProvider()

    Box(modifier = modifier) {
        Image(
            modifier = Modifier.padding(16.dp),
            bitmap = imageBitmap,
            contentDescription = "QR Code"
        )
    }
}

