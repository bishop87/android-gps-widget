package com.bishop87.gpstracker.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.bishop87.gpstracker.R
import com.bishop87.gpstracker.databinding.ActivitySettingsBinding
import com.bishop87.gpstracker.viewmodel.SettingsViewModel

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
            if (binding.etDeviceName.text.toString() != settings.deviceName) binding.etDeviceName.setText(settings.deviceName)
            if (binding.etApiUrl.text.toString() != settings.apiUrl) binding.etApiUrl.setText(settings.apiUrl)
            if (binding.etUsername.text.toString() != settings.username) binding.etUsername.setText(settings.username)
            if (binding.etPassword.text.toString() != settings.password) binding.etPassword.setText(settings.password)
            if (binding.etInterval.text.toString() != settings.trackingIntervalSec.toString()) binding.etInterval.setText(settings.trackingIntervalSec.toString())
            if (binding.etMapApiUrl.text.toString() != settings.mapApiUrl) binding.etMapApiUrl.setText(settings.mapApiUrl)

            currentColor = settings.widgetBackgroundColor
            binding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentColor)

            currentOverlayColor = settings.overlayBackgroundColor
            binding.viewOverlayColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentOverlayColor)
        }

        viewModel.saved.observe(this) { saved ->
            if (saved == true) {
                // Toast rimosso per evitare spam a ogni tasto premuto: Toast.makeText(this, "Impostazioni salvate", Toast.LENGTH_SHORT).show()
                // Nessun finish() per l'auto-save
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

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Listener per auto-salvataggio ad ogni modifica del testo
        binding.etDeviceName.addTextChangedListener { autoSave() }
        binding.etApiUrl.addTextChangedListener { autoSave() }
        binding.etMapApiUrl.addTextChangedListener { autoSave() }
        binding.etUsername.addTextChangedListener { autoSave() }
        binding.etPassword.addTextChangedListener { autoSave() }
        binding.etInterval.addTextChangedListener { autoSave() }
    }

    private var isUpdatingUI = true // Serve per ignorare il primo trigger durante l'observe dei livedata

    private fun autoSave() {
        if (isUpdatingUI) return

        val intervalText = binding.etInterval.text.toString()
        val intervalSec = intervalText.toIntOrNull() ?: 300

        viewModel.saveSettings(
            deviceName = binding.etDeviceName.text.toString().trim(),
            apiUrl = binding.etApiUrl.text.toString().trim(),
            username = binding.etUsername.text.toString().trim(),
            password = binding.etPassword.text.toString(),
            trackingIntervalSec = intervalSec,
            widgetBackgroundColor = currentColor,
            overlayBackgroundColor = currentOverlayColor,
            mapApiUrl = binding.etMapApiUrl.text.toString().trim()
        )
    }

    override fun onStart() {
        super.onStart()
        // Dopo un primo delay/observe, abilitiamo l'autosave in modo che i setValue iniziali non lo scatenino
        binding.root.post { isUpdatingUI = false }
    }

    private fun showColorPickerDialog() {
        com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
            .setTitle(getString(R.string.title_settings))
            .setPreferenceName("WidgetColorPickerDialog")
            .setPositiveButton("OK",
                com.skydoves.colorpickerview.listeners.ColorEnvelopeListener { envelope, _ ->
                    currentColor = envelope.color
                    binding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentColor)
                    autoSave()
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
                    autoSave()
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
