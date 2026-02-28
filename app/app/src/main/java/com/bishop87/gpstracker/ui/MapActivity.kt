package com.bishop87.gpstracker.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.preference.PreferenceManager
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private val viewModel: MapViewModel by viewModels()
    private lateinit var map: MapView
    private var myLocationOverlay: MyLocationNewOverlay? = null

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

        // IMPORTANTE: config osmdroid prima del setContentView
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mappa GPS"
        
        setupDefaultDates()
        setupMap()
        setupObservers()
        setupClickListeners()

        // Esegue il caricamento automatico con i filtri di default all'apertura
        viewModel.fetchMapData(selectedDateFrom, selectedDateTo)
    }

    private fun setupMap() {
        map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        
        // Imposta controller zoom
        map.controller.setZoom(15.0)

        // Richiede il permesso per mostrare la posizione corrente
        requestMyLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
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
     * Disegna i punti GPS sulla mappa: cerchi visivi e marker overlay trasparenti per gli hint.
     */
    private fun drawPointsOnMap(points: List<MapDataPoint>) {
        map.overlays.clear()
        
        // Ricevitore per i tocchi sulla mappa vuota (chiude i popup)
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                InfoWindow.closeAllInfoWindowsOn(map)
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        map.overlays.add(MapEventsOverlay(mapEventsReceiver))
        
        // Rimesse l'overlay della posizione se era abilitata
        myLocationOverlay?.let { map.overlays.add(it) }

        if (points.isEmpty()) {
            map.invalidate()
            return
        }

        val geoPoints = mutableListOf<GeoPoint>()

        for ((index, point) in points.withIndex()) {
            val lat = point.latitude?.toDoubleOrNull() ?: continue
            val lng = point.longitude?.toDoubleOrNull() ?: continue
            val pos = GeoPoint(lat, lng)
            geoPoints.add(pos)

            val isLast = (index == points.size - 1)

            // 1. Cerchio visivo (Polygon circolare)
            val circle = Polygon(map).apply {
                setPoints(Polygon.pointsAsCircle(pos, if (isLast) 12.0 else 6.0))
                fillPaint.color = if (isLast) Color.RED else Color.parseColor("#804488FF")
                outlinePaint.color = if (isLast) Color.RED else Color.BLUE
                outlinePaint.strokeWidth = 2f
            }
            
            // 2. InfoWindow sul Poligono invece del Toast
            val snippetLines = buildString {
                append("Lat: ${point.latitude}, Lng: ${point.longitude}")
                if (!point.accuracy.isNullOrBlank()) append("\nAccuracy: ${point.accuracy} m")
                if (!point.battery.isNullOrBlank()) append("\nBatteria: ${point.battery}%")
                if (!point.device.isNullOrBlank()) append("\nDevice: ${point.device}")
            }
            
            circle.title = point.timestamp ?: ""
            circle.snippet = snippetLines
            circle.infoWindow = BasicInfoWindow(org.osmdroid.library.R.layout.bonuspack_bubble, map)
            
            circle.setOnClickListener { polygon, mapView, eventPos ->
                polygon.showInfoWindow()
                true // Evento consumato
            }

            map.overlays.add(circle)
        }

        // 3. Polyline per i tracciati
        if (geoPoints.size >= 2) {
            val line = Polyline(map).apply {
                setPoints(geoPoints)
                outlinePaint.color = Color.BLUE
                outlinePaint.strokeWidth = 4f
                setOnClickListener { _, _, _ -> 
                    // Non mostrare il popup vuoto se si clicca la linea
                    true 
                }
            }
            map.overlays.add(line)
        }

        map.invalidate()

        // Centra la telecamera sull'ultimo punto animata
        map.controller.animateTo(geoPoints.last())
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
        if (myLocationOverlay == null) {
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map).apply {
                enableMyLocation()
            }
            map.overlays.add(myLocationOverlay)
        }
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
