package com.github.johnmelr.qrsms.ui.screens

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.johnmelr.qrsms.ui.components.QRCodeBoundingBox
import com.github.johnmelr.qrsms.ui.model.ScanQrCodeViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executor

@Composable
fun ScanQrScreen(
    modifier: Modifier = Modifier,
    onScanSuccess: () -> Unit = {},
    qrCodeViewModel: ScanQrCodeViewModel = hiltViewModel(),
    defaultPhoneNumber: String
) {
    val context = LocalContext.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    val scanQrCodeUiState by qrCodeViewModel.scanQrState.collectAsStateWithLifecycle()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        qrCodeViewModel.permissionResult(isGranted)
    }

    // CameraX
    val cameraController = LifecycleCameraController(context)
    val previewView = PreviewView(context)

    when {
        scanQrCodeUiState.hasCameraPermission -> {
            if (scanQrCodeUiState.showSuccessScreen) {
                ScanSuccessScreen(
                    phoneNumber = scanQrCodeUiState.success,
                    onConfirmClick = { onScanSuccess() }
                )
                return
            }

            GenerateQrDialog(
                showDialog = scanQrCodeUiState.showGenerateQrDialog,
                phoneNumber = scanQrCodeUiState.success,
                generateThenExchange = {
                    qrCodeViewModel.generateThenExchange(
                        phoneNumber = scanQrCodeUiState.success,
                        qrContent = scanQrCodeUiState.currentQrContent!!,
                        defaultPhoneNumber
                    )
                },
                onCloseDialog = {
                    qrCodeViewModel.setShowGenerateQrDialog(false)
                }
            )

            Box(modifier = Modifier
                .fillMaxSize()
            ) {
                if (scanQrCodeUiState.loading) {
                    Column(
                        modifier = Modifier
                            .zIndex(2f)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                val lifeCycleOwner = LocalLifecycleOwner.current

                CameraView(
                    previewView = previewView,
                    cameraController = cameraController,
                    lifeCycleOwner = lifeCycleOwner,
                    mainExecutor = mainExecutor,
                    initiateExchange = {
                        qrCodeViewModel.initiateExchange(it, defaultPhoneNumber)
                    }
                )
            }
        } else -> {
            SideEffect {
                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }
}

@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    previewView: PreviewView,
    cameraController: LifecycleCameraController,
    lifeCycleOwner: LifecycleOwner,
    mainExecutor: Executor,
    initiateExchange: (String) -> Unit,
) {
    AndroidView(
        modifier = modifier
            .zIndex(1f)
            .fillMaxSize(),
        factory = {
            previewView
            .apply {
                scaleType = PreviewView.ScaleType.FILL_START
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = cameraController // Set the controller to manage the camera lifecycle
            }.also { previewView ->
                startCamera(
                    cameraController = cameraController,
                    lifeCycleOwner = lifeCycleOwner,
                    previewView = previewView,
                    mainExecutor = mainExecutor,
                    initiateExchange = {
                        initiateExchange(it)
                    },
                )
            }
        },
        onRelease = { cameraController.unbind() }
    )
}

private fun startCamera(
    cameraController: LifecycleCameraController,
    lifeCycleOwner: LifecycleOwner,
    previewView: PreviewView,
    mainExecutor: Executor,
    initiateExchange: (String) -> Unit = {},
) {
    // Initialize ML Kit Barcode Scanner Options
    val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val barcodeScanner = BarcodeScanning.getClient(scannerOptions)
    // Initialize ML Kit Analyzer
    val mlkitAnalyzer = MlKitAnalyzer(
        listOf(barcodeScanner),
        COORDINATE_SYSTEM_VIEW_REFERENCED,
        mainExecutor
    ) {  result: MlKitAnalyzer.Result? ->
        val barcodeResult = result?.getValue(barcodeScanner)

        // Handle no detected qr code
        if (barcodeResult == null || barcodeResult.size == 0 || barcodeResult.first() == null) {
            previewView.overlay.clear()
            return@MlKitAnalyzer
        }

        val qrCodeBoundingBox = QRCodeBoundingBox(barcodeResult[0])
        val qrContent: String = barcodeResult[0].rawValue!!

        previewView.overlay.clear()
        previewView.overlay.add(qrCodeBoundingBox)

        initiateExchange(qrContent)
    }

    cameraController.apply {
        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        imageAnalysisImageQueueDepth = 1
        setImageAnalysisAnalyzer(mainExecutor, mlkitAnalyzer)
        cameraController.bindToLifecycle(lifeCycleOwner)
    }
}

@Composable
fun ScanSuccessScreen(
    modifier: Modifier = Modifier,
    phoneNumber: String,
    onConfirmClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)

    ) {
        Icon(
            modifier = Modifier.size(64.dp),
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = "Success",
            tint = Color.Green,
        )
        Text(
            text = "Scan Success",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Encrypted messaging for $phoneNumber is now available.",
            textAlign = TextAlign.Center
        )

        Button(
            modifier = Modifier.padding(16.dp),
            onClick = { onConfirmClick() },
        ) {
            Text(text = "Confirm")
        }
    }
}

@Preview
@Composable
fun ScanSuccessPreview() {
    ScanSuccessScreen(phoneNumber = "+639121231234")
}

@Composable
fun GenerateQrDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean = false,
    phoneNumber: String,
    generateThenExchange: () -> Unit = {},
    onCloseDialog: () -> Unit = {},
) {
    if(showDialog) {
        AlertDialog(
            onDismissRequest = { onCloseDialog() },
            confirmButton = {
                Button(onClick = { generateThenExchange() }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onCloseDialog() }) {
                    Text("Cancel")
                }
            },
            title = { Text("Generate New QR Code?")},
            text = {
                Text(text = "Unable to generate secret key. " +
                        "No existing QR Code to exchange with ${phoneNumber}.")
            }
        )
    }
}

@Preview
@Composable
fun GenerateQrDialogPreview() {
    GenerateQrDialog(showDialog = true, phoneNumber = "+639121231234")
}