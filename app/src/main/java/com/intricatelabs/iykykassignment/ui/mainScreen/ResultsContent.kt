package com.intricatelabs.iykykassignment.ui.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage // dependency: io.coil-kt:coil-compose
import com.intricatelabs.iykykassignment.ui.theme.DarkBg
import com.intricatelabs.iykykassignment.ui.theme.Gold

@Composable
fun ResultsContent(
    state: UiState.Results,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Gold)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("All set!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "We found ${state.people.size} unique people in your video and created a collage of their best shots.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // The mockup's overlapping-circle collage art is hand-tuned for
        // exactly 5 people. This grid scales to whatever count the assignment
        // actually produces (could be 3, could be 8) — trades exact mockup
        // geometry for correctness across all three sample videos.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.heightIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.people) { person -> PersonCard(person) }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .padding(16.dp)
        ) {
            Text(
                "Collage saved\nReady in your gallery",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onSaveToGallery, modifier = Modifier.weight(1f)) {
                Text("Save to gallery", color = Gold)
            }
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share collage", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("DETAILS", color = Color.Gray, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        DetailRow("Processed video", state.videoName)
    }
}

@Composable
private fun PersonCard(person: PersonResult) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = person.representativeImagePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            "${person.appearanceCount} appearances",
            color = Gold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.Gray, fontSize = 13.sp)
    }
}
