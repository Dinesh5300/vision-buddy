package com.visionbuddy.glass.read

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.visionbuddy.glass.R
import com.visionbuddy.glass.core.CameraXManager
import com.visionbuddy.glass.core.OcrHelper
import com.visionbuddy.glass.core.TtsManager
import com.visionbuddy.glass.databinding.ActivityCameraBinding
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Read Text mode:
 * Camera -> ML Kit OCR -> detected text shown on screen -> spoken aloud (TTS).
 */
class TextReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraManager: CameraXManager

    private val busy = AtomicBoolean(false)
    private var lastText = ""
    private var lastResult = ""

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else {
                TtsManager.speak(getString(R.string.permission_camera_denied))
                Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtResult.text = getString(R.string.status_initializing)
        binding.btnSpeak.text = getString(R.string.btn_speak)

        TtsManager.init(this) {
            TtsManager.speak(getString(R.string.ocr_note))
        }

        binding.btnSpeak.setOnClickListener {
            if (lastResult.isNotBlank()) TtsManager.speak(lastResult)
        }

        binding.btnMute.setOnClickListener {
            TtsManager.muted = !TtsManager.muted
            binding.btnMute.text =
                if (TtsManager.muted) getString(R.string.btn_unmute) else getString(R.string.btn_mute)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraManager = CameraXManager(this, binding.previewView)
        binding.txtStatus.text = getString(R.string.ocr_note)

        val analyzer = ImageAnalysis.Analyzer { imageProxy ->
            if (!busy.compareAndSet(false, true)) {
                imageProxy.close()
                return@Analyzer
            }
            val mediaImage = imageProxy.image
            val rotation = imageProxy.imageInfo.rotationDegrees
            if (mediaImage == null) {
                busy.set(false)
                imageProxy.close()
                return@Analyzer
            }
            val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
            OcrHelper.recognize(inputImage) { text ->
                runOnUiThread { onTextDetected(text) }
                busy.set(false)
                imageProxy.close()
            }
        }

        cameraManager.start(analyzer) { e ->
            e.printStackTrace()
            TtsManager.speak(getString(R.string.camera_error, e.message ?: ""))
        }
    }

    private fun onTextDetected(text: String) {
        if (text.isBlank()) {
            if (lastText.isNotEmpty()) {
                lastText = ""
                binding.txtResult.text = getString(R.string.no_text_found)
                binding.txtStatus.text = getString(R.string.ocr_note)
            }
            return
        }
        if (text != lastText) {
            lastText = text
            lastResult = text
            binding.txtResult.text = text
            binding.txtStatus.text = getString(R.string.btn_speak)
            TtsManager.speak(text)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraManager.isInitialized) cameraManager.stop()
        OcrHelper.close()
    }
}
