package com.example

import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import kotlin.math.roundToInt

@Composable
fun CustomizationScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val wm = remember { context.getSystemService(WindowManager::class.java) }
    val bounds = remember { wm.currentWindowMetrics.bounds }
    val displayWidth = bounds.width().coerceAtLeast(1).toFloat()

    val initial = remember { Prefs.load(context) }
    var size by remember { mutableFloatStateOf(initial.size) }
    var speed by remember { mutableFloatStateOf(initial.speed) }

    val diameterPx = size * displayWidth
    val diameterDp = with(density) { diameterPx.toDp() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("customization_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Customization",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Adjust your fingerprint animation speed and size with a live preview.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9BA9D0)
        )

        // Live Lottie Preview Box
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10141E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(diameterDp.coerceAtMost(200.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                OverlayAnimHostView(ctx).apply {
                                    bindActiveAnimation(
                                        context = ctx,
                                        speed = speed,
                                        targetSizePx = diameterPx.roundToInt().coerceAtLeast(64),
                                        autoPlay = true
                                    )
                                }
                            },
                            update = { hostView ->
                                hostView.setSpeed(speed)
                                hostView.playImmediate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = this.size.minDimension / 2f
                            drawCircle(
                                color = Color(0x4400F5FF),
                                radius = r,
                                center = Offset(this.size.width / 2f, this.size.height / 2f),
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                Text(
                    text = "Speed: ${"%.2f".format(speed)}x  •  Size: ${(size * 100).roundToInt()}% (${diameterPx.roundToInt()} px)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF00F5FF)
                )
            }
        }

        // Animation Speed Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Animation Speed",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${"%.2f".format(speed)}x",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = speed,
                    onValueChange = { speed = it.coerceIn(0.25f, 3.0f) },
                    valueRange = 0.25f..3.0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speed_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { speed = (speed - 0.1f).coerceIn(0.25f, 3.0f) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("speed_minus_button")
                    ) {
                        Text("-0.1x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 0.5f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("0.5x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 1.0f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1.0x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 1.5f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1.5x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 2.0f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("2.0x")
                    }
                    FilledTonalButton(
                        onClick = { speed = (speed + 0.1f).coerceIn(0.25f, 3.0f) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("speed_plus_button")
                    ) {
                        Text("+0.1x")
                    }
                }
            }
        }

        // Animation Size Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Animation Size",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(size * 100).roundToInt()}% (${diameterPx.roundToInt()} px)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = size,
                    onValueChange = { size = it.coerceIn(0.15f, 0.50f) },
                    valueRange = 0.15f..0.50f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customization_size_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { size = (size - 0.01f).coerceIn(0.15f, 0.50f) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("size_minus_button")
                    ) {
                        Text("-1%")
                    }
                    FilledTonalButton(
                        onClick = { size = 0.20f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("20%")
                    }
                    FilledTonalButton(
                        onClick = { size = 0.28f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("28%")
                    }
                    FilledTonalButton(
                        onClick = { size = 0.38f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("38%")
                    }
                    FilledTonalButton(
                        onClick = { size = (size + 0.01f).coerceIn(0.15f, 0.50f) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("size_plus_button")
                    ) {
                        Text("+1%")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Reset & Save Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    size = 0.28f
                    speed = 1.0f
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("customization_reset_button")
            ) {
                Text("Reset")
            }

            Button(
                onClick = {
                    Prefs.saveCustomization(context, size = size, speed = speed)
                    FpAccessibilityService.instance?.reload()
                    onSaved()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("customization_save_button")
            ) {
                Text("Save")
            }
        }
    }
}
