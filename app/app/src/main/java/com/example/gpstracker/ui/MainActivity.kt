package com.example.gpstracker.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gpstracker.BuildConfig
import com.example.gpstracker.R
import com.example.gpstracker.databinding.ActivityMainBinding
import com.example.gpstracker.util.BatteryOptimizationHelper
import com.example.gpstracker.util.PermissionHelper
import com.example.gpstracker.viewmodel.MainViewModel
import com.example.gpstracker.viewmodel.UiState

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Launcher per i permessi base (FINE + COARSE)
    private val baseLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            requestBackgroundLocationIfNeeded()
            requestNotificationPermissionIfNeeded()
            checkBatteryOptimization()
        } else {
            val permanentlyDenied = PermissionHelper.BASE_LOCATION_PERMISSIONS.any { perm ->
                !shouldShowRequestPermissionRationale(perm)
                    && results[perm] == false
            }
            if (permanentlyDenied) {
                showPermissionPermanentlyDeniedDialog()
            } else {
                Toast.makeText(this, R.string.error_location_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher per il permesso background location (separato, API 29+)
    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Background location necessario per tracking automatico",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Launcher per la notifica (API 33+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* mostra info se negato, non bloccante */ }

    // Launcher per l'overlay
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                viewModel.toggleOverlay(true)
            } else {
                binding.switchOverlay.isChecked = false
                Toast.makeText(this, "Permesso overlay negato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
        requestPermissionsIfNeeded()
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            updateUiForState(state)
        }
        viewModel.statusMessage.observe(this) { message ->
            binding.tvStatus.text = message
        }
        viewModel.trackingEnabled.observe(this) { enabled ->
            binding.switchTracking.isChecked = enabled
        }
        viewModel.overlayEnabled.observe(this) { enabled ->
            binding.switchOverlay.isChecked = enabled
        }
    }

    private fun setupClickListeners() {
        binding.btnTestApi.setOnClickListener {
            if (!PermissionHelper.hasFineLocationPermission(this)) {
                requestPermissionsIfNeeded()
                return@setOnClickListener
            }
            viewModel.testApiCall()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.switchTracking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !PermissionHelper.hasFineLocationPermission(this)) {
                binding.switchTracking.isChecked = false
                requestPermissionsIfNeeded()
                return@setOnCheckedChangeListener
            }
            viewModel.toggleTracking(isChecked)
        }

        binding.switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    binding.switchOverlay.isChecked = false
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    overlayPermissionLauncher.launch(intent)
                    return@setOnCheckedChangeListener
                }
            }
            viewModel.toggleOverlay(isChecked)
        }
    }

    private fun updateUiForState(state: UiState) {
        binding.progressBar.visibility =
            if (state == UiState.ACQUIRING || state == UiState.SENDING) View.VISIBLE else View.GONE
        binding.btnTestApi.isEnabled =
            state != UiState.ACQUIRING && state != UiState.SENDING

        binding.tvStatus.text = when (state) {
            UiState.IDLE -> getString(R.string.status_idle)
            UiState.ACQUIRING -> getString(R.string.status_acquiring)
            UiState.SENDING -> getString(R.string.status_sending)
            UiState.SUCCESS -> getString(R.string.status_success)
            UiState.ERROR -> binding.tvStatus.text // mantenuto dal viewModel
        }

        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"
    }

    private fun requestPermissionsIfNeeded() {
        if (!PermissionHelper.hasFineLocationPermission(this)) {
            val shouldShowRationale = PermissionHelper.shouldShowRationale(
                this, PermissionHelper.BASE_LOCATION_PERMISSIONS[0]
            )
            if (shouldShowRationale) {
                AlertDialog.Builder(this)
                    .setTitle("Permesso necessario")
                    .setMessage(R.string.rationale_location)
                    .setPositiveButton("Concedi") { _, _ ->
                        baseLocationPermissionLauncher.launch(PermissionHelper.BASE_LOCATION_PERMISSIONS)
                    }
                    .setNegativeButton("Non ora", null)
                    .show()
            } else {
                baseLocationPermissionLauncher.launch(PermissionHelper.BASE_LOCATION_PERMISSIONS)
            }
        } else {
            requestBackgroundLocationIfNeeded()
            requestNotificationPermissionIfNeeded()
            checkBatteryOptimization()
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (!PermissionHelper.hasBackgroundLocationPermission(this)) {
            AlertDialog.Builder(this)
                .setTitle("Posizione in background")
                .setMessage(R.string.rationale_background_location)
                .setPositiveButton("Impostazioni") { _, _ ->
                    backgroundLocationPermissionLauncher.launch(PermissionHelper.BACKGROUND_LOCATION)
                }
                .setNegativeButton("Non ora", null)
                .show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        val perm = PermissionHelper.NOTIFICATION_PERMISSION ?: return
        if (!PermissionHelper.hasNotificationPermission(this)) {
            notificationPermissionLauncher.launch(perm)
        }
    }

    private fun checkBatteryOptimization() {
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.battery_opt_title)
                .setMessage(R.string.battery_opt_message)
                .setPositiveButton(R.string.battery_opt_positive) { _, _ ->
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
                }
                .setNegativeButton(R.string.battery_opt_negative, null)
                .show()
        }
    }

    private fun showPermissionPermanentlyDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permesso richiesto")
            .setMessage(R.string.error_permission_permanent)
            .setPositiveButton(R.string.btn_go_to_settings) { _, _ ->
                PermissionHelper.openAppSettings(this)
            }
            .setNegativeButton("Chiudi", null)
            .show()
    }
}
