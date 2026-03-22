package com.carownerassistant.feature.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.carownerassistant.core.flags.FeatureFlag
import com.carownerassistant.core.flags.FeatureFlagRepository
import com.carownerassistant.core.model.GeoPoint
import com.carownerassistant.core.model.PlaceInfo
import com.carownerassistant.core.model.PlaceSearchCriteria
import com.carownerassistant.core.model.PlaceType
import com.carownerassistant.core.model.PlacesRules
import com.carownerassistant.core.model.repository.PlaceGatewayResult
import com.carownerassistant.core.model.repository.PlacesRepository
import java.text.DecimalFormat
import kotlinx.coroutines.launch
import kotlin.math.cos

private val decimalFormat = DecimalFormat("0.0")

private enum class PlacesViewMode {
    LIST,
    MAP,
}

private enum class SearchCenterMode {
    CURRENT,
    MANUAL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesRoute(
    placesRepository: PlacesRepository,
    featureFlagRepository: FeatureFlagRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by placesRepository.observeFavorites().collectAsState(initial = emptyList())

    var viewMode by remember { mutableStateOf(PlacesViewMode.LIST) }
    var searchCenterMode by remember { mutableStateOf(SearchCenterMode.MANUAL) }
    var selectedTypes by remember {
        mutableStateOf(
            linkedSetOf(
                PlaceType.GAS_STATION,
                PlaceType.SERVICE_CENTER,
                PlaceType.TIRE_SHOP,
            ),
        )
    }
    var radiusKm by remember { mutableStateOf(10f) }
    var manualLatitude by remember { mutableStateOf("55.751244") }
    var manualLongitude by remember { mutableStateOf("37.618423") }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var statusMessage by remember {
        mutableStateOf("Use manual coordinates or grant location permission for current position.")
    }
    var searchResults by remember { mutableStateOf<List<PlaceInfo>>(emptyList()) }
    var detailsPlace by remember { mutableStateOf<PlaceInfo?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            currentLocation = loadCurrentLocation(context)
            statusMessage = if (currentLocation != null) {
                "Using the latest available device location."
            } else {
                "Location permission granted, but no current location was available. Manual mode stays available."
            }
        } else {
            statusMessage = "Location permission denied. Continue with manual location."
        }
    }

    fun resolvedCenter(): GeoPoint? {
        return when (searchCenterMode) {
            SearchCenterMode.CURRENT -> currentLocation
            SearchCenterMode.MANUAL -> {
                val latitude = manualLatitude.toDoubleOrNull()
                val longitude = manualLongitude.toDoubleOrNull()
                if (latitude == null || longitude == null) {
                    null
                } else {
                    GeoPoint(latitude = latitude, longitude = longitude)
                }
            }
        }
    }

