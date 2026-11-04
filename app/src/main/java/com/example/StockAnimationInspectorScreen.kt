package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object StockAnimDiagnosticCommands {
    const val STEP_1_CMD =
        "echo '=== ROOT & BOOTLOADER CHECK ==='; " +
            "which su 2>/dev/null || echo 'su: not found'; " +
            "pm list packages | grep -iE 'magisk|kernelsu|apatch' || echo 'root-manager-pkg: none'; " +
            "getprop ro.boot.flash.locked; getprop ro.boot.vbmeta.device_state; getprop ro.build.display.id; " +
            "echo '=== PACKAGES (itel/Transsion/FP/FOD/SystemUI) ==='; " +
            "pm list packages -f | grep -iE 'finger|fp|fod|udfps|biometric|systemui|transsion|itel'; " +
            "echo '=== SETTINGS: SYSTEM ==='; " +
            "settings list system | grep -iE 'finger|fp|fod|udfps|biometric|anim'; " +
            "echo '=== SETTINGS: SECURE ==='; " +
            "settings list secure | grep -iE 'finger|fp|fod|udfps|biometric|anim'; " +
            "echo '=== SETTINGS: GLOBAL ==='; " +
            "settings list global | grep -iE 'finger|fp|fod|udfps|biometric|anim'"

    const val STEP_2_DUMPSYS_CMD =
        "echo '=== DUMPSYS FINGERPRINT ==='; " +
            "dumpsys fingerprint; " +
            "echo '=== DUMPSYS BIOMETRIC ==='; " +
            "dumpsys biometric; " +
            "echo '=== FOD / UDFPS / BIOMETRIC WINDOWS ==='; " +
            "dumpsys window windows | grep -iE 'Window #|fod|udfps|finger|biometric|keyguard|StatusBar'"

    const val STEP_3_OVERLAY_CMD =
        "echo '=== CMD OVERLAY LIST ==='; " +
            "cmd overlay list | grep -iE '^[a-zA-Z]|systemui|finger|fp|fod|udfps|transsion|itel'"

    const val STEP_2_DISABLE_ANIM_KEY_CMD =
        "settings put secure key_fingerprint_animi_key none; " +
            "echo -n 'New key_fingerprint_animi_key: '; " +
            "settings get secure key_fingerprint_animi_key"

    const val STEP_2_RESTORE_ANIM_KEY_CMD =
        "settings put secure key_fingerprint_animi_key fp_anim_01; " +
            "echo -n 'Restored key_fingerprint_animi_key: '; " +
            "settings get secure key_fingerprint_animi_key"
}

