package com.vinithreddybanda.whatsapstatus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.vinithreddybanda.whatsapstatus.model.Status
import com.vinithreddybanda.whatsapstatus.ui.theme.WhatsapStatusTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WhatsapStatusTheme {
                HomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(checkPermission(context)) }

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedStatus by remember { mutableStateOf<Status?>(null) }
    val scope = rememberCoroutineScope()

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = "package:${context.packageName}".toUri()
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            } else {
                legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkPermission(context)
                if (hasPermission) {
                    viewModel.getStatuses()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.getStatuses()
        }
    }

    // Pager State
    // Remember tabs list to avoid recreation
    val tabs = remember { listOf(StatusTab.All, StatusTab.Images, StatusTab.Videos, StatusTab.Saved) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    // Sync Pager with ViewModel Tab Selection
    LaunchedEffect(viewModel.selectedTab) {
        val index = tabs.indexOf(viewModel.selectedTab)
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    // Sync ViewModel Tab Selection with Pager
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
             val tab = tabs[page]
             if (viewModel.selectedTab != tab) {
                 viewModel.onTabSelected(tab)
             }
        }
    }

    // Blur effect
    val blurEffect = if (selectedStatus != null) Modifier.blur(16.dp) else Modifier

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = blurEffect,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.status_saver)) },
                    // Actions (Menu) removed as requested
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (hasPermission) {
                    // Tab Row (Chips)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(tabs) { tab ->
                            val isSelected = viewModel.selectedTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(tabs.indexOf(tab))
                                    }
                                },
                                label = {
                                    Text(
                                        text = when(tab) {
                                            StatusTab.All -> stringResource(R.string.tab_all)
                                            StatusTab.Images -> stringResource(R.string.tab_images)
                                            StatusTab.Videos -> stringResource(R.string.tab_videos)
                                            StatusTab.Saved -> stringResource(R.string.tab_saved)
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // Unselected BG
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant, // Unselected Label
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer, // Selected BG
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer // Selected Label
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                     borderColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // Horizontal Pager for Swipeable Tabs
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        key = { tabs[it].title },
                        flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
                    ) { page ->
                        val tab = tabs[page]
                        val statuses = viewModel.getStatusesForTab(tab)

                        if (statuses.isNotEmpty()) {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalItemSpacing = 16.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(statuses, key = { it.path }) { status ->
                                    StatusCard(
                                        status,
                                        viewModel,
                                        onCardClick = { openFile(context, status.file) },
                                        onMenuClick = { selectedStatus = status }
                                    )
                                }
                            }
                        } else if (!viewModel.isFetching) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                 Text(
                                     text = if (tab == StatusTab.Saved) stringResource(R.string.no_saved_statuses) else stringResource(R.string.no_statuses_available),
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                            }
                        }
                    }

                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(R.string.storage_permission_required))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                    intent.data = "package:${context.packageName}".toUri()
                                    context.startActivity(intent)
                                } else {
                                    legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }) {
                                Text(stringResource(R.string.grant_permission))
                            }
                        }
                    }
                }
            }
        }
    }

    // Pinterest-style Bottom Sheet with manual blur simulation
    if (selectedStatus != null) {
        // Overlay for click handling (transparent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { selectedStatus = null }
        )

        ModalBottomSheet(
            onDismissRequest = { selectedStatus = null },
            sheetState = sheetState,
            containerColor = Color.Transparent, // Transparent to allow custom floating layout
            scrimColor = Color.Transparent, // Transparent because we blur the background
            dragHandle = null
        ) {
            PinterestBottomSheetContent(
                status = selectedStatus!!,
                isSavedTab = viewModel.selectedTab == StatusTab.Saved,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                         if (!sheetState.isVisible) selectedStatus = null
                    }
                },
                onSave = {
                    viewModel.saveStatus(selectedStatus!!) { success ->
                         val message = if (success) context.getString(R.string.saved) else context.getString(R.string.failed_to_save)
                         Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                         scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    }
                },
                onDelete = {
                     viewModel.deleteStatus(selectedStatus!!) { success ->
                         val message = if (success) context.getString(R.string.deleted) else context.getString(R.string.failed_to_delete)
                         Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                         scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    }
                },
                onShare = {
                     shareOrRepost(context, selectedStatus!!.file, share = true)
                     scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                },
                onRepost = {
                     shareOrRepost(context, selectedStatus!!.file, share = false)
                     scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                }
            )
        }
    }
}

