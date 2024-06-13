package com.github.johnmelr.qrsms.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.johnmelr.qrsms.R
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

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
            modifier = Modifier
                .drawWithCache {
                    val width = this.size.width.toInt()
                    val height = this.size.height.toInt()

                    onDrawWithContent {
                        val pictureCanvas =
                            androidx.compose.ui.graphics.Canvas(
                                picture.beginRecording(width, height)
                            )
                        draw(this, this.layoutDirection, pictureCanvas, this.size) {
                            this@onDrawWithContent.drawContent()
                        }
                        picture.endRecording()

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawPicture(picture)
                        }
                    }
                },
            defaultPhoneNumber = defaultPhoneNumber,
            imageBitmap = imageBitmap
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

        if (hasExistingKey) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Showing existing key for ${selectedContact.displayName
                        ?: selectedContact.normalizedPhoneNumber}"
                )
                ElevatedButton(
                    onClick = { generateKeyPair(selectedContact) }) {
                    Text(text = "Generate new key")
                }
            }
        }
    }
}



@Composable
fun QrImage(
    modifier: Modifier = Modifier,
    defaultPhoneNumber: String,
    imageBitmap: ImageBitmap?,
) {
    if (imageBitmap == null) return

    val shape = RoundedCornerShape(16.dp)
    val color = MaterialTheme.colorScheme.secondaryContainer

    Column(
        modifier = modifier
            .background(color, shape)
            .padding(
                start = 48.dp,
                end = 48.dp,
                top = 16.dp,
                bottom = 32.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = defaultPhoneNumber,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "QRSMS Code",
                fontWeight = FontWeight.Light,
                fontSize = 10.sp
            )
        }
        Image(
            modifier = Modifier
                .size(200.dp)
                .clip(
                    RoundedCornerShape(12.dp)
                ),
            bitmap = imageBitmap,
            contentDescription = "QR Code",
        )
    }
}

fun generateQrCode(textToEncode: String): Bitmap {
    val size = 512 //pixels
    val hints = hashMapOf<EncodeHintType, Int>().also {
        it[EncodeHintType.MARGIN] = 2
    } // Make the QR code buffer border narrower
    val bits = QRCodeWriter().encode(textToEncode, BarcodeFormat.QR_CODE, size, size, hints)

    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
        for (x in 0 until size) {
            for (y in 0 until size) {
                it.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
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