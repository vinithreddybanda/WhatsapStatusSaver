package com.vinithreddybanda.whatsapstatus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Toast
import android.widget.VideoView
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
import androidx.compose.animation.core.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val dateFormatThreadLocal = ThreadLocal.withInitial {
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
}

data class MediaColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enterImmersiveFullscreen()
        setContent { WhatsapStatusTheme { HomeScreen() } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveFullscreen()
    }

    private fun enterImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
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
        var smoothX = 0f
        var smoothY = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: 0f
                val y = event.values.getOrNull(1) ?: 0f
                smoothX = smoothX * 0.86f + x.coerceIn(-12f, 12f) * 0.14f
                smoothY = smoothY * 0.86f + y.coerceIn(-12f, 12f) * 0.14f
                motion = MotionVector(smoothX, smoothY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return motion
}

private data class MotionVector(val x: Float = 0f, val y: Float = 0f)

@Composable
private fun rememberMediaColors(file: File?): MediaColors? {
    var colors by remember(file?.absolutePath) { mutableStateOf<MediaColors?>(null) }

    LaunchedEffect(file?.absolutePath) {
        colors = file?.let { sampleMediaColors(it) }
    }

    return colors
}

private suspend fun sampleMediaColors(file: File): MediaColors? = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = if (file.extension.equals("mp4", true) || file.extension.equals("mkv", true)) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } finally {
                retriever.release()
            }
        } else {
            BitmapFactory.decodeFile(file.absolutePath)
        } ?: return@withContext null

        val width = bitmap.width.coerceAtLeast(1)
        val height = bitmap.height.coerceAtLeast(1)
        val sampleW = minOf(width, 180)
        val sampleH = maxOf(1, (height.toFloat() * sampleW / width).roundToInt())
        val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        if (scaled !== bitmap) bitmap.recycle()

        fun sampleRegion(startX: Int, endX: Int, startY: Int, endY: Int): Color {
            var r = 0L
            var g = 0L
            var b = 0L
            var count = 0L
            val stepX = maxOf(1, scaled.width / 18)
            val stepY = maxOf(1, scaled.height / 18)
            var y = startY.coerceAtLeast(0)
            while (y < endY.coerceAtMost(scaled.height)) {
                var x = startX.coerceAtLeast(0)
                while (x < endX.coerceAtMost(scaled.width)) {
                    val pixel = scaled.getPixel(x, y)
                    r += android.graphics.Color.red(pixel)
                    g += android.graphics.Color.green(pixel)
                    b += android.graphics.Color.blue(pixel)
                    count++
                    x += stepX
                }
                y += stepY
            }
            if (count == 0L) return Color.DarkGray
            return Color(
                red = (r / count).toInt(),
                green = (g / count).toInt(),
                blue = (b / count).toInt()
            )
        }

        val primary = sampleRegion(0, scaled.width, 0, scaled.height)
        val secondary = sampleRegion(0, scaled.width / 2, 0, scaled.height)
        val tertiary = sampleRegion(scaled.width / 2, scaled.width, 0, scaled.height)
        scaled.recycle()

        MediaColors(
            primary = soften(primary),
            secondary = soften(secondary),
            tertiary = soften(tertiary)
        )
    }.getOrNull()
}

