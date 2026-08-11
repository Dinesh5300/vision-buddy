package com.visionbuddy.glass.detect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.toBitmap
import androidx.core.content.ContextCompat
import com.visionbuddy.glass.R
import com.visionbuddy.glass.core.CameraXManager
import com.visionbuddy.glass.core.Detection
import com.visionbuddy.glass.core.TtsManager
import com.visionbuddy.glass.core.YoloDetector
import com.visionbuddy.glass.databinding.ActivityCameraBinding
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Detect Object mode:
 * Camera -> YOLO (TFLite) -> detected objects shown on screen -> spoken aloud (TTS).
 */
class ObjectDetectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraManager: CameraXManager

    private val busy = AtomicBoolean(false)
    @Volatile
    private var detector: YoloDetector? = null
    @Volatile
    private var detectorReady = false
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
            TtsManager.speak(getString(R.string.detect_note))
        }

        binding.btnSpeak.setOnClickListener {
            if (lastSignature.isNotBlank()) TtsManager.speak(lastSignature)
        }

        binding.btnMute.setOnClickListener {
            TtsManager.muted = !TtsManager.muted
            binding.btnMute.text =
                if (TtsManager.muted) getString(R.string.btn_unmute) else getString(R.string.btn_mute)
        }

        modelExecutor.execute {
            detector = YoloDetector(this)
            detectorReady = detector?.load() == true
            runOnUiThread {
                binding.txtStatus.text =
                    if (detectorReady) getString(R.string.detect_note)
                    else "YOLO model not found in assets. Add yolov5n-fp16-320.tflite."
            }
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

        val analyzer = ImageAnalysis.Analyzer { imageProxy ->
            val detector = detector ?: run {
                imageProxy.close()
                return@Analyzer
            }
            if (!busy.compareAndSet(false, true) || !detectorReady) {
                imageProxy.close()
                return@Analyzer
            }
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

        cameraManager.start(analyzer) { e ->
            e.printStackTrace()
            TtsManager.speak(getString(R.string.camera_error, e.message ?: ""))
        }
    }

    private fun onObjectsDetected(detections: List<Detection>) {
        val strong = detections.filter { it.confidence >= 0.5f }
        if (strong.isEmpty()) {
            if (lastSignature.isNotEmpty()) {
                lastSignature = ""
                binding.txtResult.text = getString(R.string.no_object_found)
                binding.txtStatus.text = getString(R.string.detect_note)
            }
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
            binding.txtStatus.text = getString(R.string.btn_speak)
            TtsManager.speak(spoken)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraManager.isInitialized) cameraManager.stop()
        detector?.close()
        modelExecutor.shutdown()
    }
}
