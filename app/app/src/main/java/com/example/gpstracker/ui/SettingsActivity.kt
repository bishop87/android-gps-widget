package com.example.gpstracker.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gpstracker.R
import com.example.gpstracker.databinding.ActivitySettingsBinding
import com.example.gpstracker.viewmodel.SettingsViewModel

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private var currentColor: Int = 0xCC1565C0.toInt()
    private var currentOverlayColor: Int = 0xCC1565C0.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_settings)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.settings.observe(this) { settings ->
            binding.etDeviceName.setText(settings.deviceName)
            binding.etApiUrl.setText(settings.apiUrl)
            binding.etUsername.setText(settings.username)
            binding.etPassword.setText(settings.password)
            binding.etInterval.setText(settings.trackingIntervalSec.toString())
            
            currentColor = settings.widgetBackgroundColor
            binding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentColor)

            currentOverlayColor = settings.overlayBackgroundColor
            binding.viewOverlayColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentOverlayColor)
        }

        viewModel.saved.observe(this) { saved ->
            if (saved == true) {
                Toast.makeText(this, "Impostazioni salvate", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.validationError.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnPickColor.setOnClickListener {
            showColorPickerDialog()
        }

        binding.btnPickOverlayColor.setOnClickListener {
            showOverlayColorPickerDialog()
        }

        binding.btnSave.setOnClickListener {
            val intervalText = binding.etInterval.text.toString()
            val intervalSec = intervalText.toIntOrNull() ?: 300

            viewModel.saveSettings(
                deviceName = binding.etDeviceName.text.toString().trim(),
                apiUrl = binding.etApiUrl.text.toString().trim(),
                username = binding.etUsername.text.toString().trim(),
                password = binding.etPassword.text.toString(),
                trackingIntervalSec = intervalSec,
                widgetBackgroundColor = currentColor,
                overlayBackgroundColor = currentOverlayColor
            )
        }
    }

    private fun showColorPickerDialog() {
        com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
            .setTitle(getString(R.string.title_settings))
            .setPreferenceName("WidgetColorPickerDialog")
            .setPositiveButton("OK",
                com.skydoves.colorpickerview.listeners.ColorEnvelopeListener { envelope, _ ->
                    currentColor = envelope.color
                    binding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentColor)
                })
            .setNegativeButton("Annulla") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .attachAlphaSlideBar(true) // Abilita lo slider trasparenza (Alpha)
            .attachBrightnessSlideBar(true) // Abilita lo slider luminosità
            .setBottomSpace(12) // padding inferiore del dialog
            .show()
    }

    private fun showOverlayColorPickerDialog() {
        com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
            .setTitle("Scegli Colore Overlay")
            .setPreferenceName("OverlayColorPickerDialog")
            .setPositiveButton("OK",
                com.skydoves.colorpickerview.listeners.ColorEnvelopeListener { envelope, _ ->
                    currentOverlayColor = envelope.color
                    binding.viewOverlayColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentOverlayColor)
                })
            .setNegativeButton("Annulla") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
