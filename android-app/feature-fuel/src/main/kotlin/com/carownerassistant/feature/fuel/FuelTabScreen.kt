package com.carownerassistant.feature.fuel

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
fun FuelTabScreen(
    onOpenMileage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Fuel",
            modifier = Modifier.padding(start = 24.dp, top = 24.dp),
        )
        Button(
            onClick = onOpenMileage,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Text(text = "Add Mileage")
        }
    }
}