@Composable
fun StockAnimationInspectorScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var commandText by remember { mutableStateOf(StockAnimDiagnosticCommands.STEP_2_DISABLE_ANIM_KEY_CMD) }
    var outputText by remember {
        mutableStateOf("Tap \"2. Test Disable Stock Animation Key [SAFE]\" below to set key_fingerprint_animi_key=none via Shizuku, or use the command buttons.")
    }
    var isRunning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun executeCommand(label: String, cmd: String) {
        if (isRunning) return
        commandText = cmd
        isRunning = true
        statusMessage = "Running $label via Shizuku..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                ShizukuBridge.runShellCommand(cmd)
            }
            outputText = result
            isRunning = false
            statusMessage = "Finished $label. Tap 'Copy Output' below to paste it."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
            .testTag("stock_inspector_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stock Animation Inspector (Shizuku)",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "itel S25 • Build 15.1.2.195 • Runs commands directly on phone via Shizuku",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF00E5FF)
                )
            }
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.testTag("inspector_close_button")
            ) {
                Text("Back")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101828)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF00E676).copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "LEVEL 1 — DIAGNOSTICS & SAFE SETTING SWITCH [SAFE]",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Found hidden itel OS setting: key_fingerprint_animi_key=fp_anim_01. Tap Step 2 below to test setting it to 'none' (or restore 'fp_anim_01' anytime).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9BA9D0)
                )

                Button(
                    onClick = {
                        executeCommand("Step 2: Disable key_fingerprint_animi_key [SAFE]", StockAnimDiagnosticCommands.STEP_2_DISABLE_ANIM_KEY_CMD)
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_step_2_disable_key_button")
                ) {
                    Text(
                        text = "2. Test Disable Stock Animation Key [SAFE]",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            executeCommand("Restore fp_anim_01 [SAFE]", StockAnimDiagnosticCommands.STEP_2_RESTORE_ANIM_KEY_CMD)
                        },
                        enabled = !isRunning,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_anim_key_button")
                    ) {
                        Text("Undo (Restore fp_anim_01)")
                    }

                    FilledTonalButton(
                        onClick = {
                            executeCommand("Step 1 [SAFE]", StockAnimDiagnosticCommands.STEP_1_CMD)
                        },
                        enabled = !isRunning,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_step_1_button")
                    ) {
                        Text("1. Step 1 Check")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            executeCommand("Step 1B (Dumpsys Windows)", StockAnimDiagnosticCommands.STEP_2_DUMPSYS_CMD)
                        },
                        enabled = !isRunning,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_dumpsys_button")
                    ) {
                        Text("Inspect FP Windows")
                    }

                    FilledTonalButton(
                        onClick = {
                            executeCommand("Step 2 (Overlay List)", StockAnimDiagnosticCommands.STEP_3_OVERLAY_CMD)
                        },
                        enabled = !isRunning,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_overlays_button")
                    ) {
                        Text("Inspect RRO Overlays")
                    }
                }
            }
        }

        // Custom Shizuku Command Runner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Shizuku Command Box (Run any step command here)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text("Shell command (runs as Shizuku ADB UID 2000)") },
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shizuku_command_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (commandText.isNotBlank()) {
                                executeCommand("Custom Command", commandText)
                            }
                        },
                        enabled = !isRunning && commandText.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_custom_cmd_button")
                    ) {
                        Text("Run Command")
                    }
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Shizuku Step Output", outputText))
                            statusMessage = "Copied output to clipboard! Paste it in chat."
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_output_button")
                    ) {
                        Text("Copy Output")
                    }
                }
            }
        }

        // Hidden/Debug Diagnostic Mode (SystemUI & Gate Logger, off by default)
        var diagEnabled by remember { mutableStateOf(Prefs.isDebugDiagEnabled(context)) }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (diagEnabled) Color(0xFF00E676) else Color(0xFF26324B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DEBUG Diagnostic Mode (Off by default)",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (diagEnabled) Color(0xFF00E676) else Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Logs timestamped SystemUI accessibility events/windows, keyguard/screen state, and gate state (logcat tag: FPAnimDiag).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = diagEnabled,
                        onCheckedChange = { enabled ->
                            diagEnabled = enabled
                            Prefs.setDebugDiagEnabled(context, enabled)
                            statusMessage = if (enabled) {
                                "DEBUG Diagnostic Mode ON — run the 4 fingerprint tests now, then tap Load & Copy Diag Logs."
                            } else {
                                "DEBUG Diagnostic Mode OFF"
                            }
                        },
                        modifier = Modifier.testTag("debug_diag_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val dump = DiagnosticLogger.dumpText()
                            outputText = dump
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("FPAnimDiag Logs", dump))
                            statusMessage = "Loaded & copied FPAnimDiag logs to clipboard!"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_diag_logs_button")
                    ) {
                        Text("Load & Copy Diag Logs")
                    }
                    OutlinedButton(
                        onClick = {
                            DiagnosticLogger.clear()
                            outputText = "Diagnostic log buffer cleared."
                            statusMessage = "Cleared FPAnimDiag log buffer."
                        },
                        modifier = Modifier.testTag("clear_diag_logs_button")
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        if (isRunning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.height(22.dp)
                )
                Text(
                    text = statusMessage ?: "Running...",
                    color = Color(0xFF00E5FF),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else if (statusMessage != null) {
            Text(
                text = statusMessage!!,
                color = Color(0xFF00E676),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Output console card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0E17)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF26324B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        text = outputText,
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("shizuku_output_text")
                    )
                }
            }
        }
    }
}
