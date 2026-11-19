package com.vinithreddybanda.whatsapstatus

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredGridItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.vinithreddybanda.whatsapstatus.model.Status
import com.vinithreddybanda.whatsapstatus.ui.theme.AppPalette
import com.vinithreddybanda.whatsapstatus.ui.theme.AppType
import com.vinithreddybanda.whatsapstatus.ui.theme.LocalAppPalette
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val palette = LocalAppPalette.current
    val backdrop = rememberLayerBackdrop()

    var hasPermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var selectedStatus by remember { mutableStateOf<Status?>(null) }
    var actionStatus by remember { mutableStateOf<Status?>(null) }
    val actionVisibility = remember { MutableTransitionState(false) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = checkStoragePermission(context)
    }
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val requestAccess = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
            }
            try {
                settingsLauncher.launch(intent)
            } catch (_: Exception) {
                try {
                    settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (_: Exception) {
                    Toast.makeText(context, "Open app settings to allow file access", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkStoragePermission(context)
                if (hasPermission) viewModel.getStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.getStatuses()
    }

    LaunchedEffect(selectedStatus) {
        if (selectedStatus != null) actionStatus = selectedStatus
        actionVisibility.targetState = selectedStatus != null
    }

    LaunchedEffect(actionVisibility.isIdle, actionVisibility.currentState) {
        if (actionVisibility.isIdle && !actionVisibility.currentState) actionStatus = null
    }

    val tabs = remember { listOf(StatusTab.All, StatusTab.Images, StatusTab.Videos, StatusTab.Saved) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    LaunchedEffect(viewModel.selectedTab) {
        val index = tabs.indexOf(viewModel.selectedTab)
        if (index >= 0 && pagerState.currentPage != index) pagerState.animateScrollToPage(index)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onTabSelected(tabs[page])
        }
    }

    BackHandler(enabled = selectedStatus != null) {
        selectedStatus = null
    }

    Box(Modifier.fillMaxSize().background(palette.background)) {
        Column(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            if (hasPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Statuses",
                        style = AppType.body.copy(
                            color = palette.foreground,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            if (!hasPermission) {
                AccessScreen(
                    modifier = Modifier.weight(1f),
                    palette = palette,
                    onGrant = requestAccess
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 0,
                    key = { tabs[it].title }
                ) { page ->
                    val tab = tabs[page]
                    val allStatuses = viewModel.getStatusesForTab(tab)
                    val visibleLimit = viewModel.getVisibleStatusesForTab(tab).size
                    val statuses = remember(allStatuses, visibleLimit) {
                        allStatuses.take(visibleLimit)
                    }
                    val gridState = rememberLazyStaggeredGridState()

                    LaunchedEffect(gridState, tab, statuses.size, viewModel.isFetching) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        }.collect { lastVisible ->
                            if (statuses.isNotEmpty() && lastVisible >= statuses.size - 6) {
                                viewModel.loadMore(tab)
                            }
                        }
                    }

                    when {
                        statuses.isNotEmpty() -> {
                            LazyVerticalStaggeredGrid(
                                state = gridState,
                                columns = StaggeredGridCells.Adaptive(150.dp),
                                contentPadding = PaddingValues(
                                    start = 7.dp,
                                    end = 7.dp,
                                    top = 4.dp,
                                    bottom = WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding() + 76.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalItemSpacing = 7.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                staggeredGridItems(
                                    items = statuses,
                                    key = { it.path },
                                    contentType = { "media" }
                                ) { status ->
                                    StatusCard(
                                        status = status,
                                        palette = palette,
                                        onOpen = { openStatusFile(context, status.file) },
                                        onLongPress = { selectedStatus = status }
                                    )
                                }
                            }
                        }
                        (tab != StatusTab.Saved && viewModel.isFetching) ||
                            (tab == StatusTab.Saved && viewModel.isSavedFetching) -> LoadingGallery(palette)
                        else -> EmptyGallery(
                            palette = palette,
                            message = "No statuses found"
                        )
                    }
                }
            }
        }

        if (hasPermission) {
            GlassBottomBar(
                backdrop = backdrop,
                palette = palette,
                selectedIndex = pagerState.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter),
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
            )
        }

        AnimatedVisibility(
            visibleState = actionVisibility,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) +
                scaleIn(initialScale = 0.93f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
            exit = fadeOut() +
                slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                scaleOut(targetScale = 0.97f)
        ) {
            actionStatus?.let { status ->
            StatusActionsOverlay(
                backdrop = backdrop,
                palette = palette,
                status = status,
                isSaved = viewModel.selectedTab == StatusTab.Saved,
                onDismiss = { selectedStatus = null },
                onSave = {
                    viewModel.saveStatus(status) { success ->
                        Toast.makeText(context, if (success) "Saved" else "Couldn't save", Toast.LENGTH_SHORT).show()
                        selectedStatus = null
                    }
                },
                onDelete = {
                    viewModel.deleteStatus(status) { success ->
                        Toast.makeText(context, if (success) "Deleted" else "Couldn't delete", Toast.LENGTH_SHORT).show()
                        selectedStatus = null
                    }
                },
                onShare = {
                    selectedStatus = null
                    shareStatusFile(context, status.file, share = true)
                },
                onRepost = {
                    selectedStatus = null
                    shareStatusFile(context, status.file, share = false)
                }
            )
            }
        }
    }
}

@Composable
private fun GlassBottomBar(
    backdrop: Backdrop,
    palette: AppPalette,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val outlineIcons = listOf(
        Icons.Outlined.FolderOpen,
        Icons.Outlined.PhotoLibrary,
        Icons.Outlined.Movie,
        Icons.Outlined.BookmarkBorder
    )
    val filledIcons = listOf(
        Icons.Outlined.Folder,
        Icons.Outlined.Image,
        Icons.Outlined.Videocam,
        Icons.Outlined.Bookmark
    )
    val descriptions = listOf("All statuses", "Photos", "Videos", "Saved statuses")
    val haptics = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(selectedIndex.toFloat()) }
    var settledPosition by remember { mutableStateOf(selectedIndex.toFloat()) }
    var dragStretch by remember { mutableStateOf(0f) }

    LaunchedEffect(selectedIndex) {
        if (!isDragging) {
            settledPosition = selectedIndex.toFloat()
            dragPosition = selectedIndex.toFloat()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 258.dp)
                .fillMaxWidth()
                .height(56.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(28.dp) },
                    effects = {
                        blur(24.dp.toPx())
                        lens(22.dp.toPx(), 30.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.92f) },
                    shadow = { null },
                    onDrawSurface = { drawRect(palette.glass.copy(alpha = 0.78f)) }
                )
                .border(0.8.dp, palette.glassBorder.copy(alpha = 0.9f), RoundedCornerShape(28.dp))
                .pointerInput(Unit) {
                    val slotWidthPx = size.width / 4f
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragPosition = (offset.x / slotWidthPx).coerceIn(0f, 3f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val movement = dragAmount.x / slotWidthPx
                            dragPosition = (dragPosition + movement).coerceIn(0f, 3f)
                            dragStretch = (kotlin.math.abs(movement) * 1.6f).coerceIn(0f, 0.34f)
                        },
                        onDragEnd = {
                            val destination = dragPosition.roundToInt().coerceIn(0, 3)
                            settledPosition = destination.toFloat()
                            isDragging = false
                            dragStretch = 0f
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(destination)
                        },
                        onDragCancel = {
                            isDragging = false
                            dragStretch = 0f
                            dragPosition = settledPosition
                        }
                    )
                }
        ) {
            val slotWidth = maxWidth / 4
            val indicatorPosition by animateFloatAsState(
                targetValue = if (isDragging) dragPosition else settledPosition,
                animationSpec = spring(dampingRatio = 0.58f, stiffness = 430f),
                label = "liquidTabPosition"
            )
            val indicatorStretch by animateFloatAsState(
                targetValue = if (isDragging) 1.16f + dragStretch else 1f,
                animationSpec = spring(dampingRatio = 0.48f, stiffness = 560f),
                label = "liquidTabStretch"
            )
            val indicatorHeight by animateFloatAsState(
                targetValue = if (isDragging) 0.94f else 1f,
                animationSpec = spring(dampingRatio = 0.54f, stiffness = 500f),
                label = "liquidTabHeight"
            )

            Box(
                Modifier
                    .offset(x = slotWidth * indicatorPosition + (slotWidth - 42.dp) / 2)
                    .width(42.dp)
                    .height(42.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        scaleX = indicatorStretch
                        scaleY = indicatorHeight
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.34f),
                                palette.accent.copy(alpha = 0.58f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(0.9.dp, Color.White.copy(alpha = 0.48f), CircleShape)
            )

            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                outlineIcons.forEachIndexed { index, icon ->
                    val highlightedIndex = if (isDragging) dragPosition.roundToInt().coerceIn(0, 3) else selectedIndex
                    val selected = highlightedIndex == index
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 0.92f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tabIconScale$index"
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                settledPosition = index.toFloat()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = selected,
                            transitionSpec = {
                                (androidx.compose.animation.fadeIn(tween(100)) +
                                    androidx.compose.animation.scaleIn(
                                        initialScale = 0.72f,
                                        animationSpec = spring(dampingRatio = 0.58f)
                                    )) togetherWith
                                    (androidx.compose.animation.fadeOut(tween(75)) +
                                        androidx.compose.animation.scaleOut(targetScale = 0.82f))
                            },
                            label = "tabIconMorph$index"
                        ) { active ->
                            Image(
                                painter = rememberVectorPainter(if (active) filledIcons[index] else icon),
                                contentDescription = descriptions[index],
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                    if (active) palette.foreground else palette.muted
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusCard(
    status: Status,
    palette: AppPalette,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val cacheKey = remember(status.path, status.timestamp) { "${status.path}:${status.timestamp}" }
    val request = remember(cacheKey, status.isVideo) {
        ImageRequest.Builder(context)
            .data(status.file)
            .size(480)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .apply { if (status.isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
            .build()
    }
    val ratio = status.aspectRatio ?: if (status.isVideo) 0.5625f else 0.82f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "mediaPress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(15.dp))
            .background(palette.placeholder)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
                onLongClick = onLongPress
            )
            .aspectRatio(ratio)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = request),
            contentDescription = if (status.isVideo) "Open video status" else "Open photo status",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (status.isVideo) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberVectorPainter(Icons.Outlined.PlayArrow),
                    contentDescription = "Play",
                    modifier = Modifier.size(27.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
            }
        }
    }
}

@Composable
private fun LoadingGallery(palette: AppPalette) {
    val ratios = listOf(0.78f, 1.12f, 0.92f, 1.28f, 0.84f, 1.02f)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(start = 7.dp, end = 7.dp, top = 4.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalItemSpacing = 7.dp
    ) {
        items(6) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratios[index])
                    .clip(RoundedCornerShape(15.dp))
                    .background(palette.placeholder)
            )
        }
    }
}

