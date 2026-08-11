package com.visionbuddy.glass.sos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.visionbuddy.glass.R
import com.visionbuddy.glass.core.TtsManager
import com.visionbuddy.glass.databinding.ActivitySosBinding

/**
 * SOS screen (Phase 1 placeholder).
 * Phase 2 will connect this to Firebase: emergency contacts, SMS and live location.
 */
class SOSActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TtsManager.init(this) {
            TtsManager.speak(getString(R.string.sos_press))
        }

        binding.btnSos.setOnClickListener {
            confirmSos()
        }

        binding.btnCall112.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmSos() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_sos))
            .setMessage(getString(R.string.sos_placeholder))
            .setPositiveButton(R.string.btn_sos) { _, _ -> activateSos() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun activateSos() {
        vibrate()
        TtsManager.speak(getString(R.string.sos_activated))
        Toast.makeText(this, R.string.sos_activated, Toast.LENGTH_LONG).show()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), -1)
        )
    }
}
