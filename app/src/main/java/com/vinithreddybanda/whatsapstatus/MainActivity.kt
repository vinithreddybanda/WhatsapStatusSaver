package com.vinithreddybanda.whatsapstatus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.vinithreddybanda.whatsapstatus.model.Status
import com.vinithreddybanda.whatsapstatus.ui.theme.WhatsapStatusTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatThreadLocal = ThreadLocal.withInitial {
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enterImmersiveFullscreen()
        setContent { WhatsapStatusTheme { HomeScreen() } }
    }

    private fun enterImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
    }
}

@Composable
private fun rememberMotionVector(): MotionVector {
    val context = LocalContext.current
    var motion by remember { mutableStateOf(MotionVector()) }
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    DisposableEffect(sensor) {
        if (sensor == null) return@DisposableEffect onDispose { }
        var smoothedX = 0f
        var smoothedY = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: 0f
                val y = event.values.getOrNull(1) ?: 0f
                smoothedX = smoothedX * 0.84f + x.coerceIn(-12f, 12f) * 0.16f
                smoothedY = smoothedY * 0.84f + y.coerceIn(-12f, 12f) * 0.16f
                motion = MotionVector(smoothedX, smoothedY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    return motion
}

private data class MotionVector(val x: Float = 0f, val y: Float = 0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val motion = rememberMotionVector()
    var hasPermission by remember { mutableStateOf(checkPermission(context)) }
    var selectedStatus by remember { mutableStateOf<Status?>(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(Unit) {
        if (!hasPermission) openStorageSettings(context, legacyPermissionLauncher)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkPermission(context)
                if (hasPermission) viewModel.getStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.getStatuses()
    }

    val tabs = listOf(StatusTab.All, StatusTab.Images, StatusTab.Videos, StatusTab.Saved)
    val pagerState = rememberPagerState(initialPage = tabs.indexOf(viewModel.selectedTab), pageCount = { tabs.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page in tabs.indices) viewModel.onTabSelected(tabs[page])
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LiquidBackdrop(motion)
        Column(
            Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            HeaderGlass(motion)

            if (hasPermission) {
                GlassTabBar(
                    tabs = tabs,
                    selected = viewModel.selectedTab,
                    onSelected = { tab ->
                        scope.launch { pagerState.animateScrollToPage(tabs.indexOf(tab)) }
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1,
                    key = { tabs[it].title }
                ) { page ->
                    val tab = tabs[page]
                    val statuses = viewModel.getStatusesForTab(tab)
                    when {
                        statuses.isNotEmpty() -> {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                contentPadding = PaddingValues(14.dp, 8.dp, 14.dp, 26.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalItemSpacing = 12.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(statuses, key = { it.path }) { status ->
                                    StatusCard(
                                        status = status,
                                        motion = motion,
                                        onCardClick = { openFile(context, status.file) },
                                        onMenuClick = { selectedStatus = status }
                                    )
                                }
                            }
                        }
                        !viewModel.isFetching -> {
                            EmptyGlassState(
                                text = if (tab == StatusTab.Saved) {
                                    stringResource(R.string.no_saved_statuses)
                                } else {
                                    stringResource(R.string.no_statuses_available)
                                }
                            )
                        }
                    }
                }
            } else {
                PermissionGlassState {
                    openStorageSettings(context, legacyPermissionLauncher)
                }
            }
        }

        AnimatedVisibility(
            visible = selectedStatus != null,
            enter = fadeIn(tween(280)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.10f))
                    .blur(18.dp)
            )
        }

        selectedStatus?.let { status ->
            ModalBottomSheet(
                onDismissRequest = { selectedStatus = null },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                scrimColor = Color.Transparent,
                dragHandle = null
            ) {
                LiquidActionSheet(
                    status = status,
                    isSavedTab = viewModel.selectedTab == StatusTab.Saved,
                    onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    },
                    onSave = {
                        viewModel.saveStatus(status) { success ->
                            Toast.makeText(context, context.getString(if (success) R.string.saved else R.string.failed_to_save), Toast.LENGTH_SHORT).show()
                            scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                        }
                    },
                    onDelete = {
                        viewModel.deleteStatus(status) { success ->
                            Toast.makeText(context, context.getString(if (success) R.string.deleted else R.string.failed_to_delete), Toast.LENGTH_SHORT).show()
                            scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                        }
                    },
                    onShare = {
                        shareOrRepost(context, status.file, share = true)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    },
                    onRepost = {
                        shareOrRepost(context, status.file, share = false)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    }
                )
            }
        }
    }
}

@Composable
private fun LiquidBackdrop(motion: MotionVector) {
    val transition = rememberInfiniteTransition(label = "liquid_background")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing)),
        label = "phase"
    )
    val offsetX = motion.x * 2.4f + phase * 30f
    val offsetY = motion.y * 1.8f + (1f - phase) * 22f

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(280.dp)
                .graphicsLayer {
                    translationX = offsetX * density
                    translationY = offsetY * density
                }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF4B8DFF).copy(alpha = 0.20f), Color.Transparent),
                        radius = 480f
                    ), CircleShape
                )
                .blur(42.dp)
        )
        Box(
            Modifier
                .size(240.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    translationX = (-offsetX * 0.7f) * density
                    translationY = (-offsetY * 0.8f) * density
                }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF4FA3).copy(alpha = 0.15f), Color.Transparent),
                        radius = 420f
                    ), CircleShape
                )
                .blur(50.dp)
        )
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.CenterEnd)
                .graphicsLayer { rotationZ = motion.x * 0.16f }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF6C63FF).copy(alpha = 0.11f), Color.Transparent),
                        radius = 360f
                    ), CircleShape
                )
                .blur(48.dp)
        )
    }
}

