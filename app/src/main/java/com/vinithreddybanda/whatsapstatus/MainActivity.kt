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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import androidx.compose.ui.input.pointer.pointerInput
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

private val dateFormatThreadLocal = ThreadLocal.withInitial { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

data class MediaColors(val primary: Color, val secondary: Color, val tertiary: Color)

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen(); super.onCreate(savedInstanceState); enableEdgeToEdge(); enterImmersiveFullscreen()
        setContent { WhatsapStatusTheme { HomeScreen() } }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) enterImmersiveFullscreen() }
    private fun enterImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.statusBarColor = AndroidColor.TRANSPARENT; window.navigationBarColor = AndroidColor.TRANSPARENT
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
        var sx = 0f; var sy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                sx = sx * 0.88f + (event.values.getOrNull(0) ?: 0f).coerceIn(-10f, 10f) * 0.12f
                sy = sy * 0.88f + (event.values.getOrNull(1) ?: 0f).coerceIn(-10f, 10f) * 0.12f
                motion = MotionVector(sx, sy)
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
    LaunchedEffect(file?.absolutePath) { colors = file?.let { sampleMediaColors(it) } }
    return colors
}
private suspend fun sampleMediaColors(file: File): MediaColors? = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = if (file.extension.equals("mp4", true) || file.extension.equals("mkv", true)) {
            MediaMetadataRetriever().run { try { setDataSource(file.absolutePath); getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) } finally { release() } }
        } else BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
        val sw = minOf(bitmap.width.coerceAtLeast(1), 180)
        val sh = maxOf(1, (bitmap.height.toFloat() * sw / bitmap.width.coerceAtLeast(1)).roundToInt())
        val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, sw, sh, true)
        if (scaled !== bitmap) bitmap.recycle()
        fun sample(x0: Int, x1: Int): Color {
            var r = 0L; var g = 0L; var b = 0L; var n = 0L
            val dx = maxOf(1, scaled.width / 18); val dy = maxOf(1, scaled.height / 18)
            var y = 0
            while (y < scaled.height) { var x = x0
                while (x < x1.coerceAtMost(scaled.width)) { val p = scaled.getPixel(x, y); r += AndroidColor.red(p); g += AndroidColor.green(p); b += AndroidColor.blue(p); n++; x += dx }
                y += dy
            }
            if (n == 0L) Color.DarkGray else Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
        }
        val all = sample(0, scaled.width); val left = sample(0, scaled.width / 2); val right = sample(scaled.width / 2, scaled.width); scaled.recycle()
        MediaColors(soften(all), soften(left), soften(right))
    }.getOrNull()
}
private fun soften(c: Color): Color {
    val l = c.red * .2126f + c.green * .7152f + c.blue * .0722f; val lift = if (l < .24f) .16f else 0f
    return Color((c.red + lift).coerceIn(0f,1f), (c.green + lift).coerceIn(0f,1f), (c.blue + lift).coerceIn(0f,1f), 1f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current; val lifecycleOwner = LocalLifecycleOwner.current; val motion = rememberMotionVector()
    var hasPermission by remember { mutableStateOf(checkPermission(context)) }; var selectedStatus by remember { mutableStateOf<Status?>(null) }; var viewerStatus by remember { mutableStateOf<Status?>(null)}
    var ambientColors by remember { mutableStateOf<MediaColors?>(null) }; var collapseTarget by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope(); val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) openStorageSettings(context, launcher) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { hasPermission = checkPermission(context); if (hasPermission) viewModel.getStatuses() } }
        lifecycleOwner.lifecycle.addObserver(observer); onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(hasPermission) { if (hasPermission) viewModel.getStatuses() }
    val tabs = listOf(StatusTab.All, StatusTab.Images, StatusTab.Videos, StatusTab.Saved)
    val pagerState = rememberPagerState(initialPage = tabs.indexOf(viewModel.selectedTab).coerceAtLeast(0), pageCount = { tabs.size })
    LaunchedEffect(pagerState) { snapshotFlow { pagerState.currentPage }.collect { page -> if (page in tabs.indices) viewModel.onTabSelected(tabs[page]) } }
    val a by animateColorAsState(ambientColors?.primary ?: MaterialTheme.colorScheme.background, tween(700), label = "ambient_a")
    val b by animateColorAsState(ambientColors?.secondary ?: MaterialTheme.colorScheme.background, tween(820), label = "ambient_b")
    val c by animateColorAsState(ambientColors?.tertiary ?: MaterialTheme.colorScheme.background, tween(900), label = "ambient_c")
    val collapse by animateFloatAsState(collapseTarget, spring(dampingRatio = .84f, stiffness = 280f), label = "collapse")
    val chromeHeight by animateDpAsState((122.dp * (1f-collapse)).coerceAtLeast(0.dp), spring(dampingRatio = .88f, stiffness = 360f), label = "chrome_height")

    Box(Modifier.fillMaxSize()) {
        FluxBackdrop(a,b,c,motion)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(chromeHeight).graphicsLayer { alpha = 1f-collapse; translationY = -22f*collapse }) {
                HeaderGlass(motion)
                GlassTabBar(tabs, viewModel.selectedTab, { tab -> scope.launch { pagerState.animateScrollToPage(tabs.indexOf(tab)) } }, Modifier.align(Alignment.BottomStart).padding(horizontal=14.dp, vertical=8.dp))
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), beyondViewportPageCount = 1, key = { tabs[it].title }) { page ->
                val active = pagerState.currentPage == page
                StatusPage(
                    statuses = viewModel.getStatusesForTab(tabs[page]), isFetching = viewModel.isFetching, active = active,
                    onCollapseChanged = { if(active) collapseTarget = it }, onAmbientColors = { if(active) ambientColors = it },
                    motion = motion, onCardClick = { viewerStatus = it }, onMenuClick = { selectedStatus = it }
                )
            }
        }
        FrostEdges(a,b,collapse)
        selectedStatus?.let { status ->
            MediaActionSheet(status, viewModel.selectedTab == StatusTab.Saved, rememberMediaColors(status.file), sheetState,
                onClose = { scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus = null } },
                onSave = { viewModel.saveStatus(status) { ok -> Toast.makeText(context, context.getString(if(ok) R.string.saved else R.string.failed_to_save), Toast.LENGTH_SHORT).show(); scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus=null } } },
                onDelete = { viewModel.deleteStatus(status) { ok -> Toast.makeText(context, context.getString(if(ok) R.string.deleted else R.string.failed_to_delete), Toast.LENGTH_SHORT).show(); scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus=null } } },
                onShare = { shareOrRepost(context,status.file,true); scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus=null } },
                onRepost = { shareOrRepost(context,status.file,false); scope.launch { sheetState.hide() }.invokeOnCompletion { selectedStatus=null } }
            )
        }
        viewerStatus?.let { MediaViewer(it, rememberMediaColors(it.file), onClose = { viewerStatus=null }, onOpenWith = { shareOrRepost(context,it.file,true) }) }
    }
}