private fun soften(color: Color): Color {
    val luminance = color.red * 0.2126f + color.green * 0.7152f + color.blue * 0.0722f
    val lift = if (luminance < 0.24f) 0.16f else 0f
    return Color(
        red = (color.red + lift).coerceIn(0f, 1f),
        green = (color.green + lift).coerceIn(0f, 1f),
        blue = (color.blue + lift).coerceIn(0f, 1f),
        alpha = 1f
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val motion = rememberMotionVector()
    var hasPermission by remember { mutableStateOf(checkPermission(context)) }
    var selectedStatus by remember { mutableStateOf<Status?>(null) }
    var viewerStatus by remember { mutableStateOf<Status?>(null) }
    var ambientColors by remember { mutableStateOf<MediaColors?>(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) openStorageSettings(context, launcher)
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
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(viewModel.selectedTab).coerceAtLeast(0),
        pageCount = { tabs.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page in tabs.indices) viewModel.onTabSelected(tabs[page])
        }
    }

    val pageAmbientPrimary by animateColorAsState(
        targetValue = ambientColors?.primary ?: MaterialTheme.colorScheme.background,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "ambient_primary"
    )
    val pageAmbientSecondary by animateColorAsState(
        targetValue = ambientColors?.secondary ?: MaterialTheme.colorScheme.background,
        animationSpec = tween(820, easing = FastOutSlowInEasing),
        label = "ambient_secondary"
    )
    val pageAmbientTertiary by animateColorAsState(
        targetValue = ambientColors?.tertiary ?: MaterialTheme.colorScheme.background,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ambient_tertiary"
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        FluxBackdrop(
            primary = pageAmbientPrimary,
            secondary = pageAmbientSecondary,
            tertiary = pageAmbientTertiary,
            motion = motion
        )

        var chromeCollapse by remember { mutableStateOf(0f) }
        val animatedCollapse by animateFloatAsState(
            targetValue = chromeCollapse,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 250f),
            label = "chrome_collapse"
        )
        val chromeHeight by animateDpAsState(
            targetValue = 122.dp * (1f - animatedCollapse),
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 310f),
            label = "chrome_height"
        )

        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(chromeHeight)
                    .graphicsLayer {
                        alpha = (1f - animatedCollapse).coerceIn(0f, 1f)
                        translationY = -26f * animatedCollapse
                    }
            ) {
                HeaderGlass(motion)
                GlassTabBar(
                    tabs = tabs,
                    selected = viewModel.selectedTab,
                    onSelected = { tab ->
                        scope.launch { pagerState.animateScrollToPage(tabs.indexOf(tab)) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
                key = { tabs[it].title }
            ) { page ->
                val active = pagerState.currentPage == page
                StatusPage(
                    statuses = viewModel.getStatusesForTab(tabs[page]),
                    isFetching = viewModel.isFetching,
                    active = active,
                    onCollapseChanged = { value -> if (active) chromeCollapse = value },
                    onAmbientColors = { value -> if (active) ambientColors = value },
                    motion = motion,
                    onCardClick = { viewerStatus = it },
                    onMenuClick = { selectedStatus = it }
                )
            }
        }

        FrostEdges(
            primary = pageAmbientPrimary,
            secondary = pageAmbientSecondary,
            collapse = animatedCollapse
        )

        AnimatedVisibility(
            visible = selectedStatus != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(pageAmbientPrimary.copy(alpha = 0.20f), Color.Transparent),
                            radius = 900f
                        )
                    )
                    .blur(22.dp)
            )
        }

        selectedStatus?.let { status ->
            MediaActionSheet(
                status = status,
                isSavedTab = viewModel.selectedTab == StatusTab.Saved,
                colors = rememberMediaColors(status.file),
                sheetState = sheetState,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                },
                onSave = {
                    viewModel.saveStatus(status) { success ->
                        Toast.makeText(
                            context,
                            context.getString(if (success) R.string.saved else R.string.failed_to_save),
                            Toast.LENGTH_SHORT
                        ).show()
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null }
                    }
                },
                onDelete = {
                    viewModel.deleteStatus(status) { success ->
                        Toast.makeText(
                            context,
                            context.getString(if (success) R.string.deleted else R.string.failed_to_delete),
                            Toast.LENGTH_SHORT
                        ).show()
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

        viewerStatus?.let { status ->
            MediaViewer(
                status = status,
                colors = rememberMediaColors(status.file),
                onClose = { viewerStatus = null },
                onOpenWith = { openFile(context, status.file) }
            )
        }
    }
}

