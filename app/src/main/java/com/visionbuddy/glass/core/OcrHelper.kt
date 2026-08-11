package com.visionbuddy.glass.core

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions

/**
 * Wrapper around Google ML Kit Text Recognition (bundled model, works offline).
 */
object OcrHelper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Recognizes all text in the image and returns the trimmed text on [onResult].
     * Returns an empty string if no text was found or on failure.
     */
    fun recognize(image: InputImage, onResult: (String) -> Unit) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text?.trim().orEmpty())
            }
            .addOnFailureListener {
                onResult("")
            }
    }

    fun close() {
        try {
            recognizer.close()
        } catch (_: Exception) {
        }
    }
}