@Composable
private fun HeaderGlass(motion: MotionVector) {
    val titleLift by animateDpAsState(
        targetValue = (motion.y * -0.18f).dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "title_lift"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .graphicsLayer { translationY = titleLift.toPx() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.status_saver),
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.7).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "YOUR STATUS VAULT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f)
            )
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.MoreHoriz, contentDescription = null)
        }
    }
}

@Composable
private fun GlassTabBar(
    tabs: List<StatusTab>,
    selected: StatusTab,
    onSelected: (StatusTab) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tabs, key = { it.title }) { tab ->
            val isSelected = tab == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.96f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "tab_scale"
            )
            Text(
                text = tab.title,
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                        else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSelected) 0.12f else 0.05f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelected(tab) }
                    )
                    .padding(horizontal = 15.dp, vertical = 9.dp),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSelected) 0.95f else 0.60f)
            )
        }
    }
}

@Composable
private fun StatusCard(
    status: Status,
    motion: MotionVector,
    onCardClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val pressedSource = remember { MutableInteractionSource() }
    val pressed by pressedSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_press"
    )
    val dateText = remember(status.timestamp) {
        dateFormatThreadLocal.get()?.format(Date(status.timestamp)).orEmpty()
    }

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = motion.x * 0.04f
            }
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .clickable(pressedSource, indication = null, onClick = onCardClick)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(status.file)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = status.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (status.isVideo) 0.82f else 0.78f)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                contentScale = ContentScale.Crop
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = status.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateText,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                    )
                }
                if (status.isVideo) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMenuClick, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyGlassState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(horizontal = 22.dp, vertical = 14.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
        )
    }
}

@Composable
private fun PermissionGlassState(onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.storage_permission_description),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.grant_storage_access),
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f))
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 11.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LiquidActionSheet(
    status: Status,
    isSavedTab: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRepost: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(status.title, fontWeight = FontWeight.Bold)
                Text(
                    text = if (status.isVideo) "VIDEO" else "IMAGE",
                    fontSize = 9.sp,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
                )
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null) }
        }

        ActionRow(Icons.Outlined.Share, "Share", onShare)
        ActionRow(Icons.AutoMirrored.Outlined.Send, "Repost", onRepost)
        if (!isSavedTab) ActionRow(Icons.Outlined.Share, "Save", onSave)
        if (isSavedTab) ActionRow(Icons.Outlined.Delete, "Delete", onDelete)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(12.dp))
        Text(title, fontWeight = FontWeight.Medium)
    }
}

private fun checkPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun openStorageSettings(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            })
        }.onFailure {
            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    } else {
        launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun openFile(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (file.extension.equals("mp4", true)) "video/*" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, R.string.failed_to_open_file, Toast.LENGTH_SHORT).show()
    }
}

private fun shareOrRepost(context: Context, file: File, share: Boolean) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(if (share) Intent.ACTION_SEND else Intent.ACTION_SEND).apply {
            type = if (file.extension.equals("mp4", true)) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }.onFailure {
        Toast.makeText(context, R.string.failed_to_share, Toast.LENGTH_SHORT).show()
    }
}

@Suppress("unused")
private fun Modifier.liquidGlass() = composed { this }

@Suppress("unused")
private fun Color.withTile(tileMode: TileMode) = this