@Composable
private fun StatusPage(
    statuses: List<Status>,
    isFetching: Boolean,
    active: Boolean,
    onCollapseChanged: (Float) -> Unit,
    onAmbientColors: (MediaColors) -> Unit,
    motion: MotionVector,
    onCardClick: (Status) -> Unit,
    onMenuClick: (Status) -> Unit
) {
    val gridState = rememberLazyStaggeredGridState()
    val ambientStatus = statuses.getOrNull(gridState.firstVisibleItemIndex)
    val ambientColors = rememberMediaColors(ambientStatus?.file)

    LaunchedEffect(ambientStatus?.path, ambientColors, active) {
        if (active) ambientColors?.let(onAmbientColors)
    }

    LaunchedEffect(gridState, active) {
        if (!active) return@LaunchedEffect
        snapshotFlow {
            if (gridState.firstVisibleItemIndex > 0) 1f
            else (gridState.firstVisibleItemScrollOffset / 180f).coerceIn(0f, 1f)
        }.collect(onCollapseChanged)
    }

    Box(Modifier.fillMaxSize()) {
        when {
            statuses.isNotEmpty() -> {
                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        top = 12.dp,
                        end = 14.dp,
                        bottom = 26.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(statuses, key = { it.path }) { status ->
                        StatusCard(
                            status = status,
                            motion = motion,
                            onCardClick = { onCardClick(status) },
                            onMenuClick = { onMenuClick(status) }
                        )
                    }
                }
            }
            !isFetching -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(
                            if (statuses === emptyList<Status>() && false) R.string.no_statuses_available
                            else R.string.no_statuses_available
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.64f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FluxBackdrop(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    motion: MotionVector
) {
    val transition = rememberInfiniteTransition(label = "flux")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14500, easing = FastOutSlowInEasing)),
        label = "phase"
    )

    val driftX = motion.x * 2.4f + phase * 24f
    val driftY = motion.y * 1.8f + (1f - phase) * 18f

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(340.dp)
                .graphicsLayer {
                    translationX = driftX * density
                    translationY = driftY * density
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.34f), primary.copy(alpha = 0f)),
                        radius = 560f
                    ),
                    CircleShape
                )
                .blur(58.dp)
        )
        Box(
            Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    translationX = -driftX * 0.8f * density
                    translationY = -driftY * 0.8f * density
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = 0.27f), secondary.copy(alpha = 0f)),
                        radius = 500f
                    ),
                    CircleShape
                )
                .blur(62.dp)
        )
        Box(
            Modifier
                .size(250.dp)
                .align(Alignment.CenterEnd)
                .graphicsLayer { rotationZ = motion.x * 0.12f }
                .background(
                    Brush.radialGradient(
                        colors = listOf(tertiary.copy(alpha = 0.22f), tertiary.copy(alpha = 0f)),
                        radius = 430f
                    ),
                    CircleShape
                )
                .blur(54.dp)
        )
    }
}

@Composable
private fun FrostEdges(primary: Color, secondary: Color, collapse: Float) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.18f + collapse * 0.18f), Color.Transparent)
                    )
                )
                .blur(12.dp)
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, secondary.copy(alpha = 0.15f))
                    )
                )
                .blur(14.dp)
        )
    }
}

