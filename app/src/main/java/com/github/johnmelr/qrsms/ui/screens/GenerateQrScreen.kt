package com.github.johnmelr.qrsms.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.johnmelr.qrsms.data.contacts.ContactDetails

@Composable
fun GenerateQrScreen(
    modifier: Modifier = Modifier,
    selectedContact: ContactDetails,
    hasExistingKey: Boolean,
    generateKeyPair: (ContactDetails?) -> Unit = { },
    imageBitmap: ImageBitmap?
) {
    Log.v("GenerateQrScreen", "Testing Composition")

    Column (
        modifier = modifier
            .padding(12.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Share this QR code to ${selectedContact.displayName 
                ?: selectedContact.normalizedPhoneNumber} and ask for their code to" +
                    " start end-to-end encrypted messaging",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        QrImage(imageBitmap = imageBitmap)

        if (hasExistingKey) {
            Column {
                Text(
                    text = "Showing existing key for ${selectedContact.displayName
                        ?: selectedContact.normalizedPhoneNumber}"
                )
                Button(
                    onClick = { generateKeyPair(selectedContact) }) {

                    Text(text = "Generate New Key")
                }
            }
        }
    }
}



@Composable
fun QrImage(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap?,
) {
    if (imageBitmap == null) return

    Box(modifier = modifier) {
        Image(
            modifier = Modifier.padding(16.dp),
            bitmap = imageBitmap,
            contentDescription = "QR Code"
        )
    }
}

