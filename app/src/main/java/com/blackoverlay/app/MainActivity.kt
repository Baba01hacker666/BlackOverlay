package com.blackoverlay.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.blackoverlay.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var suppressSliderCallback = false

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshUi()
            if (OverlayService.hasOverlayPermission(this) && Prefs.isRunning(this)) {
                OverlayService.start(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.opacitySlider.valueFrom = 0f
        binding.opacitySlider.valueTo = 100f
        binding.opacitySlider.stepSize = 1f

        binding.opacitySlider.addOnChangeListener { _, value, fromUser ->
            if (suppressSliderCallback) return@addOnChangeListener
            val v = value.toInt()
            Prefs.setOpacity(this, v)
            updateOpacityLabel(v)
            if (Prefs.isRunning(this) && fromUser) {
                OverlayService.start(this, v)
            }
        }

        binding.toggleButton.setOnClickListener {
            if (!OverlayService.hasOverlayPermission(this)) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            if (Prefs.isRunning(this)) {
                OverlayService.stop(this)
            } else {
                OverlayService.start(this, binding.opacitySlider.value.toInt())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
                }
            }
            refreshUi()
        }

        binding.grantPermissionButton.setOnClickListener { requestOverlayPermission() }

        binding.preset25.setOnClickListener { setOpacityPreset(25) }
        binding.preset50.setOnClickListener { setOpacityPreset(50) }
        binding.preset75.setOnClickListener { setOpacityPreset(75) }
        binding.preset90.setOnClickListener { setOpacityPreset(90) }

        binding.startOnBootSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.setStartOnBoot(this, checked)
        }
        binding.blockTouchSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.setBlockTouch(this, checked)
            if (Prefs.isRunning(this)) OverlayService.start(this)
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun setOpacityPreset(value: Int) {
        suppressSliderCallback = true
        binding.opacitySlider.value = value.toFloat()
        suppressSliderCallback = false
        Prefs.setOpacity(this, value)
        updateOpacityLabel(value)
        if (Prefs.isRunning(this)) OverlayService.start(this, value)
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, R.string.overlay_permission_hint, Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun refreshUi() {
        val opacity = Prefs.getOpacity(this)
        suppressSliderCallback = true
        binding.opacitySlider.value = opacity.toFloat()
        suppressSliderCallback = false
        updateOpacityLabel(opacity)

        val hasPermission = OverlayService.hasOverlayPermission(this)
        binding.permissionCard.visibility =
            if (hasPermission) android.view.View.GONE else android.view.View.VISIBLE

        val running = Prefs.isRunning(this) && hasPermission
        binding.toggleButton.text =
            getString(if (running) R.string.stop_overlay else R.string.start_overlay)
        binding.statusText.text =
            getString(if (running) R.string.status_on else R.string.status_off, opacity)
        binding.statusDot.setBackgroundResource(
            if (running) R.drawable.dot_on else R.drawable.dot_off
        )

        binding.startOnBootSwitch.isChecked = Prefs.startOnBoot(this)
        binding.blockTouchSwitch.isChecked = Prefs.blockTouch(this)
    }

    private fun updateOpacityLabel(value: Int) {
        binding.opacityValue.text = getString(R.string.opacity_percent, value)
    }
}
