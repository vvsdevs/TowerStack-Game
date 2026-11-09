package com.game.towerstack

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.isActive
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFC6A4FF), surface = Color(0xFF191C39))) {
                TowerStack()
            }
        }
    }
}

@Composable
private fun TowerStack(vm: GameViewModel = viewModel()) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }.getOrNull() }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { vm.pause(); tones?.stopTone() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); tones?.release() }
    }
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previous = 0L
            while (isActive) {
                withFrameNanos { now ->
                    if (previous != 0L && vm.phase != Phase.HOME && vm.phase != Phase.PAUSED) vm.frame((now - previous) / 1_000_000_000f)
                    previous = now
                }
            }
        }
    }
    fun feedback(kind: Int) {
        if (vm.sound) tones?.startTone(kind, 85)
        if (vm.vibration) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
    val place = {
        vm.place()?.let { feedback(if (it.kept == null) ToneGenerator.TONE_PROP_NACK else if (it.perfect) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP) }
        Unit
    }
    BackHandler(vm.phase != Phase.HOME) {
        when (vm.phase) {
            Phase.PAUSED, Phase.GAME_OVER -> vm.home()
            else -> vm.pause()
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF080F24), Color(0xFF20204D), Color(0xFF32244F))))) {
        TowerCanvas(vm, place)
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(22.dp))
            Text("TOWER STACK", fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, color = Color.White)
            Text("ONE TAP. ONE LEVEL HIGHER.", fontSize = 10.sp, letterSpacing = 2.sp, color = Color(0xFFA7A5C6))
            if (vm.phase != Phase.HOME) {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${vm.score}", fontSize = 54.sp, fontWeight = FontWeight.Light, color = Color.White)
                        Text("BEST  ${vm.best}", fontSize = 12.sp, letterSpacing = 2.sp, color = Color(0xFFBFB5DC))
                    }
                    if (vm.phase == Phase.PLAYING || vm.phase == Phase.READY) {
                        OutlinedButton(onClick = { vm.pause() }, modifier = Modifier.semantics { contentDescription = "Pause game" }) { Text("?", fontSize = 22.sp) }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (vm.phase == Phase.HOME) {
                Text("Build a little higher.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Text("Find your rhythm. Reach the sky.", color = Color(0xFFBCB3D5), modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
                Button(onClick = { feedback(ToneGenerator.TONE_PROP_BEEP); vm.start() }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) { Text("Play", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                Text("PERSONAL BEST  ${vm.best}", color = Color(0xFFD5C9EB), fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.padding(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = vm.sound, onClick = vm::toggleSound, label = { Text("Sound ${if (vm.sound) "on" else "off"}") })
                    FilterChip(selected = vm.vibration, onClick = vm::toggleVibration, label = { Text("Vibration ${if (vm.vibration) "on" else "off"}") })
                }
            } else if (vm.phase == Phase.READY) {
                Text("TAP TO STACK", color = Color.White, fontSize = 14.sp, letterSpacing = 4.sp)
                Text("Line up the edges", color = Color(0xFFAFA5CB), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.height(28.dp))
        }
        if (vm.phase == Phase.PAUSED || vm.phase == Phase.GAME_OVER) {
            Box(Modifier.fillMaxSize().background(Color(0xB3080B1D)).pointerInput(Unit) { detectTapGestures {} }, contentAlignment = Alignment.Center) {
                Surface(Modifier.safeDrawingPadding().padding(28.dp).widthIn(max = 360.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color(0xFF20203F), tonalElevation = 8.dp) {
                    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val paused = vm.phase == Phase.PAUSED
                        Text(if (paused) "Take a breath" else "Beautifully stacked.", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (paused) "Your tower is waiting." else if (vm.newRecord) "NEW PERSONAL BEST" else "Every tower starts with one block.", color = Color(0xFFC9ACEF), fontSize = 12.sp)
                        Text("${vm.score}", fontSize = 64.sp, color = Color.White, fontWeight = FontWeight.Light)
                        Text("BEST  ${vm.best}", letterSpacing = 2.sp, color = Color(0xFFB8ADCF))
                        Button(onClick = { if (paused) vm.resume() else vm.start() }, modifier = Modifier.fillMaxWidth()) { Text(if (paused) "Resume" else "Play Again") }
                        if (paused) OutlinedButton(onClick = vm::start, modifier = Modifier.fillMaxWidth()) { Text("Restart") }
                        TextButton(onClick = vm::home, modifier = Modifier.fillMaxWidth()) { Text("Home") }
                    }
                }
            }
        }
    }
}

private val palette = listOf(Color(0xFF8065E8), Color(0xFFA468E9), Color(0xFFCD70CF), Color(0xFFE67FA9), Color(0xFFEF927D), Color(0xFFF4BF76))
private fun blockColor(level: Int): Color {
    val value = (level % 36) / 6f
    return lerp(palette[value.toInt()], palette[(value.toInt() + 1) % palette.size], value % 1f)
}

@Composable
private fun TowerCanvas(vm: GameViewModel, place: () -> Unit) {
    val path = remember { Path() }
    Canvas(Modifier.fillMaxSize().semantics {
        contentDescription = "Tower stacking play area"
        onClick("Stack moving block") { place(); true }
    }.pointerInput(vm) { detectTapGestures { place() } }) {
        @Suppress("UNUSED_VARIABLE") val frame = vm.revision // Invalidate drawing only, not the UI tree.
        val game = vm.engine
        val home = game.phase == Phase.HOME
        val scale = min(size.width / 8f, size.height / 13f)
        val baseY = size.height * if (home) .50f else .72f
        val camera = if (home) 0f else game.camera
        fun point(x: Float, z: Float, level: Float) = Offset(size.width / 2 + (x - z) * scale, baseY + (x + z) * scale * .48f - (level - camera) * scale * .39f)
        fun polygon(color: Color, a: Offset, b: Offset, c: Offset, d: Offset) {
            path.reset(); path.moveTo(a.x, a.y)
            path.lineTo(b.x, b.y); path.lineTo(c.x, c.y); path.lineTo(d.x, d.y)
            path.close(); drawPath(path, color)
        }
        fun slab(b: Block, fall: Float = 0f, alpha: Float = 1f) {
            val p = point(b.x, b.z, b.level.toFloat()) + Offset(0f, fall)
            if (p.y < -scale * 3 || p.y > size.height + scale * 3) return
            val a = Offset(p.x + (-b.width + b.depth) * scale / 2, p.y + (-b.width - b.depth) * scale * .24f)
            val r = Offset(p.x + (b.width + b.depth) * scale / 2, p.y + (b.width - b.depth) * scale * .24f)
            val c = Offset(p.x + (b.width - b.depth) * scale / 2, p.y + (b.width + b.depth) * scale * .24f)
            val l = Offset(p.x + (-b.width - b.depth) * scale / 2, p.y + (-b.width + b.depth) * scale * .24f)
            val down = Offset(0f, scale * .36f)
            val color = blockColor(b.level)
            polygon(lerp(color, Color.Black, .36f).copy(alpha = alpha), l, c, c + down, l + down)
            polygon(lerp(color, Color.Black, .2f).copy(alpha = alpha), c, r, r + down, c + down)
            polygon(color.copy(alpha = alpha), a, r, c, l)
            drawLine(Color.White.copy(alpha = .23f * alpha), l, a, 1.3f)
            drawLine(Color.White.copy(alpha = .32f * alpha), a, r, 1.3f)
        }
        // A faint floor halo anchors the tower without bitmap assets.
        drawOval(Color(0x147A65DB), topLeft = Offset(size.width * .17f, baseY + scale * .5f), size = androidx.compose.ui.geometry.Size(size.width * .66f, scale * .8f))
        if (home) {
            repeat(6) { slab(Block(it * .035f, it * -.025f, 2.25f - it * .11f, 2.25f - it * .11f, it)) }
            slab(Block(-.7f, 0f, 1.6f, 1.6f, 7))
        } else {
            game.tower.forEach { slab(it) }
            game.debris.forEach { slab(it.block, it.age * it.age * scale * 11f, (1f - it.age / 1.5f).coerceIn(0f, 1f)) }
            if (game.phase != Phase.GAME_OVER) slab(game.moving)
            game.sparks.forEach {
                val origin = point(it.x, it.z, it.level.toFloat())
                val t = it.age
                drawCircle(Color(0xFFFFD99E).copy(alpha = (1f - t / .7f).coerceIn(0f, 1f)), 2.5f, origin + Offset(it.vx * t, it.vy * t + 95f * t * t))
            }
        }
    }
    PerfectFeedback(vm)
}

@Composable
private fun PerfectFeedback(vm: GameViewModel) {
    val visible by remember { derivedStateOf { vm.revision; vm.engine.perfectTime > 0f && vm.phase == Phase.PLAYING } }
    if (visible) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PERFECT!", modifier = Modifier.offset(y = (-100).dp), color = Color(0xFFFFDDA7), fontWeight = FontWeight.Bold, letterSpacing = 4.sp, fontSize = 20.sp)
    }
}
