package com.example

import android.app.Activity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalibrationScreen(
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    val wm = remember { context.getSystemService(WindowManager::class.java) }
    val bounds = remember { wm.currentWindowMetrics.bounds }
    val displayWidth = bounds.width().coerceAtLeast(1).toFloat()
    val displayHeight = bounds.height().coerceAtLeast(1).toFloat()

    // Full screen, system bars hidden
    DisposableEffect(view) {
        val window = (context as? Activity)?.window
        val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val initial = remember { Prefs.load(context) }
    var cx by remember { mutableFloatStateOf(initial.cx) }
    var cy by remember { mutableFloatStateOf(initial.cy) }
    var size by remember { mutableFloatStateOf(initial.size) }
    var previewEnabled by remember { mutableStateOf(false) }

    val diameterPx = size * displayWidth
    val diameterDp = with(density) { diameterPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("calibration_screen")
    ) {
        // Layer 1 (Back): Touch surface for placing the ring on ACTION_DOWN.
        // Because this is a sibling behind the top control panel, tapping Save/Reset/Sliders
        // in the top panel never triggers this touch surface.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE -> {
                            cx = (event.rawX / displayWidth).coerceIn(0.05f, 0.95f)
                            cy = (event.rawY / displayHeight).coerceIn(0.05f, 0.95f)
                            true
                        }
                        else -> false
                    }
                }
        )

        // Layer 2 (Middle): Draggable Ring & Real Lottie Preview at (cx, cy)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (cx * displayWidth - diameterPx / 2f).roundToInt(),
                        y = (cy * displayHeight - diameterPx / 2f).roundToInt()
                    )
                }
                .size(diameterDp)
                .pointerInput(displayWidth, displayHeight) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        cx = (cx + dragAmount.x / displayWidth).coerceIn(0.05f, 0.95f)
                        cy = (cy + dragAmount.y / displayHeight).coerceIn(0.05f, 0.95f)
                    }
                }
                .testTag("calibration_ring")
        ) {
            if (previewEnabled) {
                AndroidView(
                    factory = { ctx ->
                        OverlayAnimHostView(ctx).apply {
                            bindActiveAnimation(
                                context = ctx,
                                speed = initial.speed,
                                targetSizePx = diameterPx.roundToInt().coerceAtLeast(64),
                                autoPlay = true
                            )
                        }
                    },
                    update = { hostView ->
                        hostView.setSpeed(initial.speed)
                        hostView.playImmediate()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = this.size.minDimension / 2f
                drawCircle(
                    color = Color(0xFF00F5FF),
                    radius = r,
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                    style = Stroke(width = 4f)
                )
            }
        }

        // Layer 3 (Front): Control Panel at the top.
        // Consumes touches inside the panel so tapping anywhere on the panel or its buttons
        // never moves the fingerprint circle.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color(0xEE10141E))
                .pointerInput(Unit) {
                    detectTapGestures { /* consume stray taps on panel background */ }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Press and hold your finger on the fingerprint sensor.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Text(
                text = "X: ${(cx * displayWidth).roundToInt()} px  •  Y: ${(cy * displayHeight).roundToInt()} px  •  Size: ${"%.2f".format(size)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9BA9D0)
            )

            // Size slider (0.15 to 0.50)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Size", color = Color.White)
                Slider(
                    value = size,
                    onValueChange = { size = it.coerceIn(0.15f, 0.50f) },
                    valueRange = 0.15f..0.50f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("size_slider")
                )
            }

            // Four 1 px nudge buttons (evenly weighted so none wrap vertically)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { cx = (cx - 1f / displayWidth).coerceIn(0.05f, 0.95f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nudge_left")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Nudge left 1px"
                    )
                    Text("1px")
                }

                FilledTonalButton(
                    onClick = { cy = (cy - 1f / displayHeight).coerceIn(0.05f, 0.95f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nudge_up")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Nudge up 1px"
                    )
                    Text("1px")
                }

                FilledTonalButton(
                    onClick = { cy = (cy + 1f / displayHeight).coerceIn(0.05f, 0.95f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nudge_down")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Nudge down 1px"
                    )
                    Text("1px")
                }

                FilledTonalButton(
                    onClick = { cx = (cx + 1f / displayWidth).coerceIn(0.05f, 0.95f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nudge_right")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Nudge right 1px"
                    )
                    Text("1px")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Preview real animation", color = Color.White)
                Switch(
                    checked = previewEnabled,
                    onCheckedChange = { previewEnabled = it },
                    modifier = Modifier.testTag("preview_toggle")
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Save & Reset buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        cx = 0.5f
                        cy = 0.82f
                        size = 0.28f
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_button")
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = {
                        Prefs.save(context, Calib(cx, cy, size, initial.speed))
                        FpAccessibilityService.instance?.reload()
                        onSaved()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_button")
                ) {
                    Text("Save")
                }
            }
        }
    }
}
