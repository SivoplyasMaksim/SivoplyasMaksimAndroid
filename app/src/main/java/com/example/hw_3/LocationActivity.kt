package com.example.hw_3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hw_3.databinding.ActivityLocationBinding
import java.util.Locale

class LocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationBinding

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var singleUpdateListener: LocationListener? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            when {
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true -> requestLocation()

                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ->
                    binding.resultText.text = getString(R.string.location_need_precise)

                else ->
                    binding.resultText.text = getString(R.string.location_permission_denied)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.resultText.text = getString(R.string.location_hint)
        binding.getLocationButton.setOnClickListener {
            if (hasFineLocationPermission()) requestLocation()
            else locationPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationWork()
    }

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // разрешение проверено выше
    private fun requestLocation() {
        if (!hasFineLocationPermission()) {
            locationPermissionLauncher.launch(REQUIRED_PERMISSIONS)
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            binding.resultText.text = getString(R.string.location_disabled)
            return
        }

        stopLocationWork()
        binding.resultText.text = getString(R.string.location_searching)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Современный способ: один актуальный фикс
            locationManager.getCurrentLocation(
                provider, null, ContextCompat.getMainExecutor(this)
            ) { location -> onLocationResult(location, locationManager) }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) =
                    onLocationResult(location, locationManager)

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                @Deprecated("Deprecated in Java")
                override fun onProviderEnabled(provider: String) = Unit

                @Deprecated("Deprecated in Java")
                override fun onProviderDisabled(provider: String) = Unit
            }
            singleUpdateListener = listener
            @Suppress("DEPRECATION")
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }

        // Страховка: если фикса нет 15 секунд — берём последние известные координаты
        timeoutRunnable = Runnable { fallbackToLastKnown(locationManager) }
            .also { handler.postDelayed(it, TIMEOUT_MS) }
    }

    private fun onLocationResult(location: Location?, locationManager: LocationManager) {
        if (location != null) showLocation(location) else fallbackToLastKnown(locationManager)
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLastKnown(locationManager: LocationManager) {
        stopLocationWork()
        val last = locationManager.getProviders(true).orEmpty()
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        if (last != null) showLocation(last)
        else binding.resultText.text = getString(R.string.location_unavailable)
    }

    private fun showLocation(location: Location) {
        stopLocationWork()
        binding.resultText.text = String.format(
            Locale.US,
            getString(R.string.location_format),
            location.latitude,
            location.longitude
        )
    }

    private fun stopLocationWork() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
        singleUpdateListener?.let {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.removeUpdates(it)
        }
        singleUpdateListener = null
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}