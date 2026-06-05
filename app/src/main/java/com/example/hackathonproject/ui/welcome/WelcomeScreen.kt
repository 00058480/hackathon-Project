package com.example.hackathonproject.ui.welcome

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val WELCOME_DURATION_MS = 2400L

/**
 * One-shot animated welcome screen. The logo springs in, then the title and subtitle
 * fade and slide up, after which [onFinished] is called to reveal the app.
 */
@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logoAlpha"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 350),
        label = "titleAlpha"
    )
    val titleOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else 18.dp,
        animationSpec = tween(durationMillis = 600, delayMillis = 350),
        label = "titleOffset"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 750),
        label = "subtitleAlpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(WELCOME_DURATION_MS)
        onFinished()
    }

    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.primary, colors.primaryContainer))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = colors.onPrimary,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Разделить счёт",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary,
                modifier = Modifier
                    .offset(y = titleOffset)
                    .graphicsLayer { alpha = titleAlpha }
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Делим поровну, без споров",
                fontSize = 16.sp,
                color = colors.onPrimary,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
            )
        }
    }
}
