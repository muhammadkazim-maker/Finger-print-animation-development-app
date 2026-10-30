package com.example

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import java.io.File
import java.util.Locale

private data class IndexedCatalogDesign(
    val category: AnimationCategory,
    val design: AnimationDesign
)

private enum class CatalogSubView {
    BLOCKS,
    PRO_VIDEOS_FOLDER,
    CREATE_AI,
    MY_COLLECTION
}

@Composable
fun ChangeAnimationScreen(
    onAnimationSelected: (String) -> Unit,
    onClose: () -> Unit,
    aiViewModel: AiAnimationViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories = remember { AnimationCatalog.categories }
    val totalCount = remember { AnimationCatalog.totalDesignCount }
    val calib = remember { Prefs.load(context) }

    var subView by remember { mutableStateOf(CatalogSubView.BLOCKS) }
    var selectedCategory by remember { mutableStateOf<AnimationCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeAnimId by remember { mutableStateOf(Prefs.getSelectedAnimId(context)) }
    var activeAnimLabel by remember { mutableStateOf(Prefs.getSelectedAnimLabel(context)) }
    var myCollection by remember { mutableStateOf(MyCollectionStore.loadAll(context)) }

    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()

    val proVideos = remember { AnimationCatalog.proVideoDesigns }
    val proVideoCategory = remember(proVideos) {
        AnimationCategory(
            number = 0,
            id = "pro_video_vfx",
            title = "0. Featured Pro VFX (6 Reference Videos)",
            shortTitle = "Pro Video VFX",
            subtitle = "Photorealistic multi-pass HDR shader animations",
            accentHex = 0xFF00E5FFL,
            secondaryHex = 0xFF651FFFL,
            designs = proVideos
        )
    }

    val searchResults by remember(searchQuery, categories, proVideoCategory) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase(Locale.US)
            if (q.isEmpty()) {
                emptyList()
            } else {
                val allCats = listOf(proVideoCategory) + categories
                allCats.flatMap { cat ->
                    cat.designs.filter { d ->
                        d.name.lowercase(Locale.US).contains(q) ||
                            d.styleLabel.lowercase(Locale.US).contains(q) ||
                            d.description.lowercase(Locale.US).contains(q) ||
                            "#${d.index}" == q
                    }.map { d -> IndexedCatalogDesign(cat, d) }
                }
            }
        }
    }

    BackHandler {
        when {
            subView != CatalogSubView.BLOCKS -> {
                subView = CatalogSubView.BLOCKS
            }
            searchQuery.isNotEmpty() -> searchQuery = ""
            selectedCategory != null -> selectedCategory = null
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
                            subView != CatalogSubView.BLOCKS -> subView = CatalogSubView.BLOCKS
                            searchQuery.isNotEmpty() -> searchQuery = ""
                            selectedCategory != null -> selectedCategory = null
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
                        text = when (subView) {
                            CatalogSubView.PRO_VIDEOS_FOLDER -> "Pro Video Animations Folder (${ProVideoFolder.items.size})"
                            CatalogSubView.CREATE_AI -> "Create Animation (Gemini AI)"
                            CatalogSubView.MY_COLLECTION -> "My Collection (${myCollection.size})"
                            CatalogSubView.BLOCKS -> selectedCategory?.title ?: "Fingerprint Animation Catalog"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (subView) {
                            CatalogSubView.PRO_VIDEOS_FOLDER -> "Folder: assets/pro_videos/ • Exact original video animations"
                            CatalogSubView.CREATE_AI -> "Describe any animation & Gemini will generate and save it"
                            CatalogSubView.MY_COLLECTION -> "Your AI-generated custom animations stored on device"
                            CatalogSubView.BLOCKS -> when {
                                searchQuery.isNotBlank() -> "${searchResults.size} matching designs across all 20 sheets"
                                selectedCategory != null -> "${selectedCategory!!.designs.size} animations • Tap any design to apply"
                                else -> "Pro Video Folder + ${categories.size} Collection Blocks • $totalCount Total Animations"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9BA9D0)
                    )
                }
            }

            if (subView != CatalogSubView.BLOCKS || selectedCategory != null || searchQuery.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        subView = CatalogSubView.BLOCKS
                        searchQuery = ""
                        selectedCategory = null
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("all_blocks_button")
                ) {
                    Text("All Blocks")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Bar: Pro Video Folder + Create Animation (Gemini AI) + My Collection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    subView = CatalogSubView.PRO_VIDEOS_FOLDER
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (subView == CatalogSubView.PRO_VIDEOS_FOLDER) Color(0xFF00E5FF) else Color(0xFF14243B),
                    contentColor = if (subView == CatalogSubView.PRO_VIDEOS_FOLDER) Color.Black else Color(0xFF00E5FF)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pro_video_folder_button")
            ) {
                Text(
                    text = "Pro Folder (${ProVideoFolder.items.size})",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = {
                    subView = CatalogSubView.CREATE_AI
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (subView == CatalogSubView.CREATE_AI) Color(0xFF00E5FF) else Color(0xFF651FFF),
                    contentColor = if (subView == CatalogSubView.CREATE_AI) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("create_animation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Create Animation",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Create AI",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = {
                    myCollection = MyCollectionStore.loadAll(context)
                    subView = CatalogSubView.MY_COLLECTION
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (subView == CatalogSubView.MY_COLLECTION) Color(0xFF00E676) else Color(0xFF1A2236),
                    contentColor = if (subView == CatalogSubView.MY_COLLECTION) Color.Black else Color(0xFF00E5FF)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("my_collection_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CollectionsBookmark,
                    contentDescription = "My Collection",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Saved (${myCollection.size})",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (subView) {
            CatalogSubView.PRO_VIDEOS_FOLDER -> {
                ProVideoFolderSection(
                    items = ProVideoFolder.items,
                    activeAnimId = activeAnimId,
                    speed = calib.speed,
                    onApply = { videoItem ->
                        applyProVideoItem(context, videoItem)
                        activeAnimId = videoItem.id
                        val label = "Pro Folder • #${videoItem.index} ${videoItem.title}"
                        activeAnimLabel = label
                        onAnimationSelected("Applied Original Video: ${videoItem.title}")
                    },
                    onDone = onClose,
                    modifier = Modifier.weight(1f)
                )
            }

            CatalogSubView.CREATE_AI -> {
                CreateAnimationAiSection(
                    uiState = aiUiState,
                    speed = calib.speed,
                    activeAnimId = activeAnimId,
                    onGenerate = { prompt, refImages ->
                        aiViewModel.generateAnimation(context, prompt, refImages) { updated ->
                            myCollection = updated
                        }
                    },
                    onApplySaved = { saved ->
                        applySavedAiDesign(context, saved)
                        activeAnimId = saved.id
                        val label = "My Collection • ${saved.title}"
                        activeAnimLabel = label
                        onAnimationSelected("Applied AI Animation: ${saved.title}")
                    },
                    onOpenMyCollection = {
                        myCollection = MyCollectionStore.loadAll(context)
                        subView = CatalogSubView.MY_COLLECTION
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            CatalogSubView.MY_COLLECTION -> {
                MyCollectionSection(
                    items = myCollection,
                    activeAnimId = activeAnimId,
                    speed = calib.speed,
                    onCreateNew = {
                        aiViewModel.resetState()
                        subView = CatalogSubView.CREATE_AI
                    },
                    onApply = { saved ->
                        applySavedAiDesign(context, saved)
                        activeAnimId = saved.id
                        val label = "My Collection • ${saved.title}"
                        activeAnimLabel = label
                        onAnimationSelected("Applied: $label")
                    },
                    onDelete = { saved ->
                        myCollection = MyCollectionStore.delete(context, saved.id)
                    },
                    onDone = onClose,
                    modifier = Modifier.weight(1f)
                )
            }

            CatalogSubView.BLOCKS -> {
                // Quick Search Bar to jump to any of the 922 designs or filter inside a sheet
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedCategory != null) {
                                "Filter in ${selectedCategory!!.shortTitle} or search all..."
                            } else {
                                "Search all $totalCount designs (e.g. Butterfly, Crown, Fire, Heart, DNA)..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7B88A8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search animations",
                            tint = Color(0xFF00E5FF)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFF9BA9D0)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF101420),
                        unfocusedContainerColor = Color(0xFF101420),
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF26304A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_animations_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                val currentCat = selectedCategory
                when {
                    searchQuery.isNotBlank() -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("search_results_grid"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(searchResults, key = { it.design.id }) { item ->
                                DesignGridCard(
                                    category = item.category,
                                    design = item.design,
                                    isSelected = activeAnimId == item.design.id,
                                    speed = calib.speed,
                                    showSheetBadge = true,
                                    onSelect = {
                                        applyDesign(context, item.category, item.design)
                                        activeAnimId = item.design.id
                                        val label = "${item.category.shortTitle} • #${item.design.index} ${item.design.name}"
                                        activeAnimLabel = label
                                        onAnimationSelected("Applied: $label")
                                    }
                                )
                            }
                        }
                    }

                    currentCat == null -> {
                        // Pinned My Collection Banner + 20 Collection Blocks shown in 2-column Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("category_list"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            // Full-width "My Collection" Block Card at top of grid
                            item(span = { GridItemSpan(2) }, key = "my_collection_banner_block") {
                                val hasActiveAi = myCollection.any { it.id == activeAnimId }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B2D)),
                                    shape = RoundedCornerShape(18.dp),
                                    border = if (hasActiveAi) {
                                        BorderStroke(2.dp, Color(0xFF00E676))
                                    } else {
                                        BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.55f))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            myCollection = MyCollectionStore.loadAll(context)
                                            subView = CatalogSubView.MY_COLLECTION
                                        }
                                        .testTag("my_collection_block_card")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFF651FFF).copy(alpha = 0.24f),
                                                        Color(0xFF141B2D),
                                                        Color(0xFF00E5FF).copy(alpha = 0.18f)
                                                    )
                                                )
                                            )
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .background(Color(0xFF07090F), RoundedCornerShape(14.dp))
                                                    .padding(6.dp)
                                            ) {
                                                val latestAi = myCollection.firstOrNull()
                                                if (latestAi != null) {
                                                    LottieJsonPreview(
                                                        lottieJson = latestAi.lottieJson,
                                                        cacheKey = latestAi.id,
                                                        speed = calib.speed,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00E5FF),
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                }
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "My Collection (AI Generated)",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (hasActiveAi) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Active in My Collection",
                                                            tint = Color(0xFF00E676),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (myCollection.isEmpty()) {
                                                        "No custom AI animations yet • Tap Create Animation above!"
                                                    } else {
                                                        "${myCollection.size} saved AI animations • Tap to open & apply"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF9BA9D0)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "${myCollection.size} Saved",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF00E5FF),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Separate Folder Card: "Pro Video Animations Folder (6 Videos)"
                            item(span = { GridItemSpan(2) }, key = "pro_video_folder_block") {
                                val hasActiveProVideo = ProVideoFolder.items.any { it.id == activeAnimId }
                                val firstVideo = ProVideoFolder.items.first()
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101829)),
                                    shape = RoundedCornerShape(18.dp),
                                    border = if (hasActiveProVideo) {
                                        BorderStroke(2.dp, Color(0xFF00E676))
                                    } else {
                                        BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            subView = CatalogSubView.PRO_VIDEOS_FOLDER
                                        }
                                        .testTag("pro_video_folder_card")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFF0066FF).copy(alpha = 0.28f),
                                                        Color(0xFF101829),
                                                        Color(0xFF00E5FF).copy(alpha = 0.22f)
                                                    )
                                                )
                                            )
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .background(Color.Transparent, CircleShape)
                                                    .padding(2.dp)
                                            ) {
                                                LoopingAssetVideoPreview(
                                                    assetPath = firstVideo.assetPath,
                                                    speed = calib.speed,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "📁 Pro Video Animations Folder",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (hasActiveProVideo) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Active in Pro Video Folder",
                                                            tint = Color(0xFF00E676),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${ProVideoFolder.items.size} Uploaded Videos & Animated Designs (exact design) • Tap to open",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF9BA9D0)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = Color(0xFF00E676).copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "Open (${ProVideoFolder.items.size})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF00E676),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            items(categories, key = { it.id }) { category ->
                                val previewDesign = category.designs.first()
                                val accentColor = Color(category.accentHex)
                                val secondaryColor = Color(category.secondaryHex)
                                val hasSelectedInCat = category.designs.any { it.id == activeAnimId }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121622)),
                                    shape = RoundedCornerShape(18.dp),
                                    border = if (hasSelectedInCat) {
                                        BorderStroke(2.dp, Color(0xFF00E676))
                                    } else {
                                        BorderStroke(1.dp, accentColor.copy(alpha = 0.38f))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCategory = category }
                                        .testTag("category_card_${category.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        accentColor.copy(alpha = 0.14f),
                                                        Color(0xFF121622),
                                                        secondaryColor.copy(alpha = 0.10f)
                                                    )
                                                )
                                            )
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = accentColor.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Sheet #${category.number}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accentColor,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            if (hasSelectedInCat) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Active category",
                                                    tint = Color(0xFF00E676),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .background(Color(0xFF07090F), RoundedCornerShape(14.dp))
                                                .padding(6.dp)
                                        ) {
                                            LottieJsonPreview(
                                                lottieJson = previewDesign.lottieJson,
                                                cacheKey = previewDesign.id,
                                                speed = calib.speed,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Text(
                                            text = category.shortTitle,
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
                                            text = category.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF9BA9D0),
                                            textAlign = TextAlign.Center,
                                            minLines = 2,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Surface(
                                            color = Color(0xFF1B2234),
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "${category.designs.size} Designs",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom row: Restore Default Ring or Done
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    runCatching { File(context.filesDir, "custom.json").delete() }
                                    Prefs.setSelectedAnim(context, null, null)
                                    FpAccessibilityService.instance?.reload()
                                    activeAnimId = null
                                    activeAnimLabel = null
                                    onAnimationSelected("Restored Default (default_ring.json)")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("restore_default_anim_button")
                            ) {
                                Text("Use Default Ring")
                            }

                            Button(
                                onClick = onClose,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("close_categories_button")
                            ) {
                                Text("Done")
                            }
                        }
                    }

                    else -> {
                        // All Designs Grid inside Selected Sheet Block
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("designs_grid"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(currentCat.designs, key = { it.id }) { design ->
                                DesignGridCard(
                                    category = currentCat,
                                    design = design,
                                    isSelected = activeAnimId == design.id,
                                    speed = calib.speed,
                                    showSheetBadge = false,
                                    onSelect = {
                                        applyDesign(context, currentCat, design)
                                        activeAnimId = design.id
                                        val label = "${currentCat.shortTitle} • #${design.index} ${design.name}"
                                        activeAnimLabel = label
                                        onAnimationSelected("Applied: $label")
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onClose,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("finish_design_selection_button")
                            ) {
                                Text(
                                    text = if (activeAnimLabel != null) {
                                        "Done ($activeAnimLabel)"
                                    } else {
                                        "Done"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAnimationAiSection(
    uiState: AiGenUiState,
    speed: Float,
    activeAnimId: String?,
    onGenerate: (String, List<ReferenceImageAttachment>) -> Unit,
    onApplySaved: (SavedAiAnimation) -> Unit,
    onOpenMyCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var promptText by remember { mutableStateOf("") }
    var referenceImages by remember { mutableStateOf<List<ReferenceImageAttachment>>(emptyList()) }
    var isProcessingImage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isProcessingImage = true
                val loaded = uris.take(3).mapNotNull { uri ->
                    ImageReferenceAnalyzer.loadAndAnalyzeUri(context, uri)
                }
                if (loaded.isNotEmpty()) {
                    val combined = (referenceImages + loaded)
                        .distinctBy { it.uri.toString() }
                        .take(3)
                    referenceImages = combined
                }
                isProcessingImage = false
            }
        }
    }

    val samplePrompts = remember {
        listOf(
            "Golden royal crown with fiery orange solar sparks and rotating luxury ring",
            "Cyberpunk neon butterfly wings flapping in electric cyan and violet",
            "Glowing crimson & pink heartbeat pulse with emerald matrix code rain",
            "Crystalline ice snowflake with deep blue galaxy orbital rings"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("create_animation_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121726)),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF651FFF).copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Describe Your Custom Fingerprint Animation",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = {
                        Text(
                            text = "e.g. Match the attached image's glowing shape and colors, or describe your custom design...",
                            color = Color(0xFF7B88A8)
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0B0E17),
                        unfocusedContainerColor = Color(0xFF0B0E17),
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF28324E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input")
                )

                // Reference Image Picker + Preview Section
                Surface(
                    color = Color(0xFF0B0E17),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (referenceImages.isNotEmpty()) Color(0xFF00E676).copy(alpha = 0.65f)
                        else Color(0xFF00E5FF).copy(alpha = 0.38f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reference_image_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reference Images (${referenceImages.size}/3)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Attach screenshots or icons so Gemini matches their exact colors & silhouette",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9BA9D0)
                                )
                            }

                            if (referenceImages.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { referenceImages = emptyList() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("clear_all_ref_images_button")
                                ) {
                                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (referenceImages.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("attached_reference_images_row"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                referenceImages.forEachIndexed { idx, item ->
                                    val sig = item.signature
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151C2E)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(sig.primaryHex).copy(alpha = 0.7f)),
                                        modifier = Modifier
                                            .width(142.dp)
                                            .testTag("reference_image_card_$idx")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(84.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black)
                                            ) {
                                                Image(
                                                    bitmap = item.bitmap.asImageBitmap(),
                                                    contentDescription = "Reference image ${idx + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                IconButton(
                                                    onClick = {
                                                        referenceImages = referenceImages.filterIndexed { i, _ -> i != idx }
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(26.dp)
                                                        .background(Color(0xCC000000), CircleShape)
                                                        .testTag("remove_ref_image_$idx")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Remove reference image",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }

                                            // Extracted vibrant palette chips
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                listOf(sig.primaryHex, sig.secondaryHex, sig.accentHex).forEach { hex ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(hex))
                                                    )
                                                }
                                                Text(
                                                    text = ImageReferenceAnalyzer.formatHex(sig.primaryHex),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFD0DCFF),
                                                    maxLines = 1
                                                )
                                            }

                                            Text(
                                                text = sig.shapeSummary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF9BA9D0),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = referenceImages.size < 3 && !isProcessingImage && uiState !is AiGenUiState.Loading,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1A243B),
                                contentColor = Color(0xFF00E5FF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_reference_image_button")
                        ) {
                            if (isProcessingImage) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00E5FF),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Image Colors & Shape...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add Reference Image",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (referenceImages.isEmpty()) {
                                        "Add Reference Image(s) with Prompt"
                                    } else {
                                        "Add Another Image (${referenceImages.size}/3 Attached)"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Quick Prompt Ideas (Tap to fill):",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9BA9D0)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    samplePrompts.forEachIndexed { idx, sample ->
                        Surface(
                            color = Color(0xFF1A2236),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { promptText = sample }
                                .testTag("sample_prompt_$idx")
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD0DCFF),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = { onGenerate(promptText, referenceImages) },
                    enabled = uiState !is AiGenUiState.Loading && !isProcessingImage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_ai_animation_button")
                ) {
                    if (uiState is AiGenUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (referenceImages.isNotEmpty()) {
                                "Generating from Image + Prompt..."
                            } else {
                                "Generating with Gemini AI..."
                            },
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (referenceImages.isNotEmpty()) {
                                "Generate from Image(${referenceImages.size}) + Prompt"
                            } else {
                                "Generate & Save to My Collection"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        when (uiState) {
            is AiGenUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B151E)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5252)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_error_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Generation Error",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }

            is AiGenUiState.Success -> {
                val anim = uiState.animation
                val isApplied = activeAnimId == anim.id
                val primaryColor = Color(anim.primaryHex)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141D30)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, Color(0xFF00E676)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_generated_preview_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF00E676).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Saved to My Collection ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(136.dp)
                                .background(Color(0xFF06080E), RoundedCornerShape(18.dp))
                                .padding(8.dp)
                        ) {
                            LottieJsonPreview(
                                lottieJson = anim.lottieJson,
                                cacheKey = anim.id,
                                speed = speed,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = anim.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            color = primaryColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = anim.styleBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "\"${anim.prompt}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9BA9D0),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onApplySaved(anim) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E676),
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("apply_generated_ai_button")
                            ) {
                                Text(
                                    text = if (isApplied) "Active ✓" else "Apply Now",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenMyCollection,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("open_collection_after_gen_button")
                            ) {
                                Text("View Collection")
                            }
                        }
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun MyCollectionSection(
    items: List<SavedAiAnimation>,
    activeAnimId: String?,
    speed: Float,
    onCreateNew: () -> Unit,
    onApply: (SavedAiAnimation) -> Unit,
    onDelete: (SavedAiAnimation) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("my_collection_section")
    ) {
        if (items.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121622)),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFF28324E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Your Collection is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'Create Animation' to generate custom fingerprint animations from any text prompt using Gemini AI. Every generated animation is automatically saved here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9BA9D0),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onCreateNew,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("empty_collection_create_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create First AI Animation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("my_collection_grid"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val isSelected = activeAnimId == item.id
                    val accentColor = Color(item.primaryHex)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1A233A) else Color(0xFF141824)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (isSelected) {
                            BorderStroke(2.dp, Color(0xFF00E676))
                        } else {
                            BorderStroke(1.dp, accentColor.copy(alpha = 0.42f))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onApply(item) }
                            .testTag("collection_item_${item.id}")
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
                                        text = item.styleBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDelete(item) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("delete_collection_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete animation",
                                        tint = Color(0xFFFF8A80),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(92.dp)
                                    .background(Color(0xFF06080E), RoundedCornerShape(14.dp))
                                    .padding(6.dp)
                            ) {
                                LottieJsonPreview(
                                    lottieJson = item.lottieJson,
                                    cacheKey = item.id,
                                    speed = speed,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = item.prompt,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9BA9D0),
                                textAlign = TextAlign.Center,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            FilledTonalButton(
                                onClick = { onApply(item) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("apply_collection_${item.id}")
                            ) {
                                Text(if (isSelected) "Active ✓" else "Select")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("collection_done_button")
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun DesignGridCard(
    category: AnimationCategory,
    design: AnimationDesign,
    isSelected: Boolean,
    speed: Float,
    showSheetBadge: Boolean,
    onSelect: () -> Unit
) {
    val accentColor = Color(category.accentHex)
    val c1 = design.primaryColor
    val designAccent = Color(c1[0], c1[1], c1[2], 1f)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1A233A) else Color(0xFF141824)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF00E676))
        } else {
            BorderStroke(1.dp, designAccent.copy(alpha = 0.38f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("design_card_${design.index}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showSheetBadge) {
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Sheet #${category.number}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .background(Color(0xFF06080E), RoundedCornerShape(14.dp))
                    .padding(6.dp)
            ) {
                LottieJsonPreview(
                    lottieJson = design.lottieJson,
                    cacheKey = design.id,
                    speed = speed,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "${design.index}. ${design.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                color = designAccent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = design.styleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = designAccent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            FilledTonalButton(
                onClick = onSelect,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("apply_design_${design.index}")
            ) {
                Text(if (isSelected) "Active ✓" else "Select")
            }
        }
    }
}

@Composable
private fun ProVideoFolderSection(
    items: List<ProVideoItem>,
    activeAnimId: String?,
    speed: Float,
    onApply: (ProVideoItem) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("pro_video_folder_section")
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101829)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📁 Folder: assets/pro_videos/ (${items.size} Original Video & Image Animations)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All ${items.size} uploaded videos and animated designs are stored in this separate folder with their exact original design and transparent alpha backgrounds (zero black box).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9BA9D0)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
            items(items, key = { it.id }) { item ->
                val isSelected = activeAnimId == item.id
                val accentColor = Color(item.accentHex)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1A233A) else Color(0xFF121622)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (isSelected) {
                        BorderStroke(2.dp, Color(0xFF00E676))
                    } else {
                        BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApply(item) }
                        .testTag("pro_video_card_${item.index}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(116.dp)
                                .background(Color.Transparent, CircleShape)
                                .padding(2.dp)
                        ) {
                            LoopingAssetVideoPreview(
                                assetPath = item.assetPath,
                                speed = speed,
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

                        FilledTonalButton(
                            onClick = { onApply(item) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("apply_pro_video_${item.index}")
                        ) {
                            Text(if (isSelected) "Active ✓" else "Select")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pro_video_done_button")
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun LoopingAssetVideoPreview(
    assetPath: String,
    speed: Float,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            LoopingAssetVideoView(ctx).apply {
                tag = assetPath
                setVideoAsset(assetPath, autoStart = true, speed = speed)
            }
        },
        update = { videoView ->
            if (videoView.tag != assetPath) {
                videoView.tag = assetPath
                videoView.setVideoAsset(assetPath, autoStart = true, speed = speed)
            } else {
                videoView.setPlaybackSpeed(speed)
            }
        },
        modifier = modifier
    )
}

private fun applyProVideoItem(
    context: android.content.Context,
    item: ProVideoItem
) {
    // Also write fallback Lottie JSON for any synchronous composition consumers
    val fallbackDesign = AnimationCatalog.proVideoDesigns.find { it.id == item.fallbackLottieDesignId }
        ?: AnimationCatalog.proVideoDesigns.first()
    File(context.filesDir, "custom.json").writeText(fallbackDesign.lottieJson)
    val label = "Pro Folder • #${item.index} ${item.title}"
    Prefs.setSelectedAnim(context, item.id, label)
    Prefs.setSelectedVideoAsset(context, item.assetPath)
    Prefs.setAnimStepDone(context, true)
    ProVideoFrameCache.prewarmAsync(context, item.assetPath)
    LottiePrewarmCache.updateActiveAnimationAndPrewarm(context, fallbackDesign.lottieJson)
    FpAccessibilityService.instance?.reload()
}

private fun applyDesign(
    context: android.content.Context,
    category: AnimationCategory,
    design: AnimationDesign
) {
    val json = design.lottieJson
    File(context.filesDir, "custom.json").writeText(json)
    val label = "${category.shortTitle} • #${design.index} ${design.name}"
    Prefs.setSelectedAnim(context, design.id, label)
    Prefs.setSelectedVideoAsset(context, null)
    Prefs.setAnimStepDone(context, true)
    LottiePrewarmCache.updateActiveAnimationAndPrewarm(context, json)
    FpAccessibilityService.instance?.reload()
}

private fun applySavedAiDesign(
    context: android.content.Context,
    saved: SavedAiAnimation
) {
    val json = saved.lottieJson
    File(context.filesDir, "custom.json").writeText(json)
    val label = "My Collection • ${saved.title}"
    Prefs.setSelectedAnim(context, saved.id, label)
    Prefs.setSelectedVideoAsset(context, null)
    Prefs.setAnimStepDone(context, true)
    LottiePrewarmCache.updateActiveAnimationAndPrewarm(context, json)
    FpAccessibilityService.instance?.reload()
}

@Composable
private fun LottieJsonPreview(
    lottieJson: String,
    cacheKey: String,
    speed: Float,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            LottieAnimationView(ctx).apply {
                LottiePrewarmCache.bindJsonToView(
                    view = this,
                    lottieJson = lottieJson,
                    cacheKey = cacheKey,
                    speed = speed
                )
            }
        },
        update = { view ->
            LottiePrewarmCache.bindJsonToView(
                view = view,
                lottieJson = lottieJson,
                cacheKey = cacheKey,
                speed = speed
            )
        },
        modifier = modifier
    )
}
