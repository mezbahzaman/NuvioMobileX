package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun rememberPosterCardStyleUiState(): PosterCardStyleUiState {
    LaunchedEffect(Unit) { PosterCardStyleRepository.ensureLoaded() }
    val uiState by PosterCardStyleRepository.uiState.collectAsStateWithLifecycle()
    return uiState
}