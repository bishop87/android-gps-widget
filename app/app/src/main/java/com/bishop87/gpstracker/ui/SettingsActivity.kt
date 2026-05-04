package com.bishop87.gpstracker.ui

import android.graphics.Bitmap
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.bishop87.gpstracker.R
import com.bishop87.gpstracker.databinding.ActivitySettingsBinding
import com.bishop87.gpstracker.viewmodel.SettingsViewModel

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private var currentColor: Int = 0xCC1565C0.toInt()
    private var currentOverlayColor: Int = 0xCC1565C0.toInt()
    private var currentOverlayBorderColor: Int = android.graphics.Color.TRANSPARENT

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val success = viewModel.applySettingsFromJson(result.contents)
            if (success) {
                Toast.makeText(this, "Impostazioni importate con successo", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Errore durante l'importazione (QR non valido)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            decodeQrFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
            if (!binding.etInterval.hasFocus() && binding.etInterval.text.toString() != settings.trackingIntervalSec.toString()) {
                binding.etInterval.setText(settings.trackingIntervalSec.toString())
            }
            if (binding.etMapApiUrl.text.toString() != settings.mapApiUrl) binding.etMapApiUrl.setText(settings.mapApiUrl)

            currentColor = settings.widgetBackgroundColor
            binding.viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentColor)

            currentOverlayColor = settings.overlayBackgroundColor
            binding.viewOverlayColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentOverlayColor)

            currentOverlayBorderColor = settings.overlayBorderColor
            binding.viewOverlayBorderColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentOverlayBorderColor)

            if (binding.etOverlayBorderWidth.text.toString() != settings.overlayBorderWidth.toString()) {
                binding.etOverlayBorderWidth.setText(settings.overlayBorderWidth.toString())
            }
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

        binding.btnPickOverlayBorderColor.setOnClickListener {
            showOverlayBorderColorPickerDialog()
        }

        binding.btnBack.setOnClickListener {
            binding.root.clearFocus()
            finish()
        }

        binding.btnGenerateQr.setOnClickListener {
            binding.root.clearFocus()
            generateAndSaveQrCode()
        }

        binding.btnScanQr.setOnClickListener {
            barcodeLauncher.launch(ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scansiona QR Impostazioni")
                setCameraId(0) // Usare fotocamera posteriore
                setBeepEnabled(false)
                setBarcodeImageEnabled(false)
            })
        }

        binding.btnLoadQr.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Listener per auto-salvataggio ad ogni modifica del testo
        binding.etDeviceName.addTextChangedListener { autoSave() }
        binding.etApiUrl.addTextChangedListener { autoSave() }
        binding.etMapApiUrl.addTextChangedListener { autoSave() }
        binding.etUsername.addTextChangedListener { autoSave() }
        binding.etPassword.addTextChangedListener { autoSave() }
        binding.etInterval.addTextChangedListener { autoSave() }
        binding.etOverlayBorderWidth.addTextChangedListener { autoSave() }

        binding.etInterval.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.etInterval.text.toString()
                val value = text.toIntOrNull() ?: 0
                if (value < 5) {
                    binding.etInterval.setText("5")
                }
                autoSave()
            }
        }
    }

    private var isUpdatingUI = true // Serve per ignorare il primo trigger durante l'observe dei livedata

    private fun autoSave() {
        if (isUpdatingUI) return

        val intervalText = binding.etInterval.text.toString()
        val intervalSec = intervalText.toIntOrNull() ?: 0

        if (intervalSec < 5 && binding.etInterval.hasFocus()) {
            // Se sta digitando ed è sotto a 5 (es. campo vuoto o ha scritto "2"), sospendi il salvataggio
            return
        }

        val finalInterval = if (intervalSec < 5) 5 else intervalSec

        val overlayBorderWidthStr = binding.etOverlayBorderWidth.text.toString()
        val overlayBorderWidth = overlayBorderWidthStr.toIntOrNull() ?: 0

        viewModel.saveSettings(
            deviceName = binding.etDeviceName.text.toString().trim(),
            apiUrl = binding.etApiUrl.text.toString().trim(),
            username = binding.etUsername.text.toString().trim(),
            password = binding.etPassword.text.toString(),
            trackingIntervalSec = finalInterval,
            widgetBackgroundColor = currentColor,
            overlayBackgroundColor = currentOverlayColor,
            overlayBorderColor = currentOverlayBorderColor,
            overlayBorderWidth = overlayBorderWidth,
            mapApiUrl = binding.etMapApiUrl.text.toString().trim()
        )
    }

    override fun onStart() {
        super.onStart()
        // Dopo un primo delay/observe, abilitiamo l'autosave in modo che i setValue iniziali non lo scatenino
        binding.root.post { isUpdatingUI = false }
    }

    override fun onPause() {
        super.onPause()
        binding.root.clearFocus()
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

    private fun showOverlayBorderColorPickerDialog() {
        com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
            .setTitle("Scegli Colore Bordo")
            .setPreferenceName("OverlayBorderColorPickerDialog")
            .setPositiveButton("OK",
                com.skydoves.colorpickerview.listeners.ColorEnvelopeListener { envelope, _ ->
                    currentOverlayBorderColor = envelope.color
                    binding.viewOverlayBorderColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentOverlayBorderColor)
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

    private fun generateAndSaveQrCode() {
        val json = viewModel.getSettingsAsJson()
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(json, BarcodeFormat.QR_CODE, 512, 512)

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "GPSTracker_Settings_${System.currentTimeMillis()}.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/GPS Tracker")
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Toast.makeText(this, "QR Code salvato nella Galleria", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Errore: impossibile creare file multimediale", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore nella generazione del QR Code", Toast.LENGTH_LONG).show()
        }
    }

    private fun decodeQrFromUri(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) {
                Toast.makeText(this, "Errore: impossibile caricare l'immagine", Toast.LENGTH_SHORT).show()
                return
            }
            
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            
            val source = com.google.zxing.RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            
            val reader = com.google.zxing.MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            
            if (result != null && result.text != null) {
                val success = viewModel.applySettingsFromJson(result.text)
                if (success) {
                    Toast.makeText(this, "Impostazioni caricate con successo", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Errore: l'immagine non contiene impostazioni valide", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: com.google.zxing.NotFoundException) {
            Toast.makeText(this, "Nessun QR Code trovato nell'immagine", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore durante la lettura dell'immagine", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
