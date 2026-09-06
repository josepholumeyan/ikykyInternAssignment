package com.intricatelabs.iykykassignment.ui.mainScreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intricatelabs.iykykassignment.ui.theme.DarkBg
import com.intricatelabs.iykykassignment.ui.theme.Gold

@Composable
fun IdleContent(onVideoSelected: (Uri) -> Unit) {
    val pickVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let(onVideoSelected) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("ONE VIDEO.", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("EVERY UNIQUE FACE.", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))
        Text(
            "We'll find the unique people in your video, pick their best shots, and create a collage worth sharing.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Gold.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("UPLOAD A PORTRAIT VIDEO", color = Color.White, fontWeight = FontWeight.Bold)
            Text("MP4, MOV • 9:16 • Up to 500MB", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { pickVideo.launch("video/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ CHOOSE VIDEO", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Tip: Use good lighting and steady shots for the best results.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(24.dp))
        Text("HOW IT WORKS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HowItWorksStep("Detect faces", "Finds every face in your video.", Modifier.weight(1f))
            HowItWorksStep("Identify people", "Groups the same person together.", Modifier.weight(1f))
            HowItWorksStep("Best shot", "Picks the clearest, most flattering shot.", Modifier.weight(1f))
            HowItWorksStep("Create collage", "Builds a collage you can save & share.", Modifier.weight(1f))
        }


        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color.DarkGray), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                "Everything happens on your device. Your videos never leave your phone.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HowItWorksStep(title: String, description: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Left)
        Spacer(Modifier.height(4.dp))
        Text(description, color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Left)
    }
}
