package com.expensetracker.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrReceiptResult(
    val detectedAmount: BigDecimal?,
    val detectedMerchant: String?,
    val detectedDateMillis: Long?,
    val fullRawText: String
)

object ReceiptOcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:total|grand\s*total|balance\s*due|amount|net|paid)[:\s]*\$?\s*([0-9]+[.,][0-9]{2})"""),
        Pattern.compile("""(?i)\$\s*([0-9]+[.,][0-9]{2})"""),
        Pattern.compile("""([0-9]+[.,][0-9]{2})""")
    )

    suspend fun processImage(context: Context, imageUri: Uri): OcrReceiptResult {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        return extractFromInputImage(inputImage)
    }

    suspend fun processBitmap(bitmap: Bitmap): OcrReceiptResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return extractFromInputImage(inputImage)
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }

    private suspend fun extractFromInputImage(inputImage: InputImage): OcrReceiptResult {
        val visionText: Text = recognizer.process(inputImage).awaitTask()
        val rawText = visionText.text

        var detectedAmount: BigDecimal? = null
        var detectedMerchant: String? = null
        val detectedDateMillis: Long = DateTimeUtils.nowMillis()

        // Extract lines
        val lines = mutableListOf<String>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val trimmed = line.text.trim()
                if (trimmed.isNotEmpty()) {
                    lines.add(trimmed)
                }
            }
        }

        if (lines.isNotEmpty()) {
            val possibleMerchant = lines.firstOrNull { str ->
                !str.contains("receipt", ignoreCase = true) &&
                !str.contains("welcome", ignoreCase = true) &&
                !str.contains("tax", ignoreCase = true) &&
                str.length in 3..35
            }
            detectedMerchant = possibleMerchant ?: lines.first()
        }

        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(rawText)
            var lastFound: String? = null
            while (matcher.find()) {
                lastFound = matcher.group(1)
            }
            if (lastFound != null) {
                try {
                    val cleanAmount = lastFound.replace(",", "")
                    val parsed = BigDecimal(cleanAmount)
                    if (parsed > BigDecimal.ZERO) {
                        detectedAmount = parsed
                        break
                    }
                } catch (e: Exception) {
                    // Ignore parse error
                }
            }
        }

        return OcrReceiptResult(
            detectedAmount = detectedAmount,
            detectedMerchant = detectedMerchant,
            detectedDateMillis = detectedDateMillis,
            fullRawText = rawText
        )
    }
}
