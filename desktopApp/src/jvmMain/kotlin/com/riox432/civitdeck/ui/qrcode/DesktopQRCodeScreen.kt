package com.riox432.civitdeck.ui.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.Elevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun DesktopQRCodeScreen(
    onBack: () -> Unit,
    onModelIdFound: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlInput by remember { mutableStateOf("") }
    var generatedQR by remember { mutableStateOf<ImageBitmap?>(null) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        QRCodeTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // Generate section
            QRGenerateSection(
                urlInput = urlInput,
                onUrlChange = { urlInput = it },
                onGenerate = {
                    if (urlInput.isNotBlank()) {
                        generatedQR = generateQRBitmap(urlInput)
                    }
                },
                generatedQR = generatedQR,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Scan section
            QRScanSection(
                scanResult = scanResult,
                scanError = scanError,
                onPickFile = {
                    scope.launch {
                        val result = pickAndDecodeQR()
                        if (result != null) {
                            scanResult = result
                            scanError = null
                            extractModelId(result)?.let { onModelIdFound(it) }
                        } else {
                            scanError = "Could not decode QR code from image"
                            scanResult = null
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun QRCodeTopBar(onBack: () -> Unit) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "QR Code",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QRGenerateSection(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onGenerate: () -> Unit,
    generatedQR: ImageBitmap?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = SECTION_MAX_WIDTH),
    ) {
        Text("Generate QR Code", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                placeholder = { Text("Enter CivitAI model URL or ID") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onGenerate) {
                Icon(Icons.Default.QrCode, contentDescription = "Generate QR code")
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Generate")
            }
        }

        if (generatedQR != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(CornerRadius.card))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    bitmap = generatedQR,
                    contentDescription = "Generated QR code",
                    modifier = Modifier
                        .size(QR_IMAGE_SIZE)
                        .clip(RoundedCornerShape(Spacing.sm))
                        .background(MaterialTheme.colorScheme.surface),
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = urlInput,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QRScanSection(
    scanResult: String?,
    scanError: String?,
    onPickFile: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = SECTION_MAX_WIDTH),
    ) {
        Text("Scan QR Code from Image", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedButton(onClick = onPickFile) {
            Icon(Icons.Default.FileOpen, contentDescription = "Open file")
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text("Open QR Code Image")
        }

        if (scanResult != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Decoded: $scanResult",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (scanError != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = scanError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * Generate a QR code bitmap using ZXing.
 */
private fun generateQRBitmap(content: String): ImageBitmap {
    val url = if (content.all { it.isDigit() }) {
        "https://civitai.com/models/$content"
    } else {
        content
    }
    val hints = mapOf(
        com.google.zxing.EncodeHintType.MARGIN to 1,
        com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8",
    )
    val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
        url,
        com.google.zxing.BarcodeFormat.QR_CODE,
        QR_SIZE,
        QR_SIZE,
        hints,
    )
    val image = BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until QR_SIZE) {
        for (y in 0 until QR_SIZE) {
            image.setRGB(x, y, if (bitMatrix.get(x, y)) BLACK_RGB else WHITE_RGB)
        }
    }
    return image.toComposeImageBitmap()
}

/**
 * Open a file chooser, pick an image, and decode a QR code from it.
 */
private suspend fun pickAndDecodeQR(): String? = withContext(Dispatchers.IO) {
    val chooser = JFileChooser().apply {
        fileFilter = FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "bmp", "gif")
        dialogTitle = "Select QR Code Image"
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return@withContext null

    try {
        val image = ImageIO.read(chooser.selectedFile) ?: return@withContext null
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)
        val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = com.google.zxing.BinaryBitmap(
            com.google.zxing.common.HybridBinarizer(source),
        )
        val decoded = com.google.zxing.MultiFormatReader().decode(binaryBitmap)
        decoded.text
    } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
        null
    }
}

/**
 * Extract a model ID from a CivitAI URL.
 */
private fun extractModelId(url: String): Long? {
    val regex = Regex("""civitai\.com/models/(\d+)""")
    return regex.find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()
}

private val QR_IMAGE_SIZE = 240.dp
private val SECTION_MAX_WIDTH = 500.dp
private const val QR_SIZE = 512
private const val BLACK_RGB = 0xFF000000.toInt()
private const val WHITE_RGB = 0xFFFFFFFF.toInt()
