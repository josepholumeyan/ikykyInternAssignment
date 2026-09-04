package com.intricatelabs.iykykassignment.ui.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intricatelabs.iykykassignment.ui.theme.DarkBg
import com.intricatelabs.iykykassignment.ui.theme.Gold

@Composable
fun ProcessingContent(state: UiState.Processing) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Processing your video", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "This may take a few minutes.\nYou can leave the app and we'll notify you when it's ready.",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(40.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            CircularProgressIndicator(
                progress = { state.overallProgress },
                modifier = Modifier.fillMaxSize(),
                color = Gold,
                strokeWidth = 6.dp,
                trackColor = Color.DarkGray
            )
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Gold, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "${(state.overallProgress * 100).toInt()}%",
            color = Gold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Analyzing frames...", color = Color.Gray, fontSize = 13.sp)

        Spacer(Modifier.height(32.dp))
        ProcessingStep.entries.forEach { step ->
            StepRow(
                step = step,
                status = when {
                    step.ordinal < state.currentStep.ordinal -> StepStatus.DONE
                    step == state.currentStep -> StepStatus.ACTIVE
                    else -> StepStatus.PENDING
                }
            )
        }
    }
}

private enum class StepStatus { DONE, ACTIVE, PENDING }

@Composable
private fun StepRow(step: ProcessingStep, status: StepStatus) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(step.title, color = Color.White, fontSize = 14.sp)
            Text(
                if (status == StepStatus.ACTIVE) step.activeSubtitle else step.idleSubtitle,
                color = if (status == StepStatus.ACTIVE) Gold else Color.Gray,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (status == StepStatus.DONE) Gold else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (status == StepStatus.DONE) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