@Composable
private fun HeaderGlass(motion: MotionVector) {
    val lift by animateDpAsState(
        targetValue = (motion.y * -0.18f).dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "header_lift"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .graphicsLayer { translationY = lift.toPx() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.status_saver),
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.9).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "STATUS VAULT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.45.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.44f)
            )
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(R.string.more_options))
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
            val active = tab == selected
            val scale by animateFloatAsState(
                targetValue = if (active) 1f else 0.965f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "tab_scale"
            )
            Text(
                text = when (tab) {
                    StatusTab.All -> stringResource(R.string.tab_all)
                    StatusTab.Images -> stringResource(R.string.tab_images)
                    StatusTab.Videos -> stringResource(R.string.tab_videos)
                    StatusTab.Saved -> stringResource(R.string.tab_saved)
                },
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = if (active) 0.13f else 0.055f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(onClick = { onSelected(tab) })
                    .padding(horizontal = 15.dp, vertical = 9.dp),
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (active) 0.96f else 0.60f)
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
    val context = LocalContext.current
    val colors = rememberMediaColors(status.file)
    val pressSource = remember { MutableInteractionSource() }
    val pressed by pressSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow),
        label = "card_press"
    )
    val dateText = remember(status.timestamp) {
        dateFormatThreadLocal.get()?.format(Date(status.timestamp)).orEmpty()
    }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(status.file)
            .crossfade(true)
            .build()
    )
    val bgA = colors?.primary ?: MaterialTheme.colorScheme.surface
    val bgB = colors?.secondary ?: MaterialTheme.colorScheme.surfaceVariant

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = motion.x * 0.035f
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(pressSource, indication = null, onClick = onCardClick)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .blur(26.dp)
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(bgA.copy(alpha = 0.44f), bgB.copy(alpha = 0.28f))
                    )
                )
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.05f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (status.isVideo) 0.82f else 0.78f)
            ) {
                Image(
                    painter = painter,
                    contentDescription = status.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (status.isVideo) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.34f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.24f))
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(R.string.more_options), tint = Color.White)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        status.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        dateText,
                        color = Color.White.copy(alpha = 0.66f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaActionSheet(
    status: Status,
    isSavedTab: Boolean,
    colors: MediaColors?,
    sheetState: androidx.compose.material3.SheetState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRepost: () -> Unit
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context).data(status.file).crossfade(true).build()
    )
    val primary = colors?.primary ?: MaterialTheme.colorScheme.surface
    val secondary = colors?.secondary ?: MaterialTheme.colorScheme.surfaceVariant

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Transparent,
        dragHandle = null
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(34.dp)
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(primary.copy(alpha = 0.62f), secondary.copy(alpha = 0.46f))
                        )
                    )
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(status.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (status.isVideo) stringResource(R.string.video) else stringResource(R.string.share_via),
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 9.sp,
                            letterSpacing = 1.15.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (isSavedTab) {
                    FrostActionRow(Icons.Outlined.Delete, stringResource(R.string.delete), onDelete)
                } else {
                    FrostActionRow(Icons.Outlined.PushPin, stringResource(R.string.save), onSave)
                }
                FrostActionRow(Icons.Outlined.Share, stringResource(R.string.share), onShare)
                FrostActionRow(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.repost_to_whatsapp), onRepost)
            }
        }
    }
}

@Composable
private fun FrostActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "action_press"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(18.dp))
            .clickable(source, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        Spacer(Modifier.size(12.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun MediaViewer(
    status: Status,
    colors: MediaColors?,
    onClose: () -> Unit,
    onOpenWith: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? MainActivity
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    val primary = colors?.primary ?: MaterialTheme.colorScheme.background
    val secondary = colors?.secondary ?: MaterialTheme.colorScheme.background

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.38f), secondary.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.82f)),
                    radius = 1000f
                )
            )
    ) {
        if (status.isVideo) {
            AndroidView(
                factory = { viewContext ->
                    VideoView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setVideoURI(FileProvider.getUriForFile(viewContext, "${viewContext.packageName}.provider", status.file))
                        setOnPreparedListener { it.isLooping = true; start() }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context).data(status.file).crossfade(true).build()
            )
            Image(
                painter = painter,
                contentDescription = status.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(18.dp)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(status.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.30f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onOpenWith) {
                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share), tint = Color.White)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(R.string.more_options), tint = Color.White)
            }
        }
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
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
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
        Toast.makeText(context, R.string.no_app_found_open, Toast.LENGTH_SHORT).show()
    }
}

private fun shareOrRepost(context: Context, file: File, share: Boolean) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension.equals("mp4", true)) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!share) setPackage("com.whatsapp")
        }
        context.startActivity(if (share) Intent.createChooser(intent, null) else intent)
    }.onFailure {
        Toast.makeText(context, R.string.no_app_to_handle, Toast.LENGTH_SHORT).show()
    }
}
