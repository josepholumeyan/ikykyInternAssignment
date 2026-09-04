package com.intricatelabs.iykykassignment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.intricatelabs.iykykassignment.ui.mainScreen.MainScreen
import com.intricatelabs.iykykassignment.ui.theme.IykykAssignmentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Must be registered before the Activity reaches STARTED, so this
    // can't be created lazily inside a click handler later — has to be a
    // property, set up before onCreate's setContent call.
    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // No retry UI on denial yet — GalleryExporter's legacy save path
        // will just silently fail on API 26-28 if this was denied.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only relevant on API 26-28 — GalleryExporter's API 29+ path
        // never touches this permission at all. Requested once, eagerly,
        // at app launch rather than lazily right when Save is tapped:
        // simpler, but means the permission dialog can appear before the
        // user's even picked a video. Fine for an assignment submission —
        // worth moving to on-demand (triggered from saveCollageToGallery
        // instead) if you want better UX later.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        setContent {
            IykykAssignmentTheme {
                // MainScreen's own Surface(fillMaxSize()) is the whole UI —
                // no Scaffold needed here, each Content composable already
                // manages its own padding.
                MainScreen()
            }
        }
    }
}