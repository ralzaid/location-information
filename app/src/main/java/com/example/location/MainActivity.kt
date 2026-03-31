package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var permissionGranted by mutableStateOf(false)

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionGranted = it
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            MaterialTheme {
                Screen(permissionGranted)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun Screen(permissionGranted: Boolean) {
    val context = LocalContext.current
    val client = LocationServices.getFusedLocationProviderClient(context)

    var location by remember { mutableStateOf<LatLng?>(null) }
    var address by remember { mutableStateOf("Address will appear here") }
    var markers by remember { mutableStateOf(listOf<LatLng>()) }

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(42.3601, -71.0589), 12f)
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            client.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    location = latLng
                    camera.position = CameraPosition.fromLatLngZoom(latLng, 15f)

                    getAddress(context, loc.latitude, loc.longitude) {
                        address = it
                    }
                }
            }
        } else {
            address = "Permission not granted"
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(address, Modifier.fillMaxWidth().padding(16.dp))

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = MapProperties(isMyLocationEnabled = permissionGranted),
            onMapClick = { markers = markers + it }
        ) {
            location?.let {
                Marker(state = MarkerState(it), title = "Me")
            }

            markers.forEach {
                Marker(state = MarkerState(it))
            }
        }
    }
}

fun getAddress(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    onResult: (String) -> Unit
) {
    val geocoder = Geocoder(context, Locale.getDefault())

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocation(lat, lng, 1) {
            onResult(it.firstOrNull()?.getAddressLine(0) ?: "No address")
        }
    } else {
        @Suppress("DEPRECATION")
        val list = geocoder.getFromLocation(lat, lng, 1)
        onResult(list?.firstOrNull()?.getAddressLine(0) ?: "No address")
    }
}