package com.carownerassistant.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsTabScreen(
    onOpenGarage: () -> Unit,
    onOpenHandbook: () -> Unit,
    onOpenPlaces: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Settings")
        Button(onClick = onOpenGarage) {
            Text(text = "Open Garage")
        }
        Button(onClick = onOpenHandbook) {
            Text(text = "Open Vehicle Handbook")
        }
        Button(onClick = onOpenPlaces) {
            Text(text = "Open Map and Places")
        }
    }
}
