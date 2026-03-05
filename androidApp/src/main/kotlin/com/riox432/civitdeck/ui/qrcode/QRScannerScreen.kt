package com.riox432.civitdeck.ui.qrcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.riox432.civitdeck.ui.theme.CivitDeckColors
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onBack: () -> Unit,
    onModelScanned: (Long) -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CivitDeckColors.scrim.copy(alpha = OVERLAY_ALPHA),
                    titleContentColor = CivitDeckColors.onScrim,
                    navigationIconContentColor = CivitDeckColors.onScrim,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (hasCameraPermission) {
                CameraPreviewWithScanner(
                    onModelScanned = onModelScanned,
                )
            } else {
                PermissionDeniedContent()
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Camera permission is required to scan QR codes",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}

@Suppress("TooGenericExceptionCaught")
@Composable
private fun CameraPreviewWithScanner(
    onModelScanned: (Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scannedModelId by remember { mutableStateOf<Long?>(null) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                            ) { imageProxy ->
                                processBarcode(imageProxy) { modelId ->
                                    if (scannedModelId == null) {
                                        scannedModelId = modelId
                                        onModelScanned(modelId)
                                    }
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis,
                        )
                    } catch (_: Exception) {
                        // Camera binding failed
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        ScannerOverlay()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
                // Cleanup failed
            }
        }
    }
}

@Composable
private fun ScannerOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(VIEWFINDER_SIZE)
                    .border(
                        width = 2.dp,
                        color = CivitDeckColors.onScrim.copy(alpha = BORDER_ALPHA),
                        shape = RoundedCornerShape(Spacing.md),
                    ),
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = "Point at a CivitAI model QR code",
                style = MaterialTheme.typography.bodyMedium,
                color = CivitDeckColors.onScrim,
                modifier = Modifier
                    .background(
                        CivitDeckColors.scrim.copy(alpha = OVERLAY_ALPHA),
                        RoundedCornerShape(Spacing.sm),
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
    }
}

@Suppress("TooGenericExceptionCaught")
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processBarcode(
    imageProxy: androidx.camera.core.ImageProxy,
    onModelFound: (Long) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient()
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                if (barcode.valueType == Barcode.TYPE_URL || barcode.valueType == Barcode.TYPE_TEXT) {
                    val url = barcode.url?.url ?: barcode.rawValue ?: continue
                    val modelId = extractModelId(url)
                    if (modelId != null) {
                        onModelFound(modelId)
                        return@addOnSuccessListener
                    }
                }
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

/**
 * Extracts model ID from a CivitAI URL.
 * Supports: https://civitai.com/models/12345 and https://civitai.com/models/12345/model-name
 */
internal fun extractModelId(url: String): Long? {
    val pattern = Regex("""civitai\.com/models/(\d+)""")
    return pattern.find(url)?.groupValues?.get(1)?.toLongOrNull()
}

private val VIEWFINDER_SIZE = 250.dp
private const val OVERLAY_ALPHA = 0.6f
private const val BORDER_ALPHA = 0.8f
