package com.intricatelabs.iykykassignment.ui.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intricatelabs.iykykassignment.ui.theme.DarkBg
import com.intricatelabs.iykykassignment.ui.theme.Gold

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Gold)
            }
        }
        Spacer(Modifier.height(120.dp))
        Icon(Icons.Default.Warning, contentDescription = null, tint = Gold, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Something went wrong", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Gold)
        ) {
            Text("Try again", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
