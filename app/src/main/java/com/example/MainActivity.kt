package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.LottieCompositionFactory
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import java.io.File

private enum class ScreenMode {
    WIZARD,
    MAIN,
    CALIBRATION,
    CUSTOMIZATION,
    CHANGE_ANIMATION,
    SETUP_OVERVIEW,
    STOCK_INSPECTOR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        LottiePrewarmCache.prewarmAsync(this)
        ProVideoFrameCache.prewarmAsync(this)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                FpAnimatorRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
fun FpAnimatorRoot() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var refreshTick by remember { mutableIntStateOf(0) }
    var shizukuInstalled by remember { mutableStateOf(Setup.shizukuInstalled(context)) }
    var shizukuRunning by remember { mutableStateOf(Setup.shizukuRunning()) }
    var shizukuAllowed by remember { mutableStateOf(Setup.shizukuAllowed()) }
    var serviceEnabled by remember { mutableStateOf(Setup.serviceEnabled(context)) }
    var batteryOk by remember { mutableStateOf(Setup.batteryOk(context)) }
    var hasCustomAnim by remember { mutableStateOf(AnimLoader.hasCustomAnimation(context)) }
    var importMessage by remember { mutableStateOf<String?>(null) }

    fun refreshState() {
        shizukuInstalled = Setup.shizukuInstalled(context)
        shizukuRunning = Setup.shizukuRunning()
        shizukuAllowed = Setup.shizukuAllowed()
        serviceEnabled = Setup.serviceEnabled(context)
        batteryOk = Setup.batteryOk(context)
        hasCustomAnim = AnimLoader.hasCustomAnimation(context)
        ShizukuBridge.tryBind()
        refreshTick++
    }

    // Determine initial screen or missing permission redirect
    var screenMode by remember {
        mutableStateOf(
            if (Setup.isFullSetupComplete(context)) ScreenMode.MAIN else ScreenMode.WIZARD
        )
    }

    var wizardStep by remember {
        mutableIntStateOf(firstIncompleteStep(context).coerceAtMost(8))
    }

    fun advanceToNextIncompleteOrMain() {
        refreshState()
        val next = firstIncompleteStep(context)
        if (next > 8) {
            screenMode = ScreenMode.MAIN
        } else {
            wizardStep = next
        }
    }

    // Poll while in Wizard or Setup Overview so granted permissions update & auto-advance immediately
    LaunchedEffect(screenMode, wizardStep) {
        while (screenMode == ScreenMode.WIZARD || screenMode == ScreenMode.SETUP_OVERVIEW) {
            refreshState()
            if (screenMode == ScreenMode.WIZARD) {
                val currentStepDone = when (wizardStep) {
                    1 -> shizukuInstalled && shizukuRunning
                    2 -> shizukuAllowed
                    3 -> serviceEnabled
                    4 -> batteryOk
                    5 -> Prefs.isBgDone(context)
                    6 -> Prefs.isStockStepDone(context)
                    7 -> Prefs.isCalibDone(context)
                    8 -> Prefs.isAnimStepDone(context)
                    else -> true
                }
                if (currentStepDone) {
                    advanceToNextIncompleteOrMain()
                }
            }
            delay(600L)
        }
    }

