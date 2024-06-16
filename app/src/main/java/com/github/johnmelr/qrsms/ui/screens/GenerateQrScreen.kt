package com.github.johnmelr.qrsms.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.johnmelr.qrsms.R
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.ui.components.QrImage
import com.github.johnmelr.qrsms.utils.QrUtils.generateQrCode

@Composable
fun GenerateQrScreen(
    modifier: Modifier = Modifier,
    selectedContact: ContactDetails,
    defaultPhoneNumber: String,
    hasExistingKey: Boolean,
    generateKeyPair: (ContactDetails?) -> Unit = { },
    onShareQr: (android.graphics.Picture) -> Unit = { },
    onSaveQr: (android.graphics.Picture) -> Unit = { },
    imageBitmap: ImageBitmap?
) {
    Log.v("GenerateQrScreen", "Testing Composition")

    var hasStoragePermission = false

    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
       hasStoragePermission = isGranted
    }

    Column (
        modifier = modifier
            .padding(12.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        if (imageBitmap == null) {
            CircularProgressIndicator()
            return
        }
        Text(
            text = "Share this QR code to ${selectedContact.displayName 
                ?: selectedContact.normalizedPhoneNumber} and ask for their code to" +
                    " start end-to-end encrypted messaging",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )
        val picture = remember { android.graphics.Picture() }
        QrImage(
            defaultPhoneNumber = defaultPhoneNumber,
            imageBitmap = imageBitmap,
            picture = picture,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Button(onClick = {
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                onSaveQr(picture)
            }) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = ImageVector.vectorResource(
                        R.drawable.download_24dp_fill0_wght400_grad0_opsz24),
                    contentDescription = "Download Icon"
                )
                Text("  Save")
            }
            Button(onClick = { onShareQr(picture) }) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share Icon"
                )
                Text("  Share")
            }
        }

        val openDialog = remember { mutableStateOf(false) }

        if (hasExistingKey) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Showing existing key for ${selectedContact.displayName
                        ?: selectedContact.normalizedPhoneNumber}"
                )
                ElevatedButton(
                    onClick = { openDialog.value = true }) {
                    Text(text = "Generate new key")
                }
            }
        }

        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = { openDialog.value = false },
                confirmButton = { TextButton(onClick = {
                    generateKeyPair(selectedContact)
                    openDialog.value = false
                }) { Text("Confirm") }},
                dismissButton = { TextButton(onClick = {
                    openDialog.value = false
                }) { Text("Cancel") }},
                title = { Text("Generate QR Code") },
                text = { Text("Are you sure you want replace your QR Code for ${selectedContact.displayName ?: selectedContact.normalizedPhoneNumber}?") }
            )

        }

    }
}


@Preview()
@Composable
fun QrImagePreview() {
    val image = generateQrCode("Hello World")

    QrImage(
        defaultPhoneNumber = "+639991231234",
        imageBitmap = image.asImageBitmap()
    )
}