    fun runSearch() {
        val center = resolvedCenter()
        if (!featureFlagRepository.isEnabled(FeatureFlag.MAP_PLACES_ENABLED)) {
            statusMessage = "Map and places are disabled by feature flag."
            return
        }
        if (center == null) {
            statusMessage = "Provide a valid location before searching."
            return
        }
        if (selectedTypes.isEmpty()) {
            statusMessage = "Choose at least one place type."
            return
        }

        scope.launch {
            when (
                val result = placesRepository.searchPlaces(
                    PlaceSearchCriteria(
                        center = center,
                        radiusKm = radiusKm.toInt(),
                        types = selectedTypes,
                    ),
                )
            ) {
                is PlaceGatewayResult.Success -> {
                    searchResults = result.value
                    statusMessage = if (result.value.isEmpty()) {
                        "No places matched this search radius and type selection."
                    } else {
                        "Found ${result.value.size} places."
                    }
                }

                PlaceGatewayResult.NoNetwork -> {
                    searchResults = emptyList()
                    statusMessage = "Provider reported no network."
                }

                PlaceGatewayResult.PermissionDenied -> {
                    searchResults = emptyList()
                    statusMessage = "Provider denied access."
                }

                PlaceGatewayResult.ProviderUnavailable -> {
                    searchResults = emptyList()
                    statusMessage = "Places provider is unavailable."
                }

                is PlaceGatewayResult.InvalidRequest -> {
                    searchResults = emptyList()
                    statusMessage = result.reason
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runSearch()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Map and Places") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchControlsCard(
                    viewMode = viewMode,
                    searchCenterMode = searchCenterMode,
                    manualLatitude = manualLatitude,
                    manualLongitude = manualLongitude,
                    currentLocation = currentLocation,
                    radiusKm = radiusKm,
                    selectedTypes = selectedTypes,
                    statusMessage = statusMessage,
                    onSelectListMode = { viewMode = PlacesViewMode.LIST },
                    onSelectMapMode = { viewMode = PlacesViewMode.MAP },
                    onUseCurrentLocation = {
                        searchCenterMode = SearchCenterMode.CURRENT
                        if (hasFineLocationPermission(context)) {
                            currentLocation = loadCurrentLocation(context)
                            statusMessage = if (currentLocation != null) {
                                "Using the latest available device location."
                            } else {
                                "No current location was available. Manual mode stays available."
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    onUseManualLocation = { searchCenterMode = SearchCenterMode.MANUAL },
                    onManualLatitudeChange = { manualLatitude = it },
                    onManualLongitudeChange = { manualLongitude = it },
                    onRadiusChange = { radiusKm = it },
                    onToggleType = { type ->
                        selectedTypes = selectedTypes.toMutableSet().apply {
                            if (type in this) remove(type) else add(type)
                        }.toCollection(linkedSetOf())
                    },
                    onSearch = ::runSearch,
                )
            }

            if (favorites.isNotEmpty()) {
                item {
                    FavoritesSection(
                        favorites = favorites,
                        onOpenPlace = { place ->
                            scope.launch {
                                detailsPlace = resolveDetails(placesRepository, place)
                            }
                        },
                        onToggleFavorite = { place ->
                            scope.launch {
                                placesRepository.removeFavorite(place.ref.normalizedPlaceId)
                                runSearch()
                            }
                        },
                    )
                }
            }

            if (viewMode == PlacesViewMode.MAP) {
                item {
                    PlacesMapCard(
                        center = resolvedCenter(),
                        radiusKm = radiusKm.toInt(),
                        places = searchResults,
                    )
                }
            }

            if (searchResults.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Run a search to see place cards here.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(searchResults, key = { it.ref.normalizedPlaceId }) { place ->
                    PlaceCard(
                        place = place,
                        onOpenDetails = {
                            scope.launch {
                                detailsPlace = resolveDetails(placesRepository, place)
                            }
                        },
                        onToggleFavorite = {
                            scope.launch {
                                if (place.isFavorite) {
                                    placesRepository.removeFavorite(place.ref.normalizedPlaceId)
                                } else {
                                    placesRepository.addFavorite(place)
                                }
                                runSearch()
                            }
                        },
                    )
                }
            }
        }
    }

    detailsPlace?.let { place ->
        PlaceDetailsDialog(
            place = place,
            onDismiss = { detailsPlace = null },
            onToggleFavorite = {
                scope.launch {
                    if (place.isFavorite) {
                        placesRepository.removeFavorite(place.ref.normalizedPlaceId)
                        detailsPlace = place.copy(isFavorite = false)
                    } else {
                        placesRepository.addFavorite(place)
                        detailsPlace = place.copy(isFavorite = true)
                    }
                    runSearch()
                }
            },
            onOpenExternalMaps = {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "geo:${place.coordinates.latitude},${place.coordinates.longitude}?q=" +
                                "${place.coordinates.latitude},${place.coordinates.longitude}" +
                                "(${Uri.encode(place.displayName)})",
                        ),
                    ),
                )
            },
        )
    }
}