@Composable
private fun EmptyGallery(palette: AppPalette, message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(message, style = AppType.body.copy(color = palette.muted, textAlign = TextAlign.Center))
    }
}

@Composable
private fun AccessScreen(
    modifier: Modifier = Modifier,
    palette: AppPalette,
    onGrant: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(26.dp)).background(palette.placeholder),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberVectorPainter(Icons.Outlined.FolderOpen),
                contentDescription = null,
                modifier = Modifier.size(31.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(palette.foreground)
            )
        }
        Spacer(Modifier.height(22.dp))
        BasicText("Allow access", style = AppType.display.copy(color = palette.foreground))
        Spacer(Modifier.height(7.dp))
        BasicText(
            "To find WhatsApp statuses on this device.",
            style = AppType.body.copy(color = palette.muted, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(24.dp))
        SpringActionButton(text = "Continue", palette = palette, onClick = onGrant)
    }
}

@Composable
private fun SpringActionButton(text: String, palette: AppPalette, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "actionPress"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(palette.accent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text, style = AppType.body.copy(color = palette.foreground, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun StatusActionsOverlay(
    backdrop: Backdrop,
    palette: AppPalette,
    status: Status,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRepost: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(status.path, status.timestamp) {
        ImageRequest.Builder(context)
            .data(status.file)
            .size(180)
            .memoryCacheKey("${status.path}:${status.timestamp}")
            .crossfade(false)
            .apply { if (status.isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
            .build()
    }

    var dragDistance by remember { mutableStateOf(0f) }
    val animatedDragDistance by animateFloatAsState(
        targetValue = dragDistance,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "actionSheetDrag"
    )

    Box(
        Modifier
            .fillMaxSize()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(0.dp) },
                effects = { blur(16.dp.toPx()) },
                highlight = { null },
                shadow = { null },
                onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.62f)) }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .graphicsLayer { translationY = animatedDragDistance }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(27.dp) },
                    effects = {
                        blur(24.dp.toPx())
                        lens(24.dp.toPx(), 32.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.88f) },
                    shadow = { null },
                    onDrawSurface = { drawRect(Color(0xE6101013)) }
                )
                .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(27.dp))
                .pointerInput(onDismiss) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            if (dragAmount.y > 0f || dragDistance > 0f) {
                                change.consume()
                                dragDistance = (dragDistance + dragAmount.y).coerceAtLeast(0f)
                            }
                        },
                        onDragEnd = {
                            if (dragDistance > 88f) onDismiss() else dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.36f))
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 12.dp, top = 13.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(request),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(9.dp))
                ActionGlassButton(
                    modifier = Modifier.weight(1f),
                    icon = if (isSaved) Icons.Outlined.DeleteOutline else Icons.Outlined.BookmarkBorder,
                    label = if (isSaved) "Delete" else "Save",
                    onClick = if (isSaved) onDelete else onSave
                )
                ActionGlassButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    onClick = onShare
                )
                ActionGlassButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Outlined.Send,
                    label = "WhatsApp",
                    onClick = onRepost
                )
            }
        }
    }
}

@Composable
private fun ActionGlassButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassActionPress"
    )
    Column(
        modifier
            .height(62.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(15.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.96f))
        )
        Spacer(Modifier.height(5.dp))
        BasicText(
            label,
            style = AppType.body.copy(
                color = Color.White.copy(alpha = 0.72f),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun openStatusFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mediaMimeType(file))
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No compatible app found to open this media", Toast.LENGTH_SHORT).show()
    }
}

private fun shareStatusFile(context: Context, file: File, share: Boolean) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mediaMimeType(file)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!share) setPackage("com.whatsapp")
        }
        if (share) {
            context.startActivity(Intent.createChooser(intent, "Share status"))
        } else {
            context.startActivity(intent)
        }
    } catch (_: Exception) {
        val message = if (share) "No app available to share this media" else "WhatsApp is not installed"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

private fun mediaMimeType(file: File): String {
    return when (file.extension.lowercase(Locale.ROOT)) {
        "mp4", "m4v", "3gp", "webm" -> "video/*"
        "gif" -> "image/gif"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
}
