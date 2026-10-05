package com.errorbook.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Temporary root so the app compiles; replaced with the real
 * bottom-navigation + NavHost in Phase 4.
 */
@Composable
fun ErrorBookRoot() {
    Text("错因本", modifier = Modifier.fillMaxSize())
}