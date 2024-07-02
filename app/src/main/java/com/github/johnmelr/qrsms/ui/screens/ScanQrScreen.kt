package com.github.johnmelr.qrsms.ui.screens

import android.Manifest
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.github.johnmelr.qrsms.ui.components.QRCodeBoundingBox
import com.github.johnmelr.qrsms.ui.model.ScanQrCodeViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun ScanQrScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    onScanSuccess: () -> Unit = {},
    qrCodeViewModel: ScanQrCodeViewModel = hiltViewModel(),
    defaultPhoneNumber: String
) {
    // Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        qrCodeViewModel.permissionResult(isGranted)
    }

    val scanQrCodeUiState by qrCodeViewModel.scanQrState.collectAsStateWithLifecycle()

    // Dialog/Status Indicator Flags
    // val isProcessing = scanQrCodeUiState.isProcessing
    var isProcessing = false
    var phoneNumberResult = ""
    var showSuccess = false

    // Camera X Params
    val lifeCycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Camera Preview
    val previewView = remember { PreviewView(context) }
    // CameraX
    val cameraController = LifecycleCameraController(context)


    // Initialize ML Kit Barcode Scanner Options
    val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val barcodeScanner = BarcodeScanning.getClient(scannerOptions)
    val mlKitAnalyzer = MlKitAnalyzer(
        listOf(barcodeScanner),
        COORDINATE_SYSTEM_VIEW_REFERENCED,
        ContextCompat.getMainExecutor(context)
    ) { result ->
        val barcodeResult = result?.getValue(barcodeScanner)
        // Handle no detected qr code
        if (barcodeResult == null || barcodeResult.size == 0 || barcodeResult.first() == null) {
            previewView.overlay.clear()
            return@MlKitAnalyzer
        }

        val qrCodeBoundingBox = QRCodeBoundingBox(barcodeResult[0])
        val qrContent: String = barcodeResult[0].rawValue!!

        isProcessing = true

        previewView.overlay.clear()
        previewView.overlay.add(qrCodeBoundingBox)

        val exchangeResult: String? = qrCodeViewModel.initiateExchange(qrContent, defaultPhoneNumber)
        phoneNumberResult = exchangeResult ?: ""

        val isSuccess =  scanQrCodeUiState.isSuccess
        val errorMessage =  scanQrCodeUiState.errorMsg

        if (exchangeResult != null && isSuccess) {
            showSuccess = true
        } else if (errorMessage.isNotBlank()) {
            val toast = Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT)
            toast.show()
        }

        isProcessing = false
    }

    cameraController.apply {
        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraController.bindToLifecycle(lifeCycleOwner)
        imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST

        setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context),
            mlKitAnalyzer
        )
    }

    // Render Screen
    when {
        scanQrCodeUiState.hasCameraPermission -> {
            ScanQrComponents(
                showScanSuccess = showSuccess,
                showIsProcessing = isProcessing,
                cameraController = cameraController,
                previewView = previewView,
                phoneNumberResult = phoneNumberResult
            )

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
fun ScanQrComponents(
    modifier: Modifier = Modifier,
    onScanSuccess: () -> Unit = {},
    showScanSuccess: Boolean = false,
    showIsProcessing: Boolean = false,
    cameraController: LifecycleCameraController,
    previewView: PreviewView,
    phoneNumberResult: String
){
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showScanSuccess) {
            ScanSuccessMessage(
                modifier = Modifier.zIndex(3f),
                phoneNumber = phoneNumberResult,
                onScanSuccess = onScanSuccess
            )
            return@Box
        }

        Row(
            modifier = modifier
                .background(Color.Transparent)
                .zIndex(2f)
                .matchParentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showIsProcessing) {
                CircularProgressIndicator()
                Text(text = "Processing...")
            }
        }

        // AndroidView composable provides compatibility to View components.
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .zIndex(1f),
            factory = {
                previewView.apply {
                    scaleType = PreviewView.ScaleType.FILL_START
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    controller = cameraController // Set the controller to manage the camera lifecycle
                }
            },
            onRelease = {
                cameraController.unbind()
            },
        )
    }
}

@Composable
fun ScanSuccessMessage(
    modifier: Modifier = Modifier,
    phoneNumber: String,
    onScanSuccess: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(64.dp),
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = "Success",
            tint = Color.Green
        )

        Text(
            modifier = Modifier.padding(12.dp),
            text = "Scan Success",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Encrypted messaging for $phoneNumber is now available.",
            textAlign = TextAlign.Center
        )

        Button(
            modifier = Modifier.padding(32.dp),
            onClick = { onScanSuccess() },
        ) {
            Text(text = "Confirm")
        }
    }
}

@Preview
@Composable
fun ScanSuccessMessagePreview() {
    ScanSuccessMessage(phoneNumber = "+639121231234")
}
