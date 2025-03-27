package com.example.locationinformation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.locationinformation.ui.theme.LocationInformationTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            //if permission not given ask the user for it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }

        setContent {
            LocationInformationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    //state variable to hold the user's location
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    var addressText by remember { mutableStateOf("...") }

    // Request last known location
    // Runs once on start
    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            //try to get the last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    //if successful, it updates userLocation
                    userLocation = LatLng(it.latitude, it.longitude)
                }
            }

        }
    }

    val cameraPositionState = rememberCameraPositionState()

    //center map after user location is updated
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 15f))
        }
    }

    //get user's current address after user location is updated
    LaunchedEffect(userLocation) {
        userLocation?.let {
            addressText = reverseGeocode(context, it.latitude, it.longitude)
        }
    }


    //adding custom markers
    //when users clicks on screen, app creates marker on the location

    val customMarkers = remember { mutableStateListOf<LatLng>() }


    //Show map
    Box {
        //placing the map inside a Box
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(zoomControlsEnabled = true),
            onMapClick = { latLng ->
                customMarkers.add(latLng) // Add marker on map tap
            }
        ) {
            //place a marker at the user location
            userLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "You are here"
                )
            }

            // for each marker in custom markers list we add a marker onto map
            for (marker in customMarkers) {
                Marker(
                    state = MarkerState(position = marker),
                    title = "Custom Marker"
                )
            }
        }

        //on the top center of the screen, displaying user address
        // Address shown at top center
        Text(
            text = addressText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


// Reverse geocoding function (converts latitude and longitude to Address object)
suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context)

            //// Get the first address from the result list
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Address not found"
        } catch (e: Exception) {
            e.printStackTrace() // Print the exception stack trace for debugging
            "Address not found"
        }
    }
}
