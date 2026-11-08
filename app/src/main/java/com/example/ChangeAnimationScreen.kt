package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.Locale

private enum class ProFilterTab(val label: String) {
    ALL("All Pro HD (98)"),
    VIDEO_LOOPS("60fps Videos (14)"),
    CYBER_HUD("Real Uploads (24)"),
    PRO_DESIGNS("PDF HUDs (60)")
}

@Composable
fun ChangeAnimationScreen(
    onAnimationSelected: (String) -> Unit,
    onClose: () -> Unit,
    @Suppress("UNUSED_PARAMETER") aiViewModel: AiAnimationViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allProItems = remember { ProVideoFolder.items }
    val calib = remember { Prefs.load(context) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(ProFilterTab.ALL) }
    var activeAnimId by remember { mutableStateOf(Prefs.getSelectedAnimId(context)) }
    var activeAnimLabel by remember { mutableStateOf(Prefs.getSelectedAnimLabel(context)) }
    var previewModalItem by remember { mutableStateOf<ProVideoItem?>(null) }

    LaunchedEffect(Unit) {
        ProVideoFrameCache.prewarmGalleryThumbnailsAsync(context, count = 8)
    }

    val filteredItems by remember(searchQuery, selectedTab, allProItems) {
        derivedStateOf {
            val tabFiltered = when (selectedTab) {
                ProFilterTab.ALL -> allProItems
                ProFilterTab.VIDEO_LOOPS -> allProItems.filter { it.isAtlasGrid }
                ProFilterTab.CYBER_HUD -> allProItems.filter { !it.isAtlasGrid && it.index <= 38 }
                ProFilterTab.PRO_DESIGNS -> allProItems.filter { it.index >= 39 }
            }
            val q = searchQuery.trim().lowercase(Locale.US)
            if (q.isEmpty()) {
                tabFiltered
            } else {
                tabFiltered.filter { item ->
                    item.title.lowercase(Locale.US).contains(q) ||
                        item.subtitle.lowercase(Locale.US).contains(q) ||
                        item.badge.lowercase(Locale.US).contains(q) ||
                        "${item.index}" == q.removePrefix("#")
                }
            }
        }
    }

    BackHandler {
        when {
            previewModalItem != null -> previewModalItem = null
            searchQuery.isNotEmpty() -> searchQuery = ""
            selectedTab != ProFilterTab.ALL -> selectedTab = ProFilterTab.ALL
            else -> onClose()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("change_animation_screen")
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = {
                        when {
                            searchQuery.isNotEmpty() -> searchQuery = ""
                            selectedTab != ProFilterTab.ALL -> selectedTab = ProFilterTab.ALL
                            else -> onClose()
                        }
                    },
                    modifier = Modifier.testTag("change_anim_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column {
                    Text(
                        text = "Pro Folder HD Animations (${allProItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ultra-Crisp 512p/1120p HD • Works on Lockscreen & Unlocked Screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Selection Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101828)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.14f),
                                Color(0xFF101828),
                                Color(0xFF00E676).copy(alpha = 0.14f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active Pro Animation",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Active Pro Folder Animation",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9BA9D0)
                        )
                        Text(
                            text = activeAnimLabel ?: allProItems.first().title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    color = Color(0xFF00E676).copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "HD 60fps",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Search ${allProItems.size} HD Pro Folder animations (e.g., #1, Speedometer, Gyro, Portal)...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7B88A8)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Pro Folder",
                    tint = Color(0xFF00E5FF)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = Color.White
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF101522),
                unfocusedContainerColor = Color(0xFF101522),
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF25304B)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_animations_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips inside Pro Folder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProFilterTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { selectedTab = tab },
                    label = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF141C2E),
                        labelColor = Color(0xFFB8C7E8)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Pro Video Folder Grid (129 HD items)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("pro_video_folder_section")
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("pro_video_folder_grid"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(
                    items = filteredItems,
                    key = { it.id },
                    contentType = { "pro_video_grid_card" }
                ) { item ->
                    val isSelected = activeAnimId == item.id
                    val accentColor = Color(item.accentHex)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF19243B) else Color(0xFF111623)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        border = if (isSelected) {
                            BorderStroke(2.dp, Color(0xFF00E676))
                        } else {
                            BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                applyProVideoItem(context, item)
                                activeAnimId = item.id
                                val label = "Pro Folder • #${item.index} ${item.title}"
                                activeAnimLabel = label
                                onAnimationSelected(label)
                            }
                            .testTag("pro_video_card_${item.index}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = accentColor.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(132.dp)
                                    .background(Color.Transparent, CircleShape)
                                    .clickable { previewModalItem = item }
                                    .padding(2.dp)
                            ) {
                                LoopingAssetVideoPreview(
                                    assetPath = item.assetPath,
                                    speed = calib.speed,
                                    isCardSelected = isSelected,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Text(
                                text = "${item.index}. ${item.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9BA9D0),
                                textAlign = TextAlign.Center,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { previewModalItem = item },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(36.dp)
                                ) {
                                    Text("HD View", style = MaterialTheme.typography.labelSmall)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        applyProVideoItem(context, item)
                                        activeAnimId = item.id
                                        val label = "Pro Folder • #${item.index} ${item.title}"
                                        activeAnimLabel = label
                                        onAnimationSelected(label)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isSelected) Color(0xFF00E676) else Color(0xFF00E5FF),
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(36.dp)
                                        .testTag("apply_pro_video_${item.index}")
                                ) {
                                    Text(
                                        text = if (isSelected) "Active ✓" else "Apply",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pro_video_done_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Full-screen HD Preview Modal
    previewModalItem?.let { modalItem ->
        Dialog(onDismissRequest = { previewModalItem = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E17)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color(modalItem.accentHex)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(modalItem.accentHex).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${modalItem.badge} • FULL HD 60FPS PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(modalItem.accentHex),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(240.dp)
                            .background(Color.Black, CircleShape)
                            .padding(4.dp)
                    ) {
                        LoopingAssetVideoPreview(
                            assetPath = modalItem.assetPath,
                            speed = calib.speed,
                            isCardSelected = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "${modalItem.index}. ${modalItem.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = modalItem.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9BA9D0),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { previewModalItem = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Close")
                        }

                        Button(
                            onClick = {
                                applyProVideoItem(context, modalItem)
                                activeAnimId = modalItem.id
                                val label = "Pro Folder • #${modalItem.index} ${modalItem.title}"
                                activeAnimLabel = label
                                onAnimationSelected(label)
                                previewModalItem = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Apply HD", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoopingAssetVideoPreview(
    assetPath: String,
    speed: Float,
    isCardSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val compact = !isCardSelected
    AndroidView(
        factory = { ctx ->
            LoopingAssetVideoView(ctx).apply {
                preferCompactDecode = compact
                tag = assetPath
                setVideoAsset(assetPath, autoStart = isCardSelected, speed = speed, preferCompact = compact)
            }
        },
        update = { videoView ->
            videoView.preferCompactDecode = compact
            if (videoView.tag != assetPath) {
                videoView.tag = assetPath
                videoView.setVideoAsset(assetPath, autoStart = isCardSelected, speed = speed, preferCompact = compact)
            } else {
                videoView.setPlaybackSpeed(speed)
                if (isCardSelected) {
                    videoView.startImmediate()
                } else {
                    videoView.pauseAndReset()
                }
            }
        },
        modifier = modifier
    )
}

private fun applyProVideoItem(
    context: android.content.Context,
    item: ProVideoItem
) {
    val appCtx = context.applicationContext ?: context
    val label = "Pro Folder • #${item.index} ${item.title}"
    Prefs.setSelectedAnim(appCtx, item.id, label)
    Prefs.setSelectedVideoAsset(appCtx, item.assetPath)
    Prefs.setAnimStepDone(appCtx, true)
    ProVideoFrameCache.pinActiveOverlayAtlas(appCtx, item.assetPath)
    ProVideoFrameCache.prewarmAsync(appCtx, item.assetPath, preferCompact = false)
    FpAccessibilityService.instance?.reload()

    // Write fallback custom.json on a background thread so UI never stutters on selection
    Thread {
        runCatching {
            val fallbackDesign = AnimationCatalog.proVideoDesigns.find { it.id == item.fallbackLottieDesignId }
                ?: AnimationCatalog.proVideoDesigns.first()
            File(appCtx.filesDir, "custom.json").writeText(fallbackDesign.lottieJson)
            LottiePrewarmCache.updateActiveAnimationAndPrewarm(appCtx, fallbackDesign.lottieJson)
        }
    }.start()
}
