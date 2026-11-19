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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredGridItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.vinithreddybanda.whatsapstatus.model.Status
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var showAccessSheet by remember { mutableStateOf(!hasPermission) }
    var selectedStatus by remember { mutableStateOf<Status?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val accessSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        if (hasPermission) {
            showAccessSheet = false
            viewModel.getStatuses()
        }
    }

    val tabs = remember { listOf(StatusTab.All, StatusTab.Images, StatusTab.Videos, StatusTab.Saved) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    LaunchedEffect(viewModel.selectedTab) {
        val index = tabs.indexOf(viewModel.selectedTab)
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onTabSelected(tabs[page])
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "STATUS / ARCHIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.8.sp
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "Your stories",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1.1).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (hasPermission) viewModel.statusList.size.toString() + " moments on this device" else "Your private media space",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            modifier = Modifier.size(46.dp).clickable(enabled = hasPermission) {
                                viewModel.getStatuses(forceRefresh = true)
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Refresh statuses",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) {
                                if (searchQuery.isBlank()) {
                                    Text(
                                        "Search your stories",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->

        ) { innerPadding ->
            if (!hasPermission) {
                AccessRequiredContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onGrant = { requestAccess() }
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    beyondViewportPageCount = 0,
                    key = { tabs[it].title }
                ) { page ->
                    val tab = tabs[page]
                    val tabStatuses = viewModel.getVisibleStatusesForTab(tab)
                    val statuses = remember(tabStatuses, searchQuery) {
                        if (searchQuery.isBlank()) tabStatuses else tabStatuses.filter {
                            it.title.contains(searchQuery.trim(), ignoreCase = true)
                        }
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
                                columns = StaggeredGridCells.Fixed(2),
                                contentPadding = PaddingValues(
                                    start = 14.dp,
                                    end = 14.dp,
                                    top = 8.dp,
                                    bottom = WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding() + 104.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalItemSpacing = 15.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                staggeredGridItems(
                                    items = statuses,
                                    key = { it.path },
                                    contentType = { "status-card" }
                                ) { status ->
                                    StatusCard(
                                        status = status,
                                        onOpen = { openStatusFile(context, status.file) },
                                        onOptions = { selectedStatus = status }
                                    )
                                }
                                if (viewModel.isFetching) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        (tab != StatusTab.Saved && viewModel.isFetching) || (tab == StatusTab.Saved && viewModel.isSavedFetching) -> {
                            LoadingGallery()
                        }
                        else -> {
                            EmptyGallery(
                                title = if (tab == StatusTab.Saved) "Nothing saved yet" else "No statuses found",
                                subtitle = if (tab == StatusTab.Saved) {
                                    "Save a story and it will appear here."
                                } else {
                                    "New WhatsApp stories will appear here automatically."
                                }
                            )
                        }
                    }
                }
            }
        }

        if (hasPermission) {
            LiquidTabBar(
                selectedIndex = pagerState.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter),
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
            )
        }

        if (showAccessSheet && !hasPermission) {
            ModalBottomSheet(
                onDismissRequest = { showAccessSheet = false },
                sheetState = accessSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.32f),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
            ) {
                PermissionSheetContent(
                    onContinue = {
                        scope.launch {
                            accessSheetState.hide()
                            showAccessSheet = false
                            requestAccess()
                        }
                    },
                    onNotNow = {
                        scope.launch {
                            accessSheetState.hide()
                            showAccessSheet = false
                        }
                    }
                )
            }
        }

        selectedStatus?.let { status ->
            ModalBottomSheet(
                onDismissRequest = { selectedStatus = null },
                sheetState = actionSheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.42f),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                dragHandle = {
                    Box(
                        Modifier
                            .padding(top = 12.dp)
                            .size(width = 38.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                }
            ) {
                HideModalStatusBar()
                StatusActionsSheet(
                    status = status,
                    isSaved = viewModel.selectedTab == StatusTab.Saved,
                    onClose = {
                        scope.launch {
                            actionSheetState.hide()
                            selectedStatus = null
                        }
                    },
                    onSave = {
                        viewModel.saveStatus(status) { success ->
                            Toast.makeText(
                                context,
                                if (success) "Saved to StatusSaver" else "Couldn't save this status",
                                Toast.LENGTH_SHORT
                            ).show()
                            scope.launch {
                                actionSheetState.hide()
                                selectedStatus = null
                            }
                        }
                    },
                    onDelete = {
                        viewModel.deleteStatus(status) { success ->
                            Toast.makeText(
                                context,
                                if (success) "Removed from saved" else "Couldn't delete this status",
                                Toast.LENGTH_SHORT
                            ).show()
                            scope.launch {
                                actionSheetState.hide()
                                selectedStatus = null
                            }
                        }
                    },
                    onShare = {
                        scope.launch {
                            actionSheetState.hide()
                            selectedStatus = null
                            shareStatusFile(context, status.file, share = true)
                        }
                    },
                    onRepost = {
                        scope.launch {
                            actionSheetState.hide()
                            selectedStatus = null
                            shareStatusFile(context, status.file, share = false)
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun LiquidTabBar(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val tabs = listOf(
        Triple(Icons.Outlined.FolderOpen, "All", 0),
        Triple(Icons.Outlined.Photo, "Photos", 1),
        Triple(Icons.Outlined.PlayCircleOutline, "Videos", 2),
        Triple(Icons.Outlined.BookmarkBorder, "Saved", 3)
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .navigationBarsPadding()
            .padding(bottom = 10.dp)
            .height(68.dp)
            .shadow(14.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (icon, label, page) ->
                val selected = selectedIndex == page
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        )
                        .clickable { onSelect(page) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}



@Composable
private fun StatusCard(
    status: Status,
    onOpen: () -> Unit,
    onOptions: () -> Unit
) {
    val context = LocalContext.current
    val cacheKey = remember(status.path, status.timestamp) {
        status.path + ":" + status.timestamp
    }
    val request = remember(cacheKey, status.isVideo) {
        ImageRequest.Builder(context)
            .data(status.file)
            .size(480)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .apply {
                if (status.isVideo) decoderFactory(VideoFrameDecoder.Factory())
            }
            .build()
    }
    val ratio = (status.aspectRatio ?: if (status.isVideo) 0.5625f else 0.82f)
        .coerceIn(0.48f, 1.65f)

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onOpen)
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = request),
                contentDescription = if (status.isVideo) "Video status" else "Image status",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (status.isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.48f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(9.dp).size(34.dp)
                    .clickable(onClick = onOptions),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.48f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.MoreHoriz,
                        contentDescription = "Status options",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (status.isVideo) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(9.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.58f)
                ) {
                    Text(
                        "VIDEO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
        Text(
            text = formatStatusTime(status.timestamp),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}



@Composable
private fun LoadingGallery() {
    val ratios = listOf(0.78f, 1.12f, 0.92f, 1.28f, 0.84f, 1.02f)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 14.dp
    ) {
        items(6) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratios[index])
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}



@Composable
private fun EmptyGallery(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(76.dp).clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(7.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}



@Composable
private fun AccessRequiredContent(modifier: Modifier = Modifier, onGrant: () -> Unit) {
    Column(
        modifier = modifier.padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(82.dp).clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.height(22.dp))
        Text("Your gallery is waiting", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Give Status Saver access to your WhatsApp status folder. Your media stays on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 15.dp)
        ) {
            Text("Grant access")
        }
    }
}



@Composable
private fun PermissionSheetContent(onContinue: () -> Unit, onNotNow: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(19.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("A little access, a lot less effort", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text(
            "Status Saver reads the WhatsApp status folder to show your recent photos and videos. Nothing is uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 15.dp)
        ) {
            Text("Continue")
        }
        Text(
            "Maybe later",
            modifier = Modifier.fillMaxWidth().clickable(onClick = onNotNow).padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}



@Composable
private fun StatusActionsSheet(
    status: Status,
    isSaved: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRepost: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(status.path, status.timestamp) {
        ImageRequest.Builder(context)
            .data(status.file)
            .size(220)
            .memoryCacheKey("${status.path}:${status.timestamp}")
            .crossfade(false)
            .apply { if (status.isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
            .build()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Card(
                modifier = Modifier.size(66.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(request),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = status.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = formatStatusTime(status.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            "ACTIONS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                icon = if (isSaved) Icons.Outlined.DeleteOutline else Icons.Outlined.BookmarkBorder,
                label = if (isSaved) "Delete" else "Save",
                modifier = Modifier.weight(1f),
                onClick = if (isSaved) onDelete else onSave
            )
            ActionTile(
                icon = Icons.Outlined.Share,
                label = "Share",
                modifier = Modifier.weight(1f),
                onClick = onShare
            )
            ActionTile(
                icon = Icons.AutoMirrored.Outlined.Send,
                label = "WhatsApp",
                modifier = Modifier.weight(1f),
                onClick = onRepost
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(94.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(23.dp)
            )
            Spacer(Modifier.height(9.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
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
        val mimeType = mediaMimeType(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
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

private fun formatStatusTime(timestamp: Long): String {
    return SimpleDateFormat("dd MMM · hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
