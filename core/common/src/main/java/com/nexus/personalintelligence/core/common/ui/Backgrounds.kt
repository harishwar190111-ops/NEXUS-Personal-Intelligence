package com.nexus.personalintelligence.core.common.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class BackgroundType(val label: String) {
    FluidWaves("Fluid waves"),
    Aurora("Aurora"),
    FlowingGradient("Flowing gradient"),
    LightWaves("Abstract light waves"),
    LayeredGradients("Layered gradients"),
    Particles("Ambient particles"),
    ColourDrift("Cinematic colour drift"),
    Image("Custom image"),
}

/** Low/Balanced/High: frame interval, number of wave/blob layers, and wave sampling step. */
enum class AnimQuality(val label: String, val frameMs: Long, val layers: Int, val stepDp: Int) {
    Low("Low", 66L, 2, 40),
    Balanced("Balanced", 40L, 3, 24),
    High("High", 25L, 5, 14),
}

data class BackgroundSpec(
    val type: BackgroundType = BackgroundType.FluidWaves,
    val primary: NexusAccent = NexusAccent.Cyan,
    val secondary: NexusAccent = NexusAccent.Azure,
    val speed: Int = 3,
    val intensity: Int = 3,
    val brightness: Int = 80,
    val quality: AnimQuality = AnimQuality.Balanced,
    val animate: Boolean = true,
    val density: Int = 3,
    val reverse: Boolean = false,
)

/** Text encoding for persisting a [BackgroundSpec]. Pure Kotlin, unit-tested. */
object BackgroundCodec {
    fun encode(s: BackgroundSpec): String = listOf(
        s.type.name, s.primary.name, s.secondary.name, s.speed, s.intensity,
        s.brightness, s.quality.name, s.animate, s.density, s.reverse,
    ).joinToString("|")

    fun decode(raw: String?): BackgroundSpec? {
        if (raw.isNullOrBlank()) return null
        val p = raw.split('|')
        if (p.size < 10) return null
        val type = BackgroundType.entries.firstOrNull { it.name == p[0] } ?: return null
        return BackgroundSpec(
            type = type,
            primary = NexusAccent.fromId(p[1]),
            secondary = NexusAccent.fromId(p[2]),
            speed = p[3].toIntOrNull()?.coerceIn(1, 5) ?: 3,
            intensity = p[4].toIntOrNull()?.coerceIn(1, 5) ?: 3,
            brightness = p[5].toIntOrNull()?.coerceIn(30, 100) ?: 80,
            quality = AnimQuality.entries.firstOrNull { it.name == p[6] } ?: AnimQuality.Balanced,
            animate = p[7].toBooleanStrictOrNull() ?: true,
            density = p[8].toIntOrNull()?.coerceIn(1, 5) ?: 3,
            reverse = p[9].toBooleanStrictOrNull() ?: false,
        )
    }
}

private fun wrap01(v: Float): Float {
    val m = v % 1f
    return if (m < 0f) m + 1f else m
}

/**
 * Lightweight 2D animated background drawn with Canvas (no 3D engine, no blur, no shaders).
 * - Animation only runs while the screen is RESUMED and animation is enabled.
 * - Frame rate is capped by the quality level.
 * - If frames keep arriving much later than the budget, quality steps down automatically.
 * This is a runtime safeguard, not a measured benchmark.
 */