@Composable
private fun StatusPage(statuses: List<Status>, isFetching: Boolean, active: Boolean, onCollapseChanged: (Float)->Unit, onAmbientColors: (MediaColors)->Unit, motion: MotionVector, onCardClick:(Status)->Unit, onMenuClick:(Status)->Unit) {
    val state = rememberLazyStaggeredGridState(); val ambientStatus = statuses.getOrNull(state.firstVisibleItemIndex); val colors = rememberMediaColors(ambientStatus?.file)
    LaunchedEffect(ambientStatus?.path, colors, active) { if(active) colors?.let(onAmbientColors) }
    LaunchedEffect(state, active) { if(!active) return@LaunchedEffect; snapshotFlow { if(state.firstVisibleItemIndex>0) 1f else (state.firstVisibleItemScrollOffset/150f).coerceIn(0f,1f) }.collect(onCollapseChanged) }
    Box(Modifier.fillMaxSize()) {
        if(statuses.isNotEmpty()) LazyVerticalStaggeredGrid(state=state, columns=StaggeredGridCells.Fixed(2), contentPadding=PaddingValues(14.dp,10.dp,14.dp,28.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()), horizontalArrangement=Arrangement.spacedBy(12.dp), verticalItemSpacing=12.dp, modifier=Modifier.fillMaxSize()) {
            items(statuses,key={it.path}) { StatusCard(it,motion,{onCardClick(it)},{onMenuClick(it)}) }
        } else if(!isFetching) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(stringResource(R.string.no_statuses_available),color=MaterialTheme.colorScheme.onBackground.copy(alpha=.64f))}
    }
}

