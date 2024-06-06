package com.github.johnmelr.qrsms.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.github.johnmelr.qrsms.ui.components.QRCodeBoundingBox
import com.github.johnmelr.qrsms.ui.model.QrCodeViewModel

@Composable
fun ScanQrScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    onScanSuccess: (String) -> Unit = {},
    qrCodeViewModel: QrCodeViewModel = viewModel(),
    defaultPhoneNumber: String
) {
    val lifeCycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val scanQrCodeUiState by qrCodeViewModel.scanQrState.collectAsStateWithLifecycle()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        qrCodeViewModel.permissionResult(isGranted)
    }

    val previewView = remember { PreviewView(context) }

    // Initialize ML Kit Barcode Scanner Options
    val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val barcodeScanner = BarcodeScanning.getClient(scannerOptions)

    // CameraX
    val cameraController = LifecycleCameraController(context)

    cameraController.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
    cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    cameraController.bindToLifecycle(lifeCycleOwner)

    cameraController.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        MlKitAnalyzer(
            listOf(barcodeScanner),
            COORDINATE_SYSTEM_VIEW_REFERENCED,
            ContextCompat.getMainExecutor(context)
        ) { result: MlKitAnalyzer.Result? ->
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

            val exchangeResult: String? = qrCodeViewModel.initiateExchange(qrContent, defaultPhoneNumber)

            if (exchangeResult != null) {
                onScanSuccess(exchangeResult)
            } else {
                Log.v("Main Activity", "incorrect")
            }
        }
    )

    when {
        scanQrCodeUiState.hasCameraPermission -> {
            Column {
                AndroidView(
                    modifier = modifier.fillMaxSize(),
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
        } else -> {
            SideEffect {
                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }
}