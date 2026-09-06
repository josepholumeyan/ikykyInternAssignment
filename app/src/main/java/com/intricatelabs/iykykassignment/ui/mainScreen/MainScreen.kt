package com.intricatelabs.iykykassignment.ui.mainScreen

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import com.intricatelabs.iykykassignment.ui.theme.DarkBg

/**
 * Screen/Content separation: this Composable's only job is reading UiState
 * and routing to the matching Content, plus reacting to the VM's one-shot
 * events (share, save-result) that need an Activity context to act on.
 * Processing logic itself still lives entirely in the ViewModel.
 */
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = uiState !is UiState.Idle) {
        viewModel.handleBackPress()
    }

    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share collage"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveResultEvent.collect { success ->
            if (success) {
                Toast.makeText(
                    context, "Saved Successfully", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBg
    ) {
        Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            when (val state = uiState) {
                is UiState.Idle -> IdleContent(
                    onVideoSelected = viewModel::onVideoSelected
                )

                is UiState.Processing -> ProcessingContent(
                    state = state,
                    onCancel = viewModel::reset
                )

                is UiState.Results -> ResultsContent(
                    state = state,
                    onSaveToGallery = viewModel::saveCollageToGallery,
                    onShare = { viewModel.shareCollage(context) }
                )

                is UiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::reset,
                    onDismiss = viewModel::reset
                )
            }
        }
    }
}