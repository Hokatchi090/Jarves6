package com.jarvis.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import java.util.Locale

class JarvisDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cyan = Color.rgb(65, 224, 244)
    private val cyanBright = Color.rgb(145, 247, 255)
    private val white = Color.rgb(235, 250, 252)

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var elapsed = 0f
    private var voiceLevel = 0f
    private var speaking = false

    private var moduleMenuVisible = false
    private var appsModuleVisible = false
    private var clockVisible = true
    private var installedAppNames = emptyList<String>()
    private var appClickListener: ((String) -> Unit)? = null

    private val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
    }
    private var listening = false
    private var hudState = JarvisHudState.READY

    private data class Particle(
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        val phase: Float
    )

    private val random = Random(73129)
    private val particles = ArrayList<Particle>()

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        repeat(95) {
            particles.add(
                Particle(
                    x = random.nextFloat() * 2f - 1f,
                    y = random.nextFloat() * 2f - 1f,
                    size = 0.6f + random.nextFloat() * 2.3f,
                    speed = 0.018f + random.nextFloat() * 0.075f,
                    phase = random.nextFloat() * 6.28318f
                )
            )
        }
    }

    fun setVoiceLevel(level: Float) {
        voiceLevel = level.coerceIn(0f, 1f)
        listening = level > 0.04f
        invalidate()
    }

    fun setSpeaking(value: Boolean) {
        speaking = value
        if (!value) {
            voiceLevel *= 0.35f
        }
        invalidate()
    }

    fun setHudState(state: JarvisHudState) {
        hudState = state
        speaking = state == JarvisHudState.SPEAKING
        listening = state == JarvisHudState.LISTENING
        invalidate()
    }

    fun setModuleMenuVisible(visible: Boolean) {
        moduleMenuVisible = visible
        invalidate()
    }

    fun setAppsModule(visible: Boolean, apps: List<String> = emptyList()) {
        appsModuleVisible = visible
        installedAppNames = apps.take(6)
        invalidate()
    }

    fun setClockVisible(visible: Boolean) {
        clockVisible = visible
        invalidate()
    }

    fun setAppClickListener(listener: (String) -> Unit) {
        appClickListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        elapsed += 0.016f

        val cx = width / 2f
        val cy = height / 2f
        val base = minOf(width, height) * 0.31f

        drawAmbientGlow(canvas, cx, cy, base)
        drawParticles(canvas, cx, cy, base)
        drawOuterHud(canvas, cx, cy, base)
        drawMainRings(canvas, cx, cy, base)
        drawRadialTicks(canvas, cx, cy, base)
        drawOrbitElements(canvas, cx, cy, base)
        drawCentralCore(canvas, cx, cy, base)
        drawJarvisText(canvas, cx, cy, base)
        drawStatusLines(canvas, cx, cy, base)
        drawClockModule(canvas, cx, cy, base)

        if (appsModuleVisible) {
            drawAppsModule(canvas, cx, cy, base)
        } else if (moduleMenuVisible) {
            drawModuleMenu(canvas, cx, cy, base)
        } else {
            drawAddButton(canvas, cx, cy, base)
        }

        postInvalidateOnAnimation()
    }

    private fun drawAmbientGlow(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val pulse = if (speaking) {
            1f + voiceLevel * 0.55f
        } else {
            1f + 0.035f * sin(elapsed * 2.2f)
        }
        val radius = base * 0.72f * pulse

        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        glow.style = Paint.Style.FILL
        glow.color = cyan
        glow.alpha = if (speaking) 34 else 18
        glow.setShadowLayer(base * 0.34f, 0f, 0f, cyan)
        canvas.drawCircle(cx, cy, radius, glow)
        glow.clearShadowLayer()
    }

    private fun drawParticles(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        particles.forEachIndexed { index, p ->
            val vertical = p.y + elapsed * p.speed * 1.8f +
                    sin(elapsed * 0.35f + p.phase) * 0.018f
            val x = cx + p.x * base * 2.8f
            val y = cy + vertical * base * 2.8f

            if (y > cy - base * 2.4f && y < cy + base * 2.4f) {
                val alphaWave = 0.35f + 0.65f * (0.5f + 0.5f * sin(elapsed * 1.4f + p.phase))
                particlePaint.color = if (index % 8 == 0) cyanBright else cyan
                particlePaint.alpha = (65f * alphaWave).toInt()
                val size = p.size * (1f + voiceLevel * 1.5f)
                canvas.drawCircle(x, y, size, particlePaint)
            }
        }
    }

    private fun drawOuterHud(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val radius = base * 1.72f

        ringPaint.color = cyan
        ringPaint.alpha = 75
        ringPaint.strokeWidth = 1.1f
        canvas.drawCircle(cx, cy, radius, ringPaint)

        ringPaint.alpha = 42
        ringPaint.strokeWidth = 0.8f
        canvas.drawCircle(cx, cy, radius * 1.08f, ringPaint)

        ringPaint.alpha = 135
        ringPaint.strokeWidth = 2.2f
        val rotation = elapsed * 8f
        val outerRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(outerRect, rotation, 82f, false, ringPaint)
        canvas.drawArc(outerRect, rotation + 148f, 54f, false, ringPaint)
        canvas.drawArc(outerRect, rotation + 245f, 72f, false, ringPaint)
    }

    private fun drawMainRings(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val pulse = if (speaking) {
            1f + voiceLevel * 0.12f
        } else {
            1f + 0.018f * sin(elapsed * 2f)
        }

        val stateMultiplier = when (hudState) {
            JarvisHudState.READY -> 0.65f
            JarvisHudState.LISTENING -> 1.15f
            JarvisHudState.THINKING -> 2.20f
            JarvisHudState.SPEAKING -> 1.55f
            JarvisHudState.ERROR -> 3.00f
        }

        val rings = arrayOf(
            floatArrayOf(0.62f, 1.00f, 0.22f, 1f),
            floatArrayOf(0.77f, 0.92f, -0.38f, -1f),
            floatArrayOf(0.91f, 0.72f, 0.57f, 1f),
            floatArrayOf(1.07f, 0.56f, -0.22f, -1f),
            floatArrayOf(1.25f, 0.43f, 0.42f, 1f)
        )

        rings.forEachIndexed { index, r ->
            val rx = base * r[0] * pulse
            val ry = base * r[1] * pulse
            val tilt = r[2]
            val direction = r[3]
            val speed = (7f + index * 2.8f) * direction * stateMultiplier
            val rotation = elapsed * speed

            canvas.save()
            canvas.rotate(Math.toDegrees(tilt.toDouble()).toFloat(), cx, cy)
            canvas.scale(1f, 0.72f, cx, cy)

            val rect = RectF(cx - rx, cy - ry, cx + rx, cy + ry)

            ringPaint.color = if (index == 0) cyanBright else cyan
            ringPaint.alpha = when (index) {
                0 -> 170
                1 -> 125
                2 -> 95
                3 -> 70
                else -> 50
            }
            ringPaint.strokeWidth = when (index) {
                0 -> 2.6f
                1 -> 2.0f
                else -> 1.2f
            }

            canvas.drawArc(rect, rotation, 210f, false, ringPaint)
            canvas.drawArc(rect, rotation + 238f, 56f, false, ringPaint)
            canvas.drawArc(rect, rotation + 320f, 24f, false, ringPaint)

            canvas.restore()
        }
    }

    private fun drawRadialTicks(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val inner = base * 1.40f
        val outer = base * 1.57f

        for (i in 0 until 72) {
            val angle = Math.toRadians(i * 5.0)
            val dynamicOffset = elapsed * 1.2f
            val a = angle + Math.toRadians(dynamicOffset.toDouble())
            val length = if (i % 6 == 0) 15f else 7f

            val x1 = cx + cos(a).toFloat() * inner
            val y1 = cy + sin(a).toFloat() * inner * 0.72f
            val x2 = cx + cos(a).toFloat() * (inner + length)
            val y2 = cy + sin(a).toFloat() * (inner + length) * 0.72f

            tickPaint.color = if (i % 6 == 0) cyanBright else cyan
            tickPaint.alpha = if (i % 6 == 0) 145 else 65
            tickPaint.strokeWidth = if (i % 6 == 0) 2f else 1f

            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }
    }

    private fun drawOrbitElements(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val radius = base * 1.12f
        val orbitAngle = elapsed * 24f
        val angle = Math.toRadians(orbitAngle.toDouble())

        val x = cx + cos(angle).toFloat() * radius
        val y = cy + sin(angle).toFloat() * radius * 0.72f

        particlePaint.color = cyanBright
        particlePaint.alpha = 210
        canvas.drawCircle(x, y, 4.5f + voiceLevel * 4f, particlePaint)

        val oppositeAngle = Math.toRadians((-orbitAngle * 0.72f + 150f).toDouble())
        val x2 = cx + cos(oppositeAngle).toFloat() * radius * 0.86f
        val y2 = cy + sin(oppositeAngle).toFloat() * radius * 0.86f * 0.72f

        particlePaint.alpha = 130
        canvas.drawCircle(x2, y2, 2.8f, particlePaint)
    }

    private fun drawCentralCore(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val pulse = if (speaking) {
            1f + voiceLevel * 0.38f
        } else {
            1f + 0.045f * sin(elapsed * 2.5f)
        }
        val radius = base * 0.34f * pulse

        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        glow.color = cyan
        glow.style = Paint.Style.FILL
        glow.alpha = if (speaking) 55 else 28
        glow.setShadowLayer(base * 0.20f, 0f, 0f, cyan)
        canvas.drawCircle(cx, cy, radius * 1.32f, glow)
        glow.clearShadowLayer()

        corePaint.color = Color.rgb(13, 27, 32)
        corePaint.alpha = 235
        canvas.drawCircle(cx, cy, radius, corePaint)

        ringPaint.color = cyanBright
        ringPaint.alpha = 185
        ringPaint.strokeWidth = 1.8f
        canvas.drawCircle(cx, cy, radius * 1.08f, ringPaint)

        ringPaint.alpha = 115
        ringPaint.strokeWidth = 1f
        val innerRect = RectF(
            cx - radius * 0.82f, cy - radius * 0.82f,
            cx + radius * 0.82f, cy + radius * 0.82f
        )
        canvas.drawArc(innerRect, elapsed * 50f, 145f, false, ringPaint)
        canvas.drawArc(innerRect, elapsed * -38f + 180f, 95f, false, ringPaint)
    }

    private fun drawJarvisText(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val pulse = if (speaking) {
            1f + voiceLevel * 0.12f
        } else {
            1f + 0.025f * sin(elapsed * 2.4f)
        }

        textPaint.textSize = base * 0.205f * pulse
        textPaint.color = white
        textPaint.alpha = 245
        textPaint.setShadowLayer(base * 0.06f, 0f, 0f, cyan)

        val baseline = cy - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText("J.A.R.V.I.S.", cx, baseline, textPaint)

        textPaint.clearShadowLayer()
    }

    private fun drawStatusLines(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        linePaint.color = cyan
        linePaint.alpha = if (listening || speaking) 145 else 65
        linePaint.strokeWidth = 1f

        val left = cx - base * 1.48f
        val right = cx + base * 1.48f
        val y = cy + base * 1.62f

        canvas.drawLine(left, y, left + base * 0.42f, y, linePaint)
        canvas.drawLine(right - base * 0.42f, y, right, y, linePaint)

        val statusLabel = when (hudState) {
            JarvisHudState.READY -> "READY"
            JarvisHudState.LISTENING -> "LISTENING"
            JarvisHudState.THINKING -> "THINKING"
            JarvisHudState.SPEAKING -> "SPEAKING"
            JarvisHudState.ERROR -> "SYSTEM ERROR"
        }

        textPaint.textSize = base * 0.075f
        textPaint.color = cyanBright
        textPaint.alpha = 150
        canvas.drawText(statusLabel, cx, y + base * 0.08f, textPaint)
    }

    // ---------------- Clock module ----------------

    private fun drawClockModule(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        if (!clockVisible) return

        val time = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(java.util.Date())

        modulePaint.style = Paint.Style.FILL
        modulePaint.textAlign = Paint.Align.CENTER
        modulePaint.textSize = base * 0.16f
        modulePaint.color = Color.argb(155, 200, 220, 230)

        canvas.drawText(time, cx, cy - base * 1.85f, modulePaint)
    }

    // ---------------- Add button ----------------

    private fun addButtonPosition(cx: Float, cy: Float, base: Float): Pair<Float, Float> {
        return Pair(cx, cy + base * 1.95f)
    }

    private fun drawAddButton(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val (x, y) = addButtonPosition(cx, cy, base)

        modulePaint.style = Paint.Style.STROKE
        modulePaint.strokeWidth = 1.5f
        modulePaint.color = Color.argb(150, 120, 145, 160)

        canvas.drawCircle(x, y, base * 0.16f, modulePaint)

        modulePaint.style = Paint.Style.FILL
        modulePaint.textAlign = Paint.Align.CENTER
        modulePaint.textSize = base * 0.16f
        modulePaint.color = Color.argb(190, 150, 170, 185)

        canvas.drawText("+", x, y + base * 0.05f, modulePaint)
    }

    // ---------------- Module menu ----------------

    private fun moduleMenuPositions(cx: Float, cy: Float, base: Float): List<Triple<String, Float, Float>> {
        val labels = listOf("APPS", "SYS", "MAP", "3D", "CLK")
        val radius = base * 0.95f
        val step = 360f / labels.size

        return labels.mapIndexed { index, label ->
            val angle = Math.toRadians((-90 + index * step).toDouble())
            val x = cx + cos(angle).toFloat() * radius
            val y = cy + base * 1.95f + sin(angle).toFloat() * radius
            Triple(label, x, y)
        }
    }

    private fun drawModuleMenu(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val (centerX, centerY) = addButtonPosition(cx, cy, base)

        modulePaint.style = Paint.Style.STROKE
        modulePaint.strokeWidth = 1f
        modulePaint.color = Color.argb(85, 110, 130, 145)
        canvas.drawCircle(centerX, centerY, base * 0.95f, modulePaint)

        moduleMenuPositions(cx, cy, base).forEach { (label, x, y) ->
            modulePaint.style = Paint.Style.STROKE
            modulePaint.strokeWidth = 1f
            modulePaint.color = Color.argb(140, 90, 200, 220)
            canvas.drawCircle(x, y, base * 0.20f, modulePaint)

            modulePaint.style = Paint.Style.FILL
            modulePaint.textAlign = Paint.Align.CENTER
            modulePaint.textSize = base * 0.09f
            modulePaint.color = Color.argb(190, 150, 200, 215)
            canvas.drawText(label, x, y + base * 0.03f, modulePaint)
        }
    }

    // ---------------- Apps module ----------------

    private fun drawAppsModule(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val panelWidth = base * 3.2f
        val panelHeight = base * 1.55f
        val left = cx - panelWidth / 2f
        val top = cy + base * 0.55f

        modulePaint.style = Paint.Style.FILL
        modulePaint.color = Color.argb(150, 15, 25, 30)
        canvas.drawRoundRect(
            left, top, left + panelWidth, top + panelHeight,
            base * 0.08f, base * 0.08f, modulePaint
        )

        modulePaint.style = Paint.Style.STROKE
        modulePaint.strokeWidth = 1f
        modulePaint.color = Color.argb(120, 90, 200, 220)
        canvas.drawRoundRect(
            left, top, left + panelWidth, top + panelHeight,
            base * 0.08f, base * 0.08f, modulePaint
        )

        modulePaint.style = Paint.Style.FILL
        modulePaint.textAlign = Paint.Align.LEFT
        modulePaint.textSize = base * 0.09f
        modulePaint.color = Color.argb(180, 150, 220, 235)
        canvas.drawText("APPLICATIONS", left + base * 0.12f, top + base * 0.18f, modulePaint)

        modulePaint.textSize = base * 0.10f
        modulePaint.color = Color.argb(160, 190, 220, 230)

        installedAppNames.forEachIndexed { index, name ->
            val column = index % 2
            val row = index / 2
            val x = left + base * 0.12f + column * (panelWidth / 2f)
            val y = top + base * 0.42f + row * base * 0.32f
            canvas.drawText("\u25C7 $name", x, y, modulePaint)
        }

        modulePaint.textAlign = Paint.Align.CENTER
    }

    // ---------------- Touch handling ----------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) {
            return true
        }

        val cx = width / 2f
        val cy = height / 2f
        val base = minOf(width, height) * 0.31f

        if (appsModuleVisible) {
            val panelWidth = base * 3.2f
            val panelHeight = base * 1.55f
            val left = cx - panelWidth / 2f
            val top = cy + base * 0.55f

            installedAppNames.forEachIndexed { index, name ->
                val column = index % 2
                val row = index / 2
                val x = left + base * 0.12f + column * (panelWidth / 2f)
                val y = top + base * 0.42f + row * base * 0.32f

                if (kotlin.math.abs(event.x - x) < base * 0.6f &&
                    kotlin.math.abs(event.y - y) < base * 0.15f
                ) {
                    appClickListener?.invoke(name)
                    performClick()
                    return true
                }
            }

            appsModuleVisible = false
            invalidate()
            performClick()
            return true
        }

        if (moduleMenuVisible) {
            moduleMenuVisible = false
            invalidate()
            performClick()
            return true
        }

        val (addX, addY) = addButtonPosition(cx, cy, base)
        val distance = kotlin.math.sqrt(
            (event.x - addX) * (event.x - addX) + (event.y - addY) * (event.y - addY)
        )

        if (distance <= base * 0.22f) {
            moduleMenuVisible = true
            invalidate()
        }

        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