@Composable
private fun FluxBackdrop(primary:Color,secondary:Color,tertiary:Color,motion:MotionVector){
    val t=rememberInfiniteTransition(label="flux"); val p by t.animateFloat(0f,1f,infiniteRepeatable(tween(14500,easing=FastOutSlowInEasing)),label="phase"); val dx=motion.x*2.2f+p*20f; val dy=motion.y*1.7f+(1f-p)*16f
    Box(Modifier.fillMaxSize()){
        Box(Modifier.size(360.dp).graphicsLayer{translationX=dx*density;translationY=dy*density}.background(Brush.radialGradient(listOf(primary.copy(alpha=.38f),primary.copy(alpha=0f)),600f),CircleShape).blur(62.dp))
        Box(Modifier.size(320.dp).align(Alignment.BottomEnd).graphicsLayer{translationX=-dx*.75f*density;translationY=-dy*.7f*density}.background(Brush.radialGradient(listOf(secondary.copy(alpha=.30f),secondary.copy(alpha=0f)),540f),CircleShape).blur(64.dp))
        Box(Modifier.size(280.dp).align(Alignment.CenterEnd).graphicsLayer{rotationZ=motion.x*.12f}.background(Brush.radialGradient(listOf(tertiary.copy(alpha=.24f),tertiary.copy(alpha=0f)),460f),CircleShape).blur(60.dp))
    }
}

@Composable
private fun FrostEdges(primary:Color,secondary:Color,collapse:Float){Box(Modifier.fillMaxSize()){
    Box(Modifier.fillMaxWidth().height(62.dp).background(Brush.verticalGradient(listOf(primary.copy(alpha=.18f+collapse*.20f),Color.Transparent))).blur(18.dp))
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(68.dp).background(Brush.verticalGradient(listOf(Color.Transparent,secondary.copy(alpha=.16f+collapse*.06f)))).blur(20.dp))
}}

@Composable
private fun HeaderGlass(motion:MotionVector){val y by animateDpAsState((motion.y*-.16f).dp,spring(stiffness=Spring.StiffnessLow),label="header")
    Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp).graphicsLayer{translationY=y.toPx()},verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(stringResource(R.string.status_saver),fontSize=27.sp,fontWeight=FontWeight.Black,letterSpacing=(-.9).sp,color=MaterialTheme.colorScheme.onBackground);Text("STATUS VAULT",fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.45.sp,color=MaterialTheme.colorScheme.onBackground.copy(alpha=.44f))};IconButton(onClick={}){Icon(Icons.Default.MoreHoriz,stringResource(R.string.more_options))}}
}

@Composable
private fun GlassTabBar(tabs:List<StatusTab>,selected:StatusTab,onSelected:(StatusTab)->Unit,modifier:Modifier=Modifier){LazyRow(modifier,horizontalArrangement=Arrangement.spacedBy(8.dp)){items(tabs,key={it.title}){tab->val active=tab==selected;val scale by animateFloatAsState(if(active)1f else .965f,spring(stiffness=Spring.StiffnessMedium),label="tab");Text(when(tab){StatusTab.All->stringResource(R.string.tab_all);StatusTab.Images->stringResource(R.string.tab_images);StatusTab.Videos->stringResource(R.string.tab_videos);StatusTab.Saved->stringResource(R.string.tab_saved)},Modifier.graphicsLayer{scaleX=scale;scaleY=scale}.clip(RoundedCornerShape(18.dp)).background(if(active)Color.White.copy(alpha=.10f) else Color.White.copy(alpha=.035f)).border(1.dp,Color.White.copy(alpha=if(active).14f else .05f),RoundedCornerShape(18.dp)).clickable{onSelected(tab)}.padding(horizontal=15.dp,vertical=9.dp),fontSize=13.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,color=MaterialTheme.colorScheme.onBackground.copy(alpha=if(active).96f else .60f))}}
}

