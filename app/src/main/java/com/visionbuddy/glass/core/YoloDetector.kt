package com.visionbuddy.glass.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** One detected object. */
data class Detection(
    val label: String,
    val confidence: Float,
    /** Box in the coordinates of the original (input) bitmap. */
    val box: RectF
)

/**
 * YOLO object detector running a TFLite model on-device.
 *
 * Works with common YOLO TFLite exports:
 *  - YOLOv5: output (1, N, 85)  or (1, 85, N)   -> xywh + objectness + 80 classes
 *  - YOLOv8: output (1, N, 84)  or (1, 84, N)   -> xywh + 80 classes (no objectness)
 */
class YoloDetector(context: Context, private val modelName: String = "yolov5n-fp16-320.tflite") {

    private val context: Context = context.applicationContext
    private var interpreter: Interpreter? = null
    private var inputSize = 320
    private val labels: List<String>

    private val confThreshold = 0.45f
    private val iouThreshold = 0.45f

    // Reused buffers so detection does not allocate per frame.
    private var inputBuffer: ByteBuffer? = null
    private var outputBuffer: FloatBuffer? = null
    private var pixelCache: IntArray? = null

    init {
        labels = context.assets.open("coco_labels.txt").bufferedReader().readLines()
            .filter { it.isNotBlank() }
    }

    /** Loads the TFLite model from assets. Returns false if the model file is missing. */
    fun load(): Boolean {
        return try {
            interpreter = Interpreter(loadModelFromAssets(modelName))
            val shape = interpreter!!.getInputTensor(0).shape()
            inputSize = shape[1]
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadModelFromAssets(name: String): ByteBuffer {
        val bytes = context.assets.open(name).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.rewind()
        return buffer
    }

    /**
     * Runs detection on the bitmap. The bitmap is resized to the model input size.
     * Returns detections sorted by confidence (highest first).
     */
    fun detect(bitmap: Bitmap): List<Detection> {
        val interpreter = interpreter ?: return emptyList()
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val input = inputBuffer ?: ByteBuffer
            .allocateDirect(4 * inputSize * inputSize * 3)
            .order(ByteOrder.nativeOrder())
            .also { inputBuffer = it }
        input.rewind()

        val pixels = (pixelCache ?: IntArray(inputSize * inputSize).also { pixelCache = it })
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            input.putFloat(((pixel shr 16) and 0xFF) / 255f)
            input.putFloat(((pixel shr 8) and 0xFF) / 255f)
            input.putFloat((pixel and 0xFF) / 255f)
        }
        input.rewind()

        val outShape = interpreter.getOutputTensor(0).shape()
        val rows = outShape[1]
        val cols = outShape[2]

        val output = outputBuffer
            ?: FloatBuffer.allocate(rows * cols).also { outputBuffer = it }
        output.rewind()

        interpreter.run(input, output)
        output.rewind()

        // Handle both row-major (1, N, 85/84) and column-major (1, 84/85, N) exports.
        val transposed = (rows == 84 || rows == 85) && cols > rows
        val n = if (transposed) cols else rows
        val stride = if (transposed) rows else cols

        val raw = mutableListOf<RawDetection>()
        for (i in 0 until n) {
            val base = i * stride
            val x = output.get(base)
            val y = output.get(base + 1)
            val w = output.get(base + 2)
            val h = output.get(base + 3)

            var bestScore = -1f
            var bestClass = 0
            val classStart = if (stride == 85) 5 else 4
            for (c in classStart until stride) {
                val score = output.get(base + c)
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c - classStart
                }
            }
            var objConf = 1f
            if (stride == 85) {
                objConf = output.get(base + 4)
            }
            val confidence = objConf * bestScore
            if (confidence < confThreshold) continue

            val left = (x - w / 2) * (bitmap.width / inputSize.toFloat())
            val top = (y - h / 2) * (bitmap.height / inputSize.toFloat())
            val right = (x + w / 2) * (bitmap.width / inputSize.toFloat())
            val bottom = (y + h / 2) * (bitmap.height / inputSize.toFloat())
            raw.add(
                RawDetection(
                    label = labels.getOrElse(bestClass) { "unknown" },
                    confidence = confidence,
                    box = RectF(left, top, right, bottom)
                )
            )
        }

        return nms(raw)
    }

    private data class RawDetection(
        val label: String,
        val confidence: Float,
        val box: RectF
    )

    /** Standard Non-Maximum Suppression, keeps the strongest non-overlapping boxes. */
    private fun nms(detections: List<RawDetection>): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }
        val kept = mutableListOf<RawDetection>()
        for (candidate in sorted) {
            var overlap = false
            for (existing in kept) {
                if (iou(candidate.box, existing.box) > iouThreshold) {
                    overlap = true
                    break
                }
            }
            if (!overlap) kept.add(candidate)
        }
        return kept.map {
            Detection(it.label, it.confidence, it.box)
        }
    }

    private fun iou(a: RectF, b: RectF): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)
        val inter = maxOf(0f, right - left) * maxOf(0f, bottom - top)
        if (inter <= 0f) return 0f
        val union = (a.width() * a.height()) + (b.width() * b.height()) - inter
        return inter / union
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
