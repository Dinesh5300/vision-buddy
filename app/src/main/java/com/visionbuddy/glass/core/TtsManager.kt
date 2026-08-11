package com.visionbuddy.glass.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Global Text-to-Speech engine for voice output.
 * All UI speaks through this manager so the whole app uses one voice engine.
 */
object TtsManager {

    private var tts: TextToSpeech? = null
    private var initialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Mute flag - when true, nothing is spoken (UI text still updates). */
    var muted = false

    private var lastSpokenText = ""

    fun init(context: Context, onReady: (() -> Unit)? = null) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.setSpeechRate(1.0f)
                    initialized = true
                    onReady?.invoke()
                }
            }
        } else if (initialized) {
            onReady?.invoke()
        }
    }

    fun isReady(): Boolean = initialized

    fun speak(text: String) {
        if (muted) return
        lastSpokenText = text
        mainHandler.post {
            if (initialized) {
                tts?.stop()
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vb-${System.currentTimeMillis()}")
            }
        }
    }

    fun lastSpoken(): String = lastSpokenText

    fun stop() {
        mainHandler.post { tts?.stop() }
    }

    fun shutdown() {
        mainHandler.post {
            tts?.stop()
            tts?.shutdown()
            tts = null
            initialized = false
        }
    }
}
