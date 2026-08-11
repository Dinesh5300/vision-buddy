package com.visionbuddy.glass.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.visionbuddy.glass.R
import com.visionbuddy.glass.assist.AssistActivity
import com.visionbuddy.glass.core.TtsManager
import com.visionbuddy.glass.databinding.ActivityVoiceBinding
import com.visionbuddy.glass.detect.ObjectDetectorActivity
import com.visionbuddy.glass.read.TextReaderActivity
import com.visionbuddy.glass.sos.SOSActivity

/**
 * Voice Command mode:
 * User says a command -> app recognizes it -> runs the matching action.
 *
 *  "Read this"        -> Camera + OCR
 *  "Detect object"    -> Camera + YOLO
 *  "Start assistance" -> Combined mode
 *  "Help" / "SOS"     -> SOS screen
 */
class VoiceCommandActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceBinding
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                TtsManager.speak(getString(R.string.permission_mic_denied))
                Toast.makeText(this, R.string.permission_mic_denied, Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TtsManager.init(this) {
            TtsManager.speak(getString(R.string.voice_prompt))
        }

        binding.btnMic.setOnClickListener {
            if (isListening) stopListening() else startListening()
        }

        binding.chipRead.setOnClickListener { runAction(Action.READ_TEXT) }
        binding.chipDetect.setOnClickListener { runAction(Action.DETECT_OBJECT) }
        binding.chipSos.setOnClickListener { runAction(Action.SOS) }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            TtsManager.speak(getString(R.string.voice_no_service))
            Toast.makeText(this, R.string.voice_no_service, Toast.LENGTH_LONG).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val recognizer = speechRecognizer ?: createRecognizer()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        isListening = true
        binding.btnMic.isEnabled = false
        binding.txtStatus.text = getString(R.string.voice_listening)
        binding.txtHeard.text = ""
        recognizer.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        binding.btnMic.isEnabled = true
        binding.txtStatus.text = getString(R.string.voice_tap_to_speak)
        speechRecognizer?.stopListening()
    }

    private fun createRecognizer(): SpeechRecognizer {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                binding.btnMic.isEnabled = true
                binding.txtStatus.text = getString(R.string.voice_tap_to_speak)
            }

            override fun onError(error: Int) {
                isListening = false
                binding.btnMic.isEnabled = true
                binding.txtStatus.text = getString(R.string.voice_tap_to_speak)
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    binding.txtHeard.text = getString(R.string.voice_not_recognized)
                    TtsManager.speak(getString(R.string.voice_not_recognized))
                }
            }

            override fun onResults(results: Bundle?) {
                val transcript =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        ?: return
                binding.txtHeard.text = getString(R.string.voice_heard, transcript)
                TtsManager.speak(transcript)
                runAction(Action.from(transcript))
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer = recognizer
        return recognizer
    }

    private enum class Action {
        READ_TEXT, DETECT_OBJECT, ASSIST, SOS, HOME, NONE;

        companion object {
            fun from(transcript: String): Action {
                val t = transcript.lowercase()
                return when {
                    "read" in t || "text" in t -> READ_TEXT
                    "object" in t || "detect" in t || "what" in t || "see" in t -> DETECT_OBJECT
                    "assist" in t || "start" in t -> ASSIST
                    "sos" in t || "help" in t || "emergency" in t -> SOS
                    "home" in t || "back" in t || "menu" in t -> HOME
                    else -> NONE
                }
            }
        }
    }

    private fun runAction(action: Action) {
        when (action) {
            Action.READ_TEXT -> {
                TtsManager.speak("Reading text")
                startActivity(Intent(this, TextReaderActivity::class.java))
            }
            Action.DETECT_OBJECT -> {
                TtsManager.speak("Detecting objects")
                startActivity(Intent(this, ObjectDetectorActivity::class.java))
            }
            Action.ASSIST -> {
                TtsManager.speak("Starting assistance")
                startActivity(Intent(this, AssistActivity::class.java))
            }
            Action.SOS -> {
                TtsManager.speak("Opening SOS")
                startActivity(Intent(this, SOSActivity::class.java))
            }
            Action.HOME -> finish()
            Action.NONE -> {
                binding.txtHeard.text = getString(R.string.voice_not_recognized)
                TtsManager.speak(getString(R.string.voice_not_recognized))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
