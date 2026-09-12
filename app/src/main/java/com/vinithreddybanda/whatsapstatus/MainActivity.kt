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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.vinithreddybanda.whatsapstatus.model.Status
import com.vinithreddybanda.whatsapstatus.ui.theme.WhatsapStatusTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private val dateFormatThreadLocal = ThreadLocal.withInitial {
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enterImmersiveFullscreen()

        setContent {
            WhatsapStatusTheme {
                HomeScreen()
            }
        }
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
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
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
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        data = "package:${context.packageName}".toUri()
                    })
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    LaunchedEffect(viewModel.selectedTab) {
        tabs.indexOf(viewModel.selectedTab).takeIf { it >= 0 }?.let { index ->
            if (pagerState.currentPage != index) pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onTabSelected(tabs[page])
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LiquidBackdrop(motion)

        Column(
            modifier = Modifier
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
                    key = { tabs[it].title },
                    flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
                ) { page ->
                    val tab = tabs[page]
                    val statuses = viewModel.getStatusesForTab(tab)
                    if (statuses.isNotEmpty()) {
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
                                    viewModel = viewModel,
                                    motion = motion,
                                    onCardClick = { openFile(context, status.file) },
                                    onMenuClick = { selectedStatus = status }
                                )
                            }
                        }
                    } else if (!viewModel.isFetching) {
                        EmptyGlassState(
                            text = if (tab == StatusTab.Saved) stringResource(R.string.no_saved_statuses)
                            else stringResource(R.string.no_statuses_available)
                        )
                    }
                }
            } else {
                PermissionGlassState {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = "package:${context.packageName}".toUri()
                        })
                    } else {
                        legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.10f))
                    .blur(18.dp)
            )
        }

        if (selectedStatus != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedStatus = null },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                scrimColor = Color.Transparent,
                dragHandle = null
            ) {
                LiquidActionSheet(
                    status = selectedStatus!!,
                    isSavedTab = viewModel.selectedTab == StatusTab.Saved,
                    onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    },
                    onSave = {
                        viewModel.saveStatus(selectedStatus!!) { success ->
                            Toast.makeText(
                                context,
                                context.getString(if (success) R.string.saved else R.string.failed_to_save),
                                Toast.LENGTH_SHORT
                            ).show()
                            scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                        }
                    },
                    onDelete = {
                        viewModel.deleteStatus(selectedStatus!!) { success ->
                            Toast.makeText(
                                context,
                                context.getString(if (success) R.string.deleted else R.string.failed_to_delete),
                                Toast.LENGTH_SHORT
                            ).show()
                            scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                        }
                    },
                    onShare = {
                        shareOrRepost(context, selectedStatus!!.file, true)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    },
                    onRepost = {
                        shareOrRepost(context, selectedStatus!!.file, false)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    }
                )
            }
        }
    }
}

@Composable
private private fun LiquidBackdrop(motion: MotionVector) {
    val transition = rememberInfiniteTransition(label = "liquid_background")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing)),
        label = "phase"
    )
    val offsetX = (motion.x * 2.4f + phase * 30f)
    val offsetY = (motion.y * 1.8f + (1f - phase) * 22f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer { translationX = offsetX.dp.toPx(); translationY = offsetY.dp.toPx() }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF4B8DFF).copy(alpha = 0.20f), Color.Transparent),
                        radius = 480f
                    ),
                    CircleShape
                )
                .blur(42.dp)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    translationX = (-offsetX * 0.7f).dp.toPx()
                    translationY = (-offsetY * 0.8f).dp.toPx()
                }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF4FA3).copy(alpha = 0.15f), Color.Transparent),
                        radius = 420f
                    ),
                    CircleShape
                )
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.CenterEnd)
                .graphicsLayer { rotationZ = motion.x * 0.16f }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF6C63FF).copy(alpha = 0.11f), Color.Transparent),
                        radius = 360f
                    ),
                    CircleShape
                )
                .blur(48.dp)
        )
    }
}

