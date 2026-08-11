package com.visionbuddy.glass.core

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Small helper that binds CameraX (back camera) to a PreviewView with an
 * ImageAnalysis pipeline. Every activity with a camera screen uses this.
 */
class CameraXManager(
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    private val analysisExecutor: Executor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    fun start(analyzer: ImageAnalysis.Analyzer, onError: (Exception) -> Unit = {}) {
        val providerFuture = ProcessCameraProvider.getInstance(lifecycleOwner)
        providerFuture.addListener(
            {
                try {
                    val provider = providerFuture.get()
                    cameraProvider = provider
                    provider.unbindAll()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, analyzer) }

                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    onError(e)
                }
            },
            ContextCompat.getMainExecutor(lifecycleOwner)
        )
    }

    fun stop() {
        cameraProvider?.unbindAll()
    }
}