@Composable
fun PinterestBottomSheetContent(
    status: Status,
    isSavedTab: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRepost: () -> Unit
) {
    val context = LocalContext.current

    // Custom Layout for floating image
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // The Background Sheet
        Surface(
            modifier = Modifier
                .padding(top = 50.dp) // Push down to let image float on top
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer // Use theme surface color
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header Row (Close Button)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp)) // Space for the bottom half of the image

                // Actions
                if (isSavedTab) {
                    PinterestActionItem(
                        icon = Icons.Outlined.Delete,
                        text = stringResource(R.string.delete),
                        onClick = onDelete
                    )
                } else {
                    PinterestActionItem(
                        icon = Icons.Outlined.PushPin,
                        text = stringResource(R.string.save),
                        onClick = onSave
                    )
                }

                PinterestActionItem(
                    icon = Icons.Outlined.Share,
                    text = stringResource(R.string.share),
                    onClick = onShare
                )
                PinterestActionItem(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    text = stringResource(R.string.repost_to_whatsapp),
                    onClick = onRepost
                )
            }
        }

        // The Floating Image (Centered on top edge of Surface)
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(100.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(status.file)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .size(256)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun PinterestActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusCard(
    status: Status,
    viewModel: MainViewModel,
    onMenuClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val context = LocalContext.current

    // Randomized skeleton aspect ratio to mimic staggered grid content
    val skeletonRatio = remember(status.path) { Random.nextDouble(0.7, 1.2).toFloat() }

    // Use AsyncImagePainter to track loading state
    val builder = remember(status.path) {
        ImageRequest.Builder(context)
            .data(status.file)
            .crossfade(true)
            .size(512)
            .memoryCacheKey(status.path)
            .apply {
                if (status.isVideo) {
                    decoderFactory(VideoFrameDecoder.Factory())
                }
            }
            .build()
    }

    val painter = rememberAsyncImagePainter(model = builder)
    val isImageLoaded = painter.state is AsyncImagePainter.State.Success

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        // Image Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                // If not loaded, use random aspect ratio (skeleton).
                // If loaded, let content wrap height (FillWidth).
                .then(
                    if (!isImageLoaded) Modifier.aspectRatio(skeletonRatio)
                    else Modifier.wrapContentHeight()
                ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box {
                // Skeleton Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )

                if (status.isVideo) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.video),
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                    )
                }
            }
        }

        // Caption and Menu Row below the image
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timestamp
            Text(
                text = formatTimestamp(status.timestamp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // 3-dot menu icon
            IconButton(
                onClick = onMenuClick, // Trigger Bottom Sheet
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, if (file.extension == "mp4") "video/*" else "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.no_app_found_open), Toast.LENGTH_SHORT).show()
    }
}

fun shareOrRepost(context: Context, file: File, share: Boolean) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = if (file.extension == "mp4") "video/mp4" else "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (!share) {
            // Repost specifically to WhatsApp
            setPackage("com.whatsapp")
        }
    }

    try {
        context.startActivity(Intent.createChooser(intent, if (share) context.getString(R.string.share_via) else context.getString(R.string.repost_to_whatsapp)))
    } catch (e: Exception) {
        val message = if (!share) context.getString(R.string.whatsapp_not_installed) else context.getString(R.string.no_app_to_handle)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

fun checkPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WhatsapStatusTheme {
        // You can preview a single card here if you like
        // StatusCard( ... )
    }
}