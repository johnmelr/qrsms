package com.github.johnmelr.qrsms.ui.components

import android.graphics.Picture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QrImage(
    modifier: Modifier = Modifier,
    defaultPhoneNumber: String,
    imageBitmap: ImageBitmap?,
    picture: android.graphics.Picture = remember { Picture() },
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
            ).drawWithCache {
                val width = this.size.width.toInt()
                val height = this.size.height.toInt()

                onDrawWithContent {
                    val pictureCanvas =
                        Canvas(
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