@Composable
private fun HeaderGlass(motion: MotionVector) {
    val titleLift by animateDpAsState((motion.y * -0.18f).dp, spring(stiffness = Spring.StiffnessLow), label = "title_lift")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .graphicsLayer { translationY = titleLift.toPx() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF7C4DFF), Color(0xFFFF4DB8), Color(0xFF4DD8FF), Color(0xFF7C4DFF)),
                        tileMode = TileMode.Mirror
                    )
                )
                .padding(1.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Text("W", fontWeight = FontWeight.Black, fontSize = 15.sp)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.075f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { tab ->
            val active = selected == tab
            val scale by animateFloatAsState(if (active) 1f else 0.97f, spring(stiffness = Spring.StiffnessMediumLow), label = "tab_scale")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .liquidClickable { onSelected(tab) }
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        if (active) Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color(0xFF8C77FF).copy(alpha = 0.13f),
                                Color(0xFFFF66B3).copy(alpha = 0.11f)
                            )
                        ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .border(
                        1.dp,
                        if (active) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                        RoundedCornerShape(17.dp)
                    )
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (tab) {
                        StatusTab.All -> stringResource(R.string.tab_all)
                        StatusTab.Images -> stringResource(R.string.tab_images)
                        StatusTab.Videos -> stringResource(R.string.tab_videos)
                        StatusTab.Saved -> stringResource(R.string.tab_saved)
                    },
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (active) 1f else 0.60f)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    status: Status,
    viewModel: MainViewModel,
    motion: MotionVector,
    onMenuClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val context = LocalContext.current
    val skeletonRatio = remember(status.path) { Random.nextDouble(0.7, 1.2).toFloat() }
    val builder = remember(status.path) {
        ImageRequest.Builder(context)
            .data(status.file)
            .crossfade(360)
            .size(512)
            .memoryCacheKey(status.path)
            .apply { if (status.isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
            .build()
    }
    val painter = rememberAsyncImagePainter(builder)
    val loaded = painter.state is AsyncImagePainter.State.Success
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow), label = "card_scale")
    val tilt by animateFloatAsState(if (pressed) 1.2f else 0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow), label = "card_tilt")
    val parallax = (motion.x * 0.12f + motion.y * 0.08f).coerceIn(-2f, 2f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = tilt
                translationX = parallax.dp.toPx()
            }
            .liquidClickable(interactions, onCardClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!loaded) Modifier.aspectRatio(skeletonRatio) else Modifier)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.065f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
        ) {
            if (!loaded) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.025f)))))
            }
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            if (status.isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, stringResource(R.string.video), tint = Color.White, modifier = Modifier.size(27.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp, start = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimestamp(status.timestamp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            IconButton(onClick = onMenuClick, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.MoreHoriz, stringResource(R.string.more_options), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f), modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun EmptyGlassState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassSurface(modifier = Modifier.padding(30.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
                Text("EMPTY", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(text, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
            }
        }
    }
}

@Composable
private fun PermissionGlassState(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassSurface(modifier = Modifier.padding(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.storage_permission_required), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Storage access unlocks your status vault.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                LiquidButton(text = stringResource(R.string.grant_permission), onClick = onGrant)
            }
        }
    }
}

@Composable
private fun GlassSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.055f), Color(0xFF8F7DFF).copy(alpha = 0.055f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
    ) { content() }
}

@Composable
private fun LiquidButton(text: String, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow), label = "button_scale")
    val phase by rememberInfiniteTransition(label = "button_phase").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing)),
        label = "button_phase_value"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF7B5CFF),
                        Color(0xFFEA68B8),
                        Color(0xFF4CCBFF),
                        Color(0xFF7B5CFF)
                    ),
                    startX = -900f + phase * 1800f,
                    endX = 500f + phase * 1800f,
                    tileMode = TileMode.Mirror
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
            .liquidClickable(interactions, onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 13.sp)
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
    val context = LocalContext.current
    GlassSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp, 14.dp, 16.dp, 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ACTIONS", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (status.isVideo) "VIDEO STATUS" else "PHOTO STATUS", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.075f))) {
                    Icon(Icons.Default.Close, stringResource(R.string.close))
                }
            }
            Spacer(Modifier.height(8.dp))
            ActionRow(Icons.Outlined.Delete, stringResource(R.string.delete), onDelete, visible = isSavedTab)
            ActionRow(Icons.Outlined.PushPin, stringResource(R.string.save), onSave, visible = !isSavedTab)
            ActionRow(Icons.Outlined.Share, stringResource(R.string.share), onShare)
            ActionRow(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.repost_to_whatsapp), onRepost)
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, onClick: () -> Unit, visible: Boolean = true) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.96f, animationSpec = spring()),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.96f)
    ) {
        val interactions = remember { MutableInteractionSource() }
        val pressed by interactions.collectIsPressedAsState()
        val y by animateIntAsState(if (pressed) 1 else 0, spring(), label = "row_y")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = y * 1.5f }
                .clip(RoundedCornerShape(18.dp))
                .background(if (pressed) Color.White.copy(alpha = 0.105f) else Color.Transparent)
                .liquidClickable(interactions, onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(13.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Modifier.liquidClickable(
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    onClick: () -> Unit
): Modifier = composed {
    clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

fun formatTimestamp(timestamp: Long): String {
    return dateFormatThreadLocal.get()?.format(Date(timestamp)) ?: ""
}

fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, if (file.extension == "mp4") "video/*" else "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.no_app_found_open), Toast.LENGTH_SHORT).show()
    }
}

fun shareOrRepost(context: Context, file: File, share: Boolean) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (file.extension == "mp4") "video/mp4" else "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (!share) setPackage("com.whatsapp")
    }
    try {
        context.startActivity(Intent.createChooser(intent, if (share) context.getString(R.string.share_via) else context.getString(R.string.repost_to_whatsapp)))
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(if (!share) R.string.whatsapp_not_installed else R.string.no_app_to_handle),
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun checkPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
