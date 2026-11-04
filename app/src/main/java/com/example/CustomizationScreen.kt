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

    val bounds = remember { Prefs.getRealDisplayBounds(context) }
    val displayWidth = bounds.width().coerceAtLeast(1).toFloat()

    val initial = remember { Prefs.load(context) }
    var size by remember { mutableFloatStateOf(initial.size) }
    var speed by remember { mutableFloatStateOf(initial.speed) }
    var sensorSize by remember { mutableFloatStateOf(initial.sensorSize) }

    val diameterPx = size * displayWidth
    val diameterDp = with(density) { diameterPx.toDp() }
    val sensorDiameterPx = sensorSize * displayWidth

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
            text = "Adjust your animation speed, animation display size, and the inner fingerprint sensor trigger size.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9BA9D0)
        )

        // Live Lottie Preview Box with Concentric Circles
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
                        .height(300.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(diameterDp.coerceIn(64.dp, 285.dp))
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
                            val rOuter = this.size.minDimension / 2f
                            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
                            val rInner = rOuter * (sensorSize / size.coerceAtLeast(0.01f)).coerceIn(0.1f, 0.95f)

                            // 1. Outer Circle: Animation Size boundary (Cyan)
                            drawCircle(
                                color = Color(0xFF00F5FF),
                                radius = rOuter,
                                center = centerOffset,
                                style = Stroke(width = 2.5f)
                            )

                            // 2. Inner Sub-Circle: Physical Fingerprint Sensor Trigger Zone (Emerald Green)
                            drawCircle(
                                color = Color(0x3300E676),
                                radius = rInner,
                                center = centerOffset
                            )
                            drawCircle(
                                color = Color(0xFF00E676),
                                radius = rInner,
                                center = centerOffset,
                                style = Stroke(width = 3.5f)
                            )
                            drawLine(
                                color = Color(0xAA00E676),
                                start = Offset(centerOffset.x - rInner * 0.45f, centerOffset.y),
                                end = Offset(centerOffset.x + rInner * 0.45f, centerOffset.y),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = Color(0xAA00E676),
                                start = Offset(centerOffset.x, centerOffset.y - rInner * 0.45f),
                                end = Offset(centerOffset.x, centerOffset.y + rInner * 0.45f),
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                Text(
                    text = "Outer: Animation ${(size * 100).roundToInt()}% (${diameterPx.roundToInt()} px)  •  Inner: Sensor Target ${(sensorSize * 100).roundToInt()}% (${sensorDiameterPx.roundToInt()} px)",
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
                    onValueChange = { speed = it.coerceIn(0.25f, 8.0f) },
                    valueRange = 0.25f..8.0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speed_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { speed = (speed - 0.5f).coerceIn(0.25f, 8.0f) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-0.5x")
                    }
                    FilledTonalButton(
                        onClick = { speed = (speed - 0.1f).coerceIn(0.25f, 8.0f) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("speed_minus_button")
                    ) {
                        Text("-0.1x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 1.0f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1.0x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 2.0f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("2.0x")
                    }
                    FilledTonalButton(
                        onClick = { speed = (speed + 0.1f).coerceIn(0.25f, 8.0f) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("speed_plus_button")
                    ) {
                        Text("+0.1x")
                    }
                    FilledTonalButton(
                        onClick = { speed = (speed + 0.5f).coerceIn(0.25f, 8.0f) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+0.5x")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { speed = 3.0f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("3.0x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 4.0f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("4.0x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 5.0f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("5.0x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 6.0f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("6.0x")
                    }
                    FilledTonalButton(
                        onClick = { speed = 8.0f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("8.0x Max")
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
                    onValueChange = { size = it.coerceIn(0.15f, 1.0f) },
                    valueRange = 0.15f..1.0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customization_size_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { size = (size - 0.01f).coerceIn(0.15f, 1.0f) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("size_minus_button")
                    ) {
                        Text("-1%")
                    }
                    FilledTonalButton(
                        onClick = { size = 0.28f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("28%")
                    }
                    FilledTonalButton(
                        onClick = { size = 0.50f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("50%")
                    }
                    FilledTonalButton(
                        onClick = { size = 0.75f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("75%")
                    }
                    FilledTonalButton(
                        onClick = { size = 1.0f },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("100%")
                    }
                    FilledTonalButton(
                        onClick = { size = (size + 0.01f).coerceIn(0.15f, 1.0f) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("size_plus_button")
                    ) {
                        Text("+1%")
                    }
                }
            }
        }

        // Sensor Size Card (Physical Sensor Trigger Target)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sensor Size (Trigger Zone)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Inner circle: finger touches here trigger animation; touches outside are ignored.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF90A4AE)
                        )
                    }
                    Text(
                        text = "${(sensorSize * 100).roundToInt()}% (${sensorDiameterPx.roundToInt()} px)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = sensorSize,
                    onValueChange = { sensorSize = it.coerceIn(0.04f, 0.25f) },
                    valueRange = 0.04f..0.25f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sensor_size_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { sensorSize = (sensorSize - 0.01f).coerceIn(0.04f, 0.25f) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sensor_size_minus_button")
                    ) {
                        Text("-1%")
                    }
                    FilledTonalButton(
                        onClick = { sensorSize = 0.06f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("6%")
                    }
                    FilledTonalButton(
                        onClick = { sensorSize = 0.10f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("10% S25")
                    }
                    FilledTonalButton(
                        onClick = { sensorSize = 0.14f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("14%")
                    }
                    FilledTonalButton(
                        onClick = { sensorSize = (sensorSize + 0.01f).coerceIn(0.04f, 0.25f) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sensor_size_plus_button")
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
                    sensorSize = 0.10f
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
                    Prefs.saveCustomization(context, size = size, speed = speed, sensorSize = sensorSize)
                    ShizukuBridge.applyZone()
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