@Composable
private fun StatusCard(status:Status,motion:MotionVector,onCardClick:()->Unit,onMenuClick:()->Unit){
    val context=LocalContext.current; val colors=rememberMediaColors(status.file); val press=remember{MutableInteractionSource()}; val pressed by press.collectIsPressedAsState(); val scale by animateFloatAsState(if(pressed).975f else 1f,spring(dampingRatio=.76f,stiffness=Spring.StiffnessMediumLow),label="card"); val date=remember(status.timestamp){dateFormatThreadLocal.get()?.format(Date(status.timestamp)).orEmpty()}; val painter=rememberAsyncImagePainter(ImageRequest.Builder(context).data(status.file).crossfade(true).build()); val a=colors?.primary?:MaterialTheme.colorScheme.surface; val b=colors?.secondary?:MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale;rotationZ=motion.x*.03f}.clip(RoundedCornerShape(24.dp)).clickable(press,indication=null,onClick=onCardClick)){
        Image(painter,null,ContentScale.Crop,Modifier.matchParentSize().blur(30.dp)); Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(a.copy(alpha=.45f),b.copy(alpha=.25f))))); Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha=.05f))){Box(Modifier.fillMaxWidth().aspectRatio(if(status.isVideo).82f else .78f)){Image(painter,status.title,ContentScale.Crop,Modifier.fillMaxSize());if(status.isVideo)Box(Modifier.align(Alignment.Center).size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha=.34f)).border(1.dp,Color.White.copy(alpha=.18f),CircleShape),Alignment.Center){Icon(Icons.Default.PlayArrow,null,tint=Color.White,modifier=Modifier.size(28.dp))};IconButton(onClick=onMenuClick,modifier=Modifier.align(Alignment.TopEnd).padding(7.dp).size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha=.22f))){Icon(Icons.Default.MoreHoriz,stringResource(R.string.more_options),tint=Color.White)}};Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha=.06f)).padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(status.title,Color.White,12.sp,FontWeight.SemiBold,maxLines=1);Text(date,Color.White.copy(alpha=.66f),9.sp)}}}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaActionSheet(status:Status,isSavedTab:Boolean,colors:MediaColors?,sheetState:androidx.compose.material3.SheetState,onClose:()->Unit,onSave:()->Unit,onDelete:()->Unit,onShare:()->Unit,onRepost:()->Unit){val context=LocalContext.current;val painter=rememberAsyncImagePainter(ImageRequest.Builder(context).data(status.file).crossfade(true).build());val p=colors?.primary?:MaterialTheme.colorScheme.surface;val s=colors?.secondary?:MaterialTheme.colorScheme.surfaceVariant
    ModalBottomSheet(onDismissRequest=onClose,sheetState=sheetState,containerColor=Color.Transparent,scrimColor=Color.Transparent,dragHandle=null){Box(Modifier.fillMaxWidth().padding(horizontal=10.dp).clip(RoundedCornerShape(topStart=30.dp,topEnd=30.dp))){Image(painter,null,ContentScale.Crop,Modifier.matchParentSize().blur(36.dp));Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(p.copy(alpha=.58f),s.copy(alpha=.42f)))));Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha=.24f)).padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(status.title,Color.White,FontWeight.Bold,16.sp);Text(if(status.isVideo)stringResource(R.string.video) else stringResource(R.string.share_via),Color.White.copy(alpha=.68f),9.sp,letterSpacing=1.15.sp)};IconButton(onClick=onClose){Icon(Icons.Default.Close,stringResource(R.string.close),tint=Color.White)}};Spacer(Modifier.height(8.dp));if(isSavedTab) FrostActionRow(Icons.Outlined.Delete,stringResource(R.string.delete),onDelete) else FrostActionRow(Icons.Outlined.PushPin,stringResource(R.string.save),onSave);FrostActionRow(Icons.Outlined.Share,stringResource(R.string.share),onShare);FrostActionRow(Icons.AutoMirrored.Outlined.Send,stringResource(R.string.repost_to_whatsapp),onRepost)}}}}