    // Refresh on Activity resume and check if any required permission became missing
    DisposableEffect(lifecycleOwner) {
        val permListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                ShizukuBridge.tryBind()
                refreshState()
            }
        }
        Shizuku.addRequestPermissionResultListener(permListener)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState()
                if (screenMode == ScreenMode.MAIN && !Setup.allRequiredPermissionsGranted(context)) {
                    val next = firstIncompleteStep(context)
                    if (next <= 8) {
                        wizardStep = next
                        screenMode = ScreenMode.WIZARD
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            Shizuku.removeRequestPermissionResultListener(permListener)
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = importAndValidateLottieJson(context, uri)
            if (ok) {
                Prefs.setSelectedAnim(context, "imported", "Custom (custom.json)")
                Prefs.setAnimStepDone(context, true)
                FpAccessibilityService.instance?.reload()
                refreshState()
                importMessage = "Imported custom.json successfully."
                if (screenMode == ScreenMode.WIZARD && wizardStep == 8) {
                    screenMode = ScreenMode.MAIN
                }
            } else {
                importMessage = "Invalid Lottie JSON file."
            }
        }
    }

    BackHandler(
        enabled = screenMode == ScreenMode.CALIBRATION ||
            screenMode == ScreenMode.CUSTOMIZATION ||
            screenMode == ScreenMode.SETUP_OVERVIEW ||
            screenMode == ScreenMode.STOCK_INSPECTOR
    ) {
        screenMode = ScreenMode.MAIN
    }

    when (screenMode) {
        ScreenMode.CALIBRATION -> {
            CalibrationScreen(
                onSaved = {
                    refreshState()
                    screenMode = ScreenMode.MAIN
                }
            )
        }

        ScreenMode.CUSTOMIZATION -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Black,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                CustomizationScreen(
                    onSaved = {
                        refreshState()
                        screenMode = ScreenMode.MAIN
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        ScreenMode.CHANGE_ANIMATION -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Black,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                ChangeAnimationScreen(
                    onAnimationSelected = { msg ->
                        importMessage = msg
                        refreshState()
                    },
                    onClose = {
                        refreshState()
                        screenMode = ScreenMode.MAIN
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        ScreenMode.SETUP_OVERVIEW -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Black,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                SetupOverviewScreen(
                    shizukuInstalled = shizukuInstalled,
                    shizukuRunning = shizukuRunning,
                    shizukuAllowed = shizukuAllowed,
                    serviceEnabled = serviceEnabled,
                    batteryOk = batteryOk,
                    onRefresh = { refreshState() },
                    onOpenStockInspector = { screenMode = ScreenMode.STOCK_INSPECTOR },
                    onDone = { screenMode = ScreenMode.MAIN },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        ScreenMode.STOCK_INSPECTOR -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Black,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                StockAnimationInspectorScreen(
                    onClose = {
                        refreshState()
                        screenMode = ScreenMode.MAIN
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        ScreenMode.WIZARD -> {
            if (wizardStep == 7) {
                CalibrationScreen(
                    onSaved = {
                        advanceToNextIncompleteOrMain()
                    }
                )
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black,
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { padding ->
                    SetupWizardScreen(
                        step = wizardStep,
                        shizukuInstalled = shizukuInstalled,
                        shizukuRunning = shizukuRunning,
                        shizukuAllowed = shizukuAllowed,
                        serviceEnabled = serviceEnabled,
                        batteryOk = batteryOk,
                        importMessage = importMessage,
                        onAdvanceStep = { _ ->
                            advanceToNextIncompleteOrMain()
                        },
                        onImportJson = {
                            importJsonLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                        },
                        onOpenChangeAnimation = {
                            screenMode = ScreenMode.CHANGE_ANIMATION
                        },
                        onSkipToMain = {
                            screenMode = ScreenMode.MAIN
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }

        ScreenMode.MAIN -> {
            val currentCalib = remember(refreshTick) { Prefs.load(context) }
            val selectedAnimLabel = remember(refreshTick) { Prefs.getSelectedAnimLabel(context) }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Black,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                MainScreen(
                    shizukuRunning = shizukuRunning,
                    serviceEnabled = serviceEnabled,
                    hasCustomAnim = hasCustomAnim,
                    selectedAnimLabel = selectedAnimLabel,
                    calib = currentCalib,
                    importMessage = importMessage,
                    onEnableService = {
                        if (!Setup.enableService(context)) {
                            safeStartActivity(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                        refreshState()
                    },
                    onTestOverlay = {
                        val svc = FpAccessibilityService.instance
                        if (svc != null) {
                            svc.triggerTestPreview()
                            importMessage = "Playing 2.4s live overlay preview on sensor zone!"
                        } else {
                            importMessage = "Enable Accessibility Service first to preview the live overlay."
                        }
                    },
                    onAdjustPosition = { screenMode = ScreenMode.CALIBRATION },
                    onOpenChangeAnimation = { screenMode = ScreenMode.CHANGE_ANIMATION },
                    onOpenCustomization = { screenMode = ScreenMode.CUSTOMIZATION },
                    onImportAnimation = {
                        importJsonLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    },
                    onOpenSetup = {
                        refreshState()
                        val incomplete = firstIncompleteStep(context)
                        if (incomplete <= 4) {
                            wizardStep = incomplete
                            screenMode = ScreenMode.WIZARD
                        } else {
                            screenMode = ScreenMode.SETUP_OVERVIEW
                        }
                    },
                    onOpenStockInspector = {
                        screenMode = ScreenMode.STOCK_INSPECTOR
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun SetupWizardScreen(
    step: Int,
    shizukuInstalled: Boolean,
    shizukuRunning: Boolean,
    shizukuAllowed: Boolean,
    serviceEnabled: Boolean,
    batteryOk: Boolean,
    importMessage: String?,
    onAdvanceStep: (Int) -> Unit,
    onImportJson: () -> Unit,
    onOpenChangeAnimation: () -> Unit = {},
    onSkipToMain: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var a11yFallbackShown by remember(step) { mutableStateOf(false) }

    val stepGranted = when (step) {
        1 -> true
        2 -> true
        3 -> serviceEnabled
        4 -> batteryOk
        5 -> Prefs.isBgDone(context)
        6 -> Prefs.isStockStepDone(context)
        8 -> Prefs.isAnimStepDone(context)
        else -> false
    }

    val displayStepNum = (step - 2).coerceIn(1, 6)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("setup_wizard_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Step $displayStepNum of 6 • Standalone Mode (No Shizuku Needed)",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF00E5FF)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Big status tick
        Icon(
            imageVector = if (stepGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (stepGranted) "Completed" else "Pending",
            tint = if (stepGranted) Color(0xFF00E676) else Color(0xFF00F5FF),
            modifier = Modifier.size(88.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (step) {
            1, 2 -> {
                WizardTitle("Standalone Engine Ready")
                WizardBody(
                    "FP Animator now runs 100% standalone without requiring Shizuku."
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onAdvanceStep(3) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            }

            3 -> {
                WizardTitle("1. Enable Accessibility Service")
                WizardBody(
                    if (a11yFallbackShown) {
                        "In Settings: open App info > menu > \"Allow restricted settings\", then enable FP Animator in Accessibility so the overlay and standalone sensor engine can run."
                    } else {
                        "Enable the FP Animator Accessibility Service so it can detect lock screen / biometric prompts and display your custom fingerprint animation."
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val ok = Setup.enableService(context)
                        if (ok && Setup.serviceEnabled(context)) {
                            onAdvanceStep(4)
                        } else {
                            a11yFallbackShown = true
                            safeStartActivity(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enable_service_button")
                ) {
                    Text("Enable Accessibility Service")
                }

                if (a11yFallbackShown) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            safeStartActivity(
                                context,
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open App Info (Allow restricted settings)")
                    }
                }
            }

            4 -> {
                WizardTitle("2. Battery Optimization")
                WizardBody(
                    "Allow FP Animator to ignore battery optimizations so the standalone overlay service stays instant on the lock screen."
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        safeStartActivity(
                            context,
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("battery_opt_button")
                ) {
                    Text("Allow Ignore Battery Optimizations")
                }
            }

            5 -> {
                WizardTitle("3. Background Activity")
                WizardBody(
                    "Set Battery to No restrictions, allow Auto-start / Background activity if itel OS shows them, and lock the app in Recents."
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        safeStartActivity(
                            context,
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_app_details_button")
                ) {
                    Text("Open App Info")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        Prefs.setBgDone(context, true)
                        onAdvanceStep(6)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bg_done_button")
                ) {
                    Text("Done")
                }
            }

            6 -> {
                WizardTitle("4. Stock Animation")
                WizardBody(
                    "Open Settings, search \"fingerprint\", and turn OFF any built-in fingerprint effect if you only want your custom animation visible."
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        safeStartActivity(context, Intent(Settings.ACTION_SETTINGS))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_system_settings_button")
                ) {
                    Text("Open Settings")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Prefs.setStockStepDone(context, true)
                            onAdvanceStep(7)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stock_skip_button")
                    ) {
                        Text("Skip")
                    }
                    Button(
                        onClick = {
                            Prefs.setStockStepDone(context, true)
                            onAdvanceStep(7)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stock_done_button")
                    ) {
                        Text("Done")
                    }
                }
            }

            8 -> {
                WizardTitle("6. Choose Animation")
                WizardBody(
                    "Keep the active transparent optical animation, browse all 98 Pro animations, or import a custom Lottie .json file."
                )
                if (importMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = importMessage,
                        color = Color(0xFF00F5FF),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onImportJson,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_import_json_button")
                ) {
                    Text("Import .json")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        Prefs.setAnimStepDone(context, true)
                        FpAccessibilityService.instance?.reload()
                        onAdvanceStep(9)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("keep_default_button")
                ) {
                    Text("Keep Default & Finish")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = onOpenChangeAnimation,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wizard_change_animation_button")
        ) {
            Text("Change / Create Animation")
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onSkipToMain,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wizard_skip_to_main_button")
        ) {
            Text("Skip Setup for Now (Open Dashboard)")
        }
    }
}

@Composable
private fun WizardTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun WizardBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Color(0xFFB0BEC5),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun MainScreen(
    shizukuRunning: Boolean,
    serviceEnabled: Boolean,
    hasCustomAnim: Boolean,
    selectedAnimLabel: String?,
    calib: Calib,
    importMessage: String?,
    onEnableService: () -> Unit,
    onTestOverlay: () -> Unit,
    onAdjustPosition: () -> Unit,
    onOpenChangeAnimation: () -> Unit,
    onOpenCustomization: () -> Unit,
    onImportAnimation: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenStockInspector: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animDisplay = when {
        !selectedAnimLabel.isNullOrBlank() -> selectedAnimLabel
        hasCustomAnim -> "Custom (custom.json)"
        else -> "Default (default_ring.json)"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("main_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "FP Animator",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // Banner only if Accessibility Service is disabled
        if (!serviceEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1822)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_disabled_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252)
                        )
                        Text(
                            text = "Accessibility Service is off. Tap Enable to activate standalone fingerprint animations.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(onClick = onEnableService) {
                        Text("Enable")
                    }
                }
            }
        }

        // Status rows
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusRow(
                    label = "Detection Engine",
                    value = if (shizukuRunning) "Standalone + Shizuku" else "Standalone Native",
                    ok = true
                )
                StatusRow(
                    label = "Service enabled",
                    value = if (serviceEnabled) "Enabled" else "Disabled",
                    ok = serviceEnabled
                )
                StatusRow(
                    label = "Animation",
                    value = animDisplay,
                    ok = true
                )
                StatusRow(
                    label = "Speed & Size",
                    value = "${"%.2f".format(calib.speed)}x • ${(calib.size * 100).toInt()}%",
                    ok = true
                )
                StatusRow(
                    label = "Overlay status",
                    value = if (serviceEnabled) "Active (Ready)" else "Enable Service",
                    ok = serviceEnabled
                )
            }
        }

        if (serviceEnabled) {
            Text(
                text = "Standalone Engine Active (100% Shizuku-Free). Lock your phone or trigger a biometric prompt and touch the fingerprint sensor to see your animation.",
                color = Color(0xFF9BA9D0),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (importMessage != null) {
            Text(
                text = importMessage,
                color = Color(0xFF00F5FF),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onOpenChangeAnimation,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("change_animation_button")
        ) {
            Text("Change animation")
        }

        Button(
            onClick = onOpenCustomization,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("customization_button")
        ) {
            Text("Customization")
        }

        Button(
            onClick = onAdjustPosition,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("adjust_position_button")
        ) {
            Text("Adjust position")
        }

        OutlinedButton(
            onClick = onTestOverlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("test_live_overlay_button")
        ) {
            Text("Test Live Overlay (2.4s)")
        }

        Button(
            onClick = onImportAnimation,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("import_animation_button")
        ) {
            Text("Import animation (.json)")
        }

        OutlinedButton(
            onClick = onOpenStockInspector,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("stock_inspector_button")
        ) {
            Text("Biometric & System Inspector")
        }

        OutlinedButton(
            onClick = onOpenSetup,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("setup_button")
        ) {
            Text("Setup")
        }
    }
}

@Composable
private fun SetupOverviewScreen(
    shizukuInstalled: Boolean,
    shizukuRunning: Boolean,
    shizukuAllowed: Boolean,
    serviceEnabled: Boolean,
    batteryOk: Boolean,
    onRefresh: () -> Unit,
    onOpenStockInspector: () -> Unit = {},
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bgDone = Prefs.isBgDone(context)
    val stockDone = Prefs.isStockStepDone(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("setup_overview_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Setup & Permissions",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "FP Animator operates 100% standalone without requiring Shizuku. Tap any item below to adjust its system setting, or tap Done to return.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9BA9D0)
        )

        OverviewItemCard(
            title = "1. Standalone Touch & Biometric Engine",
            ok = true,
            buttonText = if (shizukuInstalled && shizukuRunning && shizukuAllowed) "Hybrid" else "Active",
            onClick = { onRefresh() }
        )

        OverviewItemCard(
            title = "2. Accessibility Service",
            ok = serviceEnabled,
            buttonText = if (serviceEnabled) "Settings" else "Enable",
            onClick = {
                if (!Setup.enableService(context)) {
                    safeStartActivity(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                onRefresh()
            }
        )

        OverviewItemCard(
            title = "3. Battery Optimization",
            ok = batteryOk,
            buttonText = if (batteryOk) "Unrestricted" else "Allow",
            onClick = {
                safeStartActivity(
                    context,
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        )

        OverviewItemCard(
            title = "4. Background Activity",
            ok = bgDone,
            buttonText = "App Info",
            onClick = {
                Prefs.setBgDone(context, true)
                safeStartActivity(
                    context,
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
                onRefresh()
            }
        )

        OverviewItemCard(
            title = "5. Biometric & System Inspector",
            ok = stockDone || true,
            buttonText = "Inspect",
            onClick = {
                Prefs.setStockStepDone(context, true)
                onOpenStockInspector()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("setup_overview_done_button")
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun OverviewItemCard(
    title: String,
    ok: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (ok) "Completed" else "Pending",
                    tint = if (ok) Color(0xFF00E676) else Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            OutlinedButton(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    ok: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB0BEC5)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (ok) Color(0xFF00E676) else Color(0xFFFF5252)
        )
    }
}

internal fun firstIncompleteStep(c: Context): Int {
    if (!Setup.serviceEnabled(c)) return 3
    if (!Setup.batteryOk(c)) return 4
    if (!Prefs.isBgDone(c)) return 5
    if (!Prefs.isStockStepDone(c)) return 6
    if (!Prefs.isCalibDone(c)) return 7
    if (!Prefs.isAnimStepDone(c)) return 8
    return 9
}

private fun importAndValidateLottieJson(context: Context, uri: Uri): Boolean {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
        val result = LottieCompositionFactory.fromJsonInputStreamSync(bytes.inputStream(), null)
        if (result.value == null) return false
        File(context.filesDir, "custom.json").writeBytes(bytes)
        true
    } catch (_: Exception) {
        false
    }
}

private fun openOrGetShizuku(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
    if (launch != null) {
        safeStartActivity(context, launch)
    } else {
        getShizukuInBrowser(context)
    }
}

private fun getShizukuInBrowser(context: Context) {
    safeStartActivity(
        context,
        Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
    )
}

private fun safeStartActivity(context: Context, intent: Intent) {
    runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
