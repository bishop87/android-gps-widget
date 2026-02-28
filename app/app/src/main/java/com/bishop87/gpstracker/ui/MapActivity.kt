package com.bishop87.gpstracker.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bishop87.gpstracker.R
import com.bishop87.gpstracker.data.api.MapDataPoint
import com.bishop87.gpstracker.databinding.ActivityMapBinding
import com.bishop87.gpstracker.viewmodel.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private val viewModel: MapViewModel by viewModels()
    private var googleMap: GoogleMap? = null

    // Formato data selezionata (YYYY-MM-DD HH:mm — vincolo backend)
    private var selectedDateFrom: String? = null
    private var selectedDateTo: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableMyLocation()
        else Toast.makeText(this, "Permesso posizione negato", Toast.LENGTH_SHORT).show()
    }

    private val csvExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            exportDataToCsvUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mappa GPS"
        
        setupDefaultDates()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupObservers()
        setupClickListeners()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Il layer mappa di default è Normal; l'utente può cambiarlo
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        // Abilita pulsante cambio layer (satellite/terrain/hybrid/normal)
        map.uiSettings.isMapToolbarEnabled = true
        map.uiSettings.isZoomControlsEnabled = true

        // Richiede il permesso per mostrare la posizione corrente
        requestMyLocationPermission()
        
        // Esegue il caricamento automatico con i filtri di default all'apertura
        viewModel.fetchMapData(selectedDateFrom, selectedDateTo)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBarMap.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnLoadMap.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.points.observe(this) { points ->
            drawPointsOnMap(points)
        }
    }

    private fun setupClickListeners() {
        binding.btnDateFrom.setOnClickListener {
            showDateTimePicker { formatted ->
                selectedDateFrom = formatted
                binding.btnDateFrom.text = formatted
            }
        }

        binding.btnDateTo.setOnClickListener {
            showDateTimePicker { formatted ->
                selectedDateTo = formatted
                binding.btnDateTo.text = formatted
            }
        }

        binding.btnLoadMap.setOnClickListener {
            val from = selectedDateFrom
            val to = selectedDateTo

            // Validazione: fine non può essere precedente all'inizio
            if (from != null && to != null && to < from) {
                Toast.makeText(this, "La data/ora Fine non può essere precedente all'Inizio", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.fetchMapData(dateFrom = from, dateTo = to)
        }

        binding.btnExportCsv.setOnClickListener {
            val points = viewModel.points.value
            if (points.isNullOrEmpty()) {
                Toast.makeText(this, "Nessun dato da esportare", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            csvExportLauncher.launch("gps_track_$timestamp.csv")
        }
    }
    
    /**
     * Imposta le date di default: Dal 2026-01-01 00:00 ad Adesso
     */
    private fun setupDefaultDates() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        selectedDateFrom = "2026-01-01 00:00"
        binding.btnDateFrom.text = selectedDateFrom
        
        selectedDateTo = dateFormat.format(Date())
        binding.btnDateTo.text = selectedDateTo
    }

    /**
     * Mostra un DatePicker seguito da un TimePicker e formatta il risultato come "YYYY-MM-DD HH:mm".
     */
    private fun showDateTimePicker(onSelected: (String) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val formatted = String.format("%04d-%02d-%02d %02d:%02d", year, month + 1, day, hour, minute)
                onSelected(formatted)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    /**
     * Disegna i punti GPS sulla mappa: marker rotondi, polyline e marker evidenziato per l'ultimo.
     * I punti sono già ordinati in ASC per timestamp dal ViewModel.
     */
    private fun drawPointsOnMap(points: List<MapDataPoint>) {
        val map = googleMap ?: return
        map.clear()

        if (points.isEmpty()) return

        val latLngs = mutableListOf<LatLng>()

        for ((index, point) in points.withIndex()) {
            val lat = point.latitude?.toDoubleOrNull() ?: continue
            val lng = point.longitude?.toDoubleOrNull() ?: continue
            val pos = LatLng(lat, lng)
            latLngs.add(pos)

            val isLast = (index == points.size - 1)

            // Cerchio piccolo per ogni punto — maggiore raggio per l'ultimo
            map.addCircle(
                CircleOptions()
                    .center(pos)
                    .radius(if (isLast) 12.0 else 6.0)
                    .strokeColor(if (isLast) Color.RED else Color.BLUE)
                    .fillColor(if (isLast) Color.RED else Color.parseColor("#804488FF"))
                    .strokeWidth(2f)
            )

            // Marker cliccabile con InfoWindow per i dettagli
            val snippetLines = buildString {
                append("Lat: ${point.latitude}, Lng: ${point.longitude}")
                if (!point.accuracy.isNullOrBlank()) append("\nAccuracy: ${point.accuracy} m")
                if (!point.battery.isNullOrBlank()) append("\nBatteria: ${point.battery}%")
                if (!point.device.isNullOrBlank()) append("\nDevice: ${point.device}")
            }

            map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(point.timestamp ?: "")
                    .snippet(snippetLines)
                    .alpha(0f) // Marker trasparente: funge solo da hitbox invisibile per l'InfoWindow
            )
        }

        // Disegna la polyline che unisce tutti i punti in ordine ASC (da meno recente a più recente)
        if (latLngs.size >= 2) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .color(Color.BLUE)
                    .width(4f)
                    .geodesic(true)
            )
        }

        // Centra la camera sull'ultimo punto
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngs.last(), 14f))
    }

    private fun requestMyLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> enableMyLocation()
            else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @Suppress("MissingPermission")
    private fun enableMyLocation() {
        googleMap?.isMyLocationEnabled = true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun exportDataToCsvUri(uri: android.net.Uri) {
        val points = viewModel.points.value ?: return

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // Intestazione CSV
                    writer.write("Timestamp,Latitudine,Longitudine,Accuratezza,Batteria,Device\n")

                    // Dati
                    for (point in points) {
                        val ts = point.timestamp ?: ""
                        val lat = point.latitude ?: ""
                        val lng = point.longitude ?: ""
                        val acc = point.accuracy ?: ""
                        val bat = point.battery ?: ""
                        val dev = point.device ?: ""
                        
                        // Escape quotes se necessario nella vera app, ma qui i dati sono sicuri
                        writer.write("$ts,$lat,$lng,$acc,$bat,$dev\n")
                    }
                }
            }
            Toast.makeText(this, "Esportazione CSV completata con successo", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore durante l'esportazione CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