@Composable
private fun FrostActionRow(icon:ImageVector,title:String,onClick:()->Unit){val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState();val scale by animateFloatAsState(if(pressed).975f else 1f,spring(stiffness=Spring.StiffnessMedium),label="action");Row(Modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale}.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha=.10f)).border(1.dp,Color.White.copy(alpha=.11f),RoundedCornerShape(18.dp)).clickable(source,indication=null,onClick=onClick).padding(horizontal=14.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Color.White,modifier=Modifier.size(21.dp));Spacer(Modifier.size(12.dp));Text(title,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=13.sp)};Spacer(Modifier.height(9.dp))}

@Composable
private fun MediaViewer(status:Status,colors:MediaColors?,onClose:()->Unit,onOpenWith:()->Unit){val context=LocalContext.current;val p=colors?.primary?:Color.Black;val s=colors?.secondary?:Color.Black;LaunchedEffect(Unit){(context as? MainActivity)?.window?.let{WindowCompat.setDecorFitsSystemWindows(it,false);WindowCompat.getInsetsController(it,it.decorView).hide(WindowInsetsCompat.Type.systemBars())}}
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(p.copy(alpha=.34f),s.copy(alpha=.16f),Color.Black.copy(alpha=.90f)),1000f)).pointerInput(Unit){detectVerticalDragGestures{_,dragY->if(dragY>24f)onClose()}}){
        if(status.isVideo) AndroidView(factory={vc->VideoView(vc).apply{layoutParams=ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);setVideoURI(FileProvider.getUriForFile(vc,"${vc.packageName}.provider",status.file));setOnPreparedListener{it.isLooping=true;start()}}},modifier=Modifier.fillMaxSize()) else {val painter=rememberAsyncImagePainter(ImageRequest.Builder(context).data(status.file).crossfade(true).build());Image(painter,status.title,ContentScale.Fit,Modifier.fillMaxSize().padding(12.dp))}
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(74.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.30f),Color.Transparent))).blur(16.dp));Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp).background(Brush.verticalGradient(listOf(Color.Transparent,p.copy(alpha=.22f)))).blur(20.dp))
        IconButton(onClick=onClose,modifier=Modifier.padding(12.dp).size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(p.copy(alpha=.30f),s.copy(alpha=.22f))))){Icon(Icons.Default.Close,stringResource(R.string.close),tint=Color.White)}
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()+16.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(p.copy(alpha=.35f),s.copy(alpha=.25f)))).border(1.dp,Color.White.copy(alpha=.14f),RoundedCornerShape(24.dp)).padding(horizontal=8.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){IconButton(onClick=onOpenWith){Icon(Icons.Outlined.Share,stringResource(R.string.share),tint=Color.White)};IconButton(onClick={}){Icon(Icons.Default.MoreHoriz,stringResource(R.string.more_options),tint=Color.White)}}
    }
}

private fun checkPermission(context:Context):Boolean=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R)Environment.isExternalStorageManager() else ContextCompat.checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
private fun openStorageSettings(context:Context,launcher:androidx.activity.result.ActivityResultLauncher<String>){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R)runCatching{context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply{data=Uri.parse("package:${context.packageName}")})}.onFailure{context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))}else launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)}
private fun openFile(context:Context,file:File){runCatching{val uri=FileProvider.getUriForFile(context,"${context.packageName}.provider",file);Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,if(file.extension.equals("mp4",true))"video/*" else "image/*");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}.also(context::startActivity)}.onFailure{Toast.makeText(context,R.string.no_app_found_open,Toast.LENGTH_SHORT).show()}}
private fun shareOrRepost(context:Context,file:File,share:Boolean){runCatching{val uri=FileProvider.getUriForFile(context,"${context.packageName}.provider",file);val intent=Intent(Intent.ACTION_SEND).apply{type=if(file.extension.equals("mp4",true))"video/*" else "image/*";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);if(!share)setPackage("com.whatsapp")};context.startActivity(if(share)Intent.createChooser(intent,null) else intent)}.onFailure{Toast.makeText(context,R.string.no_app_to_handle,Toast.LENGTH_SHORT).show()}}
