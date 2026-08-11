package com.visionbuddy.glass.assist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.toBitmap
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.visionbuddy.glass.R
import com.visionbuddy.glass.core.CameraXManager
import com.visionbuddy.glass.core.Detection
import com.visionbuddy.glass.core.OcrHelper
import com.visionbuddy.glass.core.TtsManager
import com.visionbuddy.glass.core.YoloDetector
import com.visionbuddy.glass.databinding.ActivityCameraBinding
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Start Assistance mode:
 * Runs OCR and YOLO together on alternating frames and announces anything new:
 * text or objects in front of the user.
 */
class AssistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraManager: CameraXManager

    private val busy = AtomicBoolean(false)
    private val frameCounter = AtomicInteger(0)

    @Volatile
    private var detector: YoloDetector? = null
    @Volatile
    private var detectorReady = false

    private var lastText = ""
    private var lastSignature = ""

    private val modelExecutor = Executors.newSingleThreadExecutor()

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
            TtsManager.speak(getString(R.string.assist_note))
        }

        binding.btnSpeak.setOnClickListener {
            if (lastText.isNotBlank()) {
                TtsManager.speak(lastText)
            } else if (lastSignature.isNotBlank()) {
                TtsManager.speak(lastSignature)
            }
        }

        binding.btnMute.setOnClickListener {
            TtsManager.muted = !TtsManager.muted
            binding.btnMute.text =
                if (TtsManager.muted) getString(R.string.btn_unmute) else getString(R.string.btn_mute)
        }

        modelExecutor.execute {
            detector = YoloDetector(this)
            detectorReady = detector?.load() == true
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
        binding.txtStatus.text = getString(R.string.assist_note)

        val analyzer = ImageAnalysis.Analyzer { imageProxy ->
            if (!busy.compareAndSet(false, true)) {
                imageProxy.close()
                return@Analyzer
            }
            val mediaImage = imageProxy.image
            val rotation = imageProxy.imageInfo.rotationDegrees

            val frame = frameCounter.getAndIncrement()
            val detector = detector

            if (frame % 2 == 0 || detector == null || !detectorReady) {
                // Frame -> OCR
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
            } else {
                // Frame -> YOLO
                try {
                    val bitmap = imageProxy.toBitmap()
                    val results = detector.detect(bitmap)
                    runOnUiThread { onObjectsDetected(results) }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    busy.set(false)
                    imageProxy.close()
                }
            }
        }

        cameraManager.start(analyzer) { e ->
            e.printStackTrace()
            TtsManager.speak(getString(R.string.camera_error, e.message ?: ""))
        }
    }

    private fun onTextDetected(text: String) {
        if (text.isBlank()) {
            lastText = ""
            return
        }
        if (text != lastText) {
            lastText = text
            binding.txtResult.text = text
            TtsManager.speak(text)
        }
    }

    private fun onObjectsDetected(detections: List<Detection>) {
        val strong = detections.filter { it.confidence >= 0.55f }
        if (strong.isEmpty()) {
            lastSignature = ""
            return
        }
        val top = strong.sortedByDescending { it.confidence }.take(3)
        val signature = top.joinToString(",") { it.label }
        if (signature != lastSignature) {
            lastSignature = signature
            binding.txtResult.text = top.joinToString("\n") {
                "${it.label} (${(it.confidence * 100).toInt()}%)"
            }
            val spoken = top.joinToString(" and ") { it.label }
            TtsManager.speak(spoken)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraManager.isInitialized) cameraManager.stop()
        detector?.close()
        OcrHelper.close()
        modelExecutor.shutdown()
    }
}