@Composable
fun AnimatedBackdrop(spec: BackgroundSpec, image: ImageBitmap?, modifier: Modifier = Modifier) {
    if (spec.type == BackgroundType.Image) {
        Box(modifier.fillMaxSize()) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (1f - spec.brightness / 100f + 0.25f).coerceIn(0f, 0.8f))),
                )
            }
        }
        return
    }

    val time = remember { mutableLongStateOf(0L) }
    var degraded by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val animated = spec.animate && spec.type != BackgroundType.LayeredGradients
    val effective = AnimQuality.entries[(spec.quality.ordinal - degraded).coerceAtLeast(0)]
    val path = remember { Path() }
    val seeds = remember {
        val r = Random(7)
        FloatArray(4 * 160) { r.nextFloat() }
    }

    LaunchedEffect(animated, spec.quality, lifecycle) {
        degraded = 0
        if (!animated) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var start = 0L
            var lastDraw = 0L
            var slow = 0
            while (true) {
                withFrameNanosCompat { now ->
                    if (start == 0L) {
                        start = now
                        lastDraw = now
                    }
                    val budget = AnimQuality.entries[(spec.quality.ordinal - degraded).coerceAtLeast(0)].frameMs * 1_000_000L
                    val delta = now - lastDraw
                    if (delta >= budget) {
                        if (delta > budget * 22 / 10) slow++ else if (slow > 0) slow--
                        lastDraw = now
                        time.longValue = now - start
                        if (slow >= 12 && degraded < spec.quality.ordinal) {
                            degraded += 1
                            slow = 0
                        }
                    }
                }
            }
        }
    }

    val primary = spec.primary.color
    val secondary = spec.secondary.color

    Canvas(modifier.fillMaxSize()) {
        val t = (time.longValue / 1_000_000_000.0).toFloat()
        val dir = if (spec.reverse) -1f else 1f
        val sp = (0.15f + 0.12f * spec.speed) * dir
        val a = 0.10f + 0.07f * spec.intensity
        val w = size.width
        val h = size.height

        when (spec.type) {
            BackgroundType.FluidWaves -> {
                val layers = effective.layers
                val stepPx = effective.stepDp.dp.toPx()
                for (i in 0 until layers) {
                    val f = i / (layers - 1f).coerceAtLeast(1f)
                    val baseY = h * (0.52f + 0.40f * f)
                    val amp = h * (0.03f + 0.012f * spec.intensity) * (1f - 0.35f * f)
                    val k = (2f * PI.toFloat()) / (w * (0.55f + 0.35f * f))
                    val phase = t * sp * (1f + 0.6f * f) + i * 1.7f
                    path.reset()
                    path.moveTo(0f, h)
                    var x = 0f
                    while (x <= w + stepPx) {
                        val y = baseY + amp * sin(k * x + phase) + amp * 0.5f * sin(2.3f * k * x - phase * 0.7f)
                        path.lineTo(x, y)
                        x += stepPx
                    }
                    path.lineTo(w + stepPx, h)
                    path.close()
                    val c = lerp(primary, secondary, f)
                    drawPath(
                        path,
                        Brush.verticalGradient(
                            colors = listOf(c.copy(alpha = a), c.copy(alpha = 0.02f)),
                            startY = baseY - amp,
                            endY = h,
                        ),
                    )
                }
            }
            BackgroundType.Aurora -> {
                val blobs = 2 + effective.layers
                for (i in 0 until blobs) {
                    val seed = i * 1.37f
                    val cx = w * (0.5f + 0.42f * sin(t * sp * 0.6f + seed))
                    val cy = h * (0.35f + 0.28f * cos(t * sp * 0.45f + seed * 1.7f))
                    val r = size.maxDimension * (0.28f + 0.05f * (i % 3))
                    val c = if (i % 2 == 0) primary else secondary
                    drawCircle(
                        Brush.radialGradient(
                            colors = listOf(c.copy(alpha = a * 0.9f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(cx, cy),
                    )
                }
            }
            BackgroundType.FlowingGradient -> {
                val ang = t * sp * 0.5f
                val len = size.maxDimension * 0.75f
                val dx = cos(ang) * len
                val dy = sin(ang) * len
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(primary.copy(alpha = a + 0.1f), secondary.copy(alpha = a), primary.copy(alpha = a * 0.6f)),
                        start = Offset(w / 2f - dx, h / 2f - dy),
                        end = Offset(w / 2f + dx, h / 2f + dy),
                    ),
                )
            }
            BackgroundType.LightWaves -> {
                val lines = 3 + effective.layers * 2
                val stepPx = effective.stepDp.dp.toPx()
                val amp = h * (0.03f + 0.01f * spec.intensity)
                val k = (2f * PI.toFloat()) / (w * 0.8f)
                for (i in 0 until lines) {
                    val f = i / (lines - 1f)
                    val baseY = h * (0.22f + 0.58f * f)
                    path.reset()
                    var x = 0f
                    path.moveTo(0f, baseY)
                    while (x <= w + stepPx) {
                        path.lineTo(x, baseY + amp * sin(k * x + t * sp * (0.8f + 0.5f * f) + i * 0.55f))
                        x += stepPx
                    }
                    drawPath(path, lerp(primary, secondary, f).copy(alpha = a * 0.9f), style = Stroke(width = 2.dp.toPx()))
                }
            }
            BackgroundType.LayeredGradients -> {
                drawRect(Brush.linearGradient(listOf(primary.copy(alpha = a), Color.Transparent), start = Offset.Zero, end = Offset(w, h)))
                drawRect(Brush.radialGradient(listOf(secondary.copy(alpha = a), Color.Transparent), center = Offset(w * 0.8f, h * 0.75f), radius = size.maxDimension * 0.6f))
                drawRect(Brush.radialGradient(listOf(primary.copy(alpha = a * 0.8f), Color.Transparent), center = Offset(w * 0.15f, h * 0.2f), radius = size.maxDimension * 0.45f))
            }
            BackgroundType.Particles -> {
                val n = ((8 + spec.density * 14) * (if (effective == AnimQuality.Low) 0.5f else 1f)).toInt().coerceIn(6, 160)
                for (i in 0 until n) {
                    val s0 = seeds[i * 4]
                    val s1 = seeds[i * 4 + 1]
                    val s2 = seeds[i * 4 + 2]
                    val s3 = seeds[i * 4 + 3]
                    val x = wrap01(s0 + t * sp * 0.012f * (0.4f + s2)) * w
                    val y = wrap01(s1 + 0.03f * sin(t * 0.6f + s2 * 6.28f)) * h
                    val r = (1.2f + 3.2f * s3).dp.toPx()
                    val c = if (i % 2 == 0) primary else secondary
                    drawCircle(c.copy(alpha = (0.18f + 0.5f * s3) * (0.5f + 0.1f * spec.intensity)), radius = r, center = Offset(x, y))
                }
            }
            BackgroundType.ColourDrift -> {
                val m = (sin(t * sp * 0.35f) + 1f) / 2f
                val c1 = lerp(primary, secondary, m)
                val c2 = lerp(secondary, primary, m)
                drawRect(Brush.verticalGradient(listOf(c1.copy(alpha = a), Color.Transparent, c2.copy(alpha = a * 0.8f))))
            }
            BackgroundType.Image -> Unit
        }
        drawRect(Color.Black.copy(alpha = (1f - spec.brightness / 100f).coerceIn(0f, 0.7f)))
    }
}

private suspend fun <R> withFrameNanosCompat(onFrame: (Long) -> R): R =
    androidx.compose.runtime.withFrameNanos(onFrame)
