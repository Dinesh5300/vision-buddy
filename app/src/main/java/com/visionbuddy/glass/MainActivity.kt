package com.visionbuddy.glass

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.visionbuddy.glass.assist.AssistActivity
import com.visionbuddy.glass.core.TtsManager
import com.visionbuddy.glass.databinding.ActivityMainBinding
import com.visionbuddy.glass.detect.ObjectDetectorActivity
import com.visionbuddy.glass.read.TextReaderActivity
import com.visionbuddy.glass.sos.SOSActivity
import com.visionbuddy.glass.voice.VoiceCommandActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TtsManager.init(this)

        binding.btnStartAssist.setOnClickListener {
            startActivity(Intent(this, AssistActivity::class.java))
        }
        binding.btnReadText.setOnClickListener {
            startActivity(Intent(this, TextReaderActivity::class.java))
        }
        binding.btnDetectObject.setOnClickListener {
            startActivity(Intent(this, ObjectDetectorActivity::class.java))
        }
        binding.btnVoiceCommand.setOnClickListener {
            startActivity(Intent(this, VoiceCommandActivity::class.java))
        }
        binding.btnSos.setOnClickListener {
            startActivity(Intent(this, SOSActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TtsManager.shutdown()
    }
}
