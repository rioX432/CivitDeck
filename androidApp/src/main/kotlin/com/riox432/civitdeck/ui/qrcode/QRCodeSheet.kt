package com.riox432.civitdeck.ui.qrcode

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeSheet(
    modelId: Long,
    modelName: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val civitaiUrl = "https://civitai.com/models/$modelId"
    val qrBitmap = remember(civitaiUrl) { QRCodeGenerator.generate(civitaiUrl) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        QRCodeSheetContent(
            modelName = modelName,
            qrBitmap = qrBitmap,
            civitaiUrl = civitaiUrl,
            onShare = { shareQRCode(context, qrBitmap, modelName) },
        )
    }
}

@Composable
private fun QRCodeSheetContent(
    modelName: String,
    qrBitmap: Bitmap,
    civitaiUrl: String,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "QR Code",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        QRCardContent(
            modelName = modelName,
            qrBitmap = qrBitmap,
            civitaiUrl = civitaiUrl,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        ShareButton(onShare = onShare)
        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

@Composable
private fun QRCardContent(
    modelName: String,
    qrBitmap: Bitmap,
    civitaiUrl: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.card))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = modelName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "QR code for $modelName",
            modifier = Modifier
                .size(QR_IMAGE_SIZE)
                .clip(RoundedCornerShape(Spacing.sm))
                .background(MaterialTheme.colorScheme.surface),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = civitaiUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShareButton(onShare: () -> Unit) {
    Button(
        onClick = onShare,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text("Share QR Code")
        }
    }
}

private fun shareQRCode(context: Context, bitmap: Bitmap, modelName: String) {
    val cacheDir = File(context.cacheDir, "qr_codes")
    cacheDir.mkdirs()
    val file = File(cacheDir, "qr_${modelName.take(MAX_FILE_NAME_LENGTH).replace(Regex("[^a-zA-Z0-9]"), "_")}.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Check out this model on CivitAI: $modelName")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
}

private val QR_IMAGE_SIZE = 240.dp
private const val QUALITY = 100
private const val MAX_FILE_NAME_LENGTH = 30