@Composable
private fun SearchControlsCard(
    viewMode: PlacesViewMode,
    searchCenterMode: SearchCenterMode,
    manualLatitude: String,
    manualLongitude: String,
    currentLocation: GeoPoint?,
    radiusKm: Float,
    selectedTypes: Set<PlaceType>,
    statusMessage: String,
    onSelectListMode: () -> Unit,
    onSelectMapMode: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onUseManualLocation: () -> Unit,
    onManualLatitudeChange: (String) -> Unit,
    onManualLongitudeChange: (String) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onToggleType: (PlaceType) -> Unit,
    onSearch: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Find gas stations, service centers, and tire shops around a chosen point.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSelectListMode,
                    enabled = viewMode != PlacesViewMode.LIST,
                ) {
                    Text(text = "List mode")
                }
                Button(
                    onClick = onSelectMapMode,
                    enabled = viewMode != PlacesViewMode.MAP,
                ) {
                    Text(text = "Map mode")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUseCurrentLocation) {
                    Text(
                        text = if (searchCenterMode == SearchCenterMode.CURRENT) {
                            "Current location selected"
                        } else {
                            "Use current location"
                        },
                    )
                }
                Button(
                    onClick = onUseManualLocation,
                    enabled = searchCenterMode != SearchCenterMode.MANUAL,
                ) {
                    Text(text = "Use manual location")
                }
            }
            if (searchCenterMode == SearchCenterMode.MANUAL) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualLatitude,
                        onValueChange = onManualLatitudeChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(text = "Latitude") },
                    )
                    OutlinedTextField(
                        value = manualLongitude,
                        onValueChange = onManualLongitudeChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(text = "Longitude") },
                    )
                }
            } else {
                Text(
                    text = currentLocation?.let {
                        "Current center: ${it.latitude}, ${it.longitude}"
                    } ?: "Current location not ready yet.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column {
                Text(text = "Search radius: ${radiusKm.toInt()} km")
                Slider(
                    value = radiusKm,
                    onValueChange = onRadiusChange,
                    valueRange = 1f..30f,
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PlaceType.entries) { type ->
                    FilterChip(
                        selected = type in selectedTypes,
                        onClick = { onToggleType(type) },
                        label = { Text(text = type.displayLabel()) },
                    )
                }
            }
            Button(onClick = onSearch) {
                Text(text = "Search places")
            }
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FavoritesSection(
    favorites: List<PlaceInfo>,
    onOpenPlace: (PlaceInfo) -> Unit,
    onToggleFavorite: (PlaceInfo) -> Unit,
) {
    val grouped = remember(favorites) { PlacesRules.groupFavoritesByType(favorites) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Global favorites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            grouped.forEach { (type, places) ->
                Text(
                    text = type.displayLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                places.forEach { place ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = place.displayName,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenPlace(place) },
                        )
                        TextButton(onClick = { onToggleFavorite(place) }) {
                            Text(text = "Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacesMapCard(
    center: GeoPoint?,
    radiusKm: Int,
    places: List<PlaceInfo>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Map preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = center?.let {
                    "Center ${it.latitude}, ${it.longitude} with ${radiusKm} km radius"
                } ?: "Choose a valid center to visualize search radius.",
                style = MaterialTheme.typography.bodySmall,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                if (center == null) {
                    Text(
                        text = "Map preview unavailable until a valid center is set.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                        val radiusPx = minOf(size.width, size.height) * 0.38f
                        drawCircle(
                            color = Color(0xFF4C7D5B),
                            radius = radiusPx,
                            center = canvasCenter,
                            style = Stroke(width = 4f),
                        )
                        drawCircle(
                            color = Color(0xFF1D3557),
                            radius = 10f,
                            center = canvasCenter,
                        )
                        places.forEach { place ->
                            val latDeltaKm = (place.coordinates.latitude - center.latitude) * 111.0
                            val lonDeltaKm = (place.coordinates.longitude - center.longitude) * 111.0 *
                                cos(Math.toRadians(center.latitude))
                            val offset = Offset(
                                x = canvasCenter.x + ((lonDeltaKm / radiusKm.toFloat()) * radiusPx).toFloat(),
                                y = canvasCenter.y - ((latDeltaKm / radiusKm.toFloat()) * radiusPx).toFloat(),
                            )
                            drawCircle(
                                color = place.type.mapColor(),
                                radius = 10f,
                                center = offset,
                            )
                        }
                    }
                }
            }
            Text(
                text = "The MVP keeps provider interfaces abstract. This preview visualizes the active search radius and normalized results without binding UI to a specific SDK.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PlaceCard(
    place: PlaceInfo,
    onOpenDetails: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(text = place.type.displayLabel())
                }
                TextButton(onClick = onToggleFavorite) {
                    Text(text = if (place.isFavorite) "Unfavorite" else "Favorite")
                }
            }
            Text(text = place.formattedAddress)
            Text(
                text = place.distanceKm?.let { "Distance: ${decimalFormat.format(it)} km" }
                    ?: "Distance unavailable",
            )
            Text(
                text = buildString {
                    append("Rating: ")
                    append(place.rating?.let { decimalFormat.format(it) } ?: "n/a")
                    place.reviewCount?.let { append(" from $it reviews") }
                },
            )
            if (place.normalizedTags.isNotEmpty()) {
                Text(
                    text = "Tags: ${place.normalizedTags.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PlaceDetailsDialog(
    place: PlaceInfo,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenExternalMaps: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = place.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = place.type.displayLabel())
                Text(text = place.formattedAddress)
                Text(
                    text = place.distanceKm?.let { "Distance: ${decimalFormat.format(it)} km" }
                        ?: "Distance unavailable",
                )
                Text(
                    text = buildString {
                        append("Rating: ")
                        append(place.rating?.let { decimalFormat.format(it) } ?: "n/a")
                        place.reviewCount?.let { append(" from $it reviews") }
                    },
                )
                Text(text = "Phone: ${place.phone ?: "not available"}")
                Text(
                    text = if (place.normalizedTags.isEmpty()) {
                        "Tags: none"
                    } else {
                        "Tags: ${place.normalizedTags.joinToString()}"
                    },
                )
                Text(
                    text = "Normalized provider ref: ${place.ref.normalizedPlaceId}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onToggleFavorite) {
                    Text(text = if (place.isFavorite) "Remove favorite" else "Save favorite")
                }
                TextButton(onClick = onOpenExternalMaps) {
                    Text(text = "Open maps")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
    )
}

private suspend fun resolveDetails(
    placesRepository: PlacesRepository,
    place: PlaceInfo,
): PlaceInfo {
    return when (val result = placesRepository.getPlaceDetails(place.ref)) {
        is PlaceGatewayResult.Success -> result.value.copy(
            distanceKm = result.value.distanceKm ?: place.distanceKm,
            isFavorite = result.value.isFavorite || place.isFavorite,
        )
        else -> place
    }
}

private fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun loadCurrentLocation(context: Context): GeoPoint? {
    val locationManager = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = locationManager.getProviders(true)
    val bestLocation = providers
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull(Location::getTime)
        ?: return null
    return GeoPoint(
        latitude = bestLocation.latitude,
        longitude = bestLocation.longitude,
    )
}

private fun PlaceType.displayLabel(): String {
    return when (this) {
        PlaceType.GAS_STATION -> "Gas stations"
        PlaceType.SERVICE_CENTER -> "Service centers"
        PlaceType.TIRE_SHOP -> "Tire shops"
    }
}

private fun PlaceType.mapColor(): Color {
    return when (this) {
        PlaceType.GAS_STATION -> Color(0xFF2A9D8F)
        PlaceType.SERVICE_CENTER -> Color(0xFFE76F51)
        PlaceType.TIRE_SHOP -> Color(0xFF264653)
    }
}
