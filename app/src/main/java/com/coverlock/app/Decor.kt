package com.coverlock.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Full-screen black-and-white misty mountain background. Drawn once, then cached. */
class BackdropView(context: Context) : View(context) {
    private var cache: Bitmap? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cache = if (w > 0 && h > 0) build(w, h) else null
    }

    override fun onDraw(canvas: Canvas) {
        cache?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    private fun build(w: Int, h: Int): Bitmap {
        val d = resources.displayMetrics.density
        val wf = w.toFloat()
        val hf = h.toFloat()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // dark misty sky
        c.drawRect(0f, 0f, wf, hf, Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, hf,
                intArrayOf(Color.rgb(54, 56, 58), Color.rgb(26, 27, 28), Ui.BLACK),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        })
        // soft light behind the peaks
        c.drawRect(0f, 0f, wf, hf, Paint().apply {
            shader = RadialGradient(
                wf * 0.62f, hf * 0.30f, wf * 1.05f,
                intArrayOf(Ui.white(46), Ui.white(0)),
                null,
                Shader.TileMode.CLAMP
            )
        })

        // layered ridges, far (pale, hazy) to near (dark), with fog drifting between them
        val layers = listOf(
            floatArrayOf(0.34f, 0.10f, 1.3f, 0.0f, 92f),
            floatArrayOf(0.46f, 0.12f, 1.9f, 2.1f, 70f),
            floatArrayOf(0.58f, 0.13f, 2.6f, 4.3f, 46f),
            floatArrayOf(0.72f, 0.10f, 3.4f, 1.2f, 26f)
        )
        for ((i, L) in layers.withIndex()) {
            val base = hf * L[0]
            val amp = hf * L[1]
            val path = Path()
            path.moveTo(0f, hf)
            var x = 0f
            while (x <= wf + 6f) {
                val u = x / wf
                val ridge = abs(sin(u * L[2] * 3.1f + L[3])) * 0.65f +
                    0.25f * sin(u * L[2] * 9f + L[3] * 2f) + 0.10f * sin(u * 31f + i)
                path.lineTo(x, base - amp * ridge)
                x += 6f
            }
            path.lineTo(wf, hf)
            path.close()
            val tone = L[4].toInt()
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, base - amp, 0f, hf,
                    intArrayOf(Color.rgb(tone + 20, tone + 21, tone + 23), Color.rgb(tone / 3, tone / 3, tone / 3 + 1)),
                    null,
                    Shader.TileMode.CLAMP
                )
            })
            // fog band hugging the foot of this layer
            c.drawRect(0f, base - amp * 0.25f, wf, base + amp * 1.6f, Paint().apply {
                shader = LinearGradient(
                    0f, base - amp * 0.25f, 0f, base + amp * 1.6f,
                    intArrayOf(Ui.white(0), Ui.white(34 - i * 6), Ui.white(0)),
                    null,
                    Shader.TileMode.CLAMP
                )
            })
        }

        // a little halftone cluster in the top right corner
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.white(60) }
        val step = 9f * d
        for (row in 0 until 7) {
            for (col in 0 until 7) {
                val fall = 1f - (row + col) / 12f
                val r = step * 0.42f * fall
                if (r > 0.6f) c.drawCircle(wf - step * (col + 1.2f) + (row % 2) * step / 2f, step * (row + 2f) * 0.87f, r, dot)
            }
        }

        // fine film grain
        val rnd = Random(7)
        val grain = Paint().apply { color = Ui.white(14) }
        repeat((w * h) / 900) {
            c.drawCircle(rnd.nextFloat() * wf, rnd.nextFloat() * hf, 0.8f * d, grain)
        }

        // thin chrome "+" marks
        val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ui.white(120); strokeWidth = 1f * d; style = Paint.Style.STROKE
        }
        for (s in listOf(floatArrayOf(0.90f, 0.24f), floatArrayOf(0.07f, 0.50f), floatArrayOf(0.88f, 0.80f))) {
            val cx = wf * s[0]; val cy = hf * s[1]; val r = 7f * d
            c.drawLine(cx - r, cy, cx + r, cy, mark); c.drawLine(cx, cy - r, cx, cy + r, mark)
        }
        return bmp
    }
}

/** A small chrome sparkle, used in headers. */
class SparkleView(context: Context) : View(context) {
    override fun onDraw(canvas: Canvas) {
        val r = min(width, height) / 2f
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawPath(Ui.sparkle(cx, cy, r), Ui.chrome(cx, cy, r))
    }
}

/**
 * Big illustration at the top of each setup page: an outlined step number next to a glass tile
 * with a simple glyph, an orbit ring and a little sparkle flying around it.
 * kind: 1 = listen, 2 = battery / power, 3 = wallpaper, 4 = done.
 */
class HeroView(context: Context, private val kind: Int) : View(context) {
    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var animator: ValueAnimator? = null

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2.4f * d
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val numLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Ui.HEAVY
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 1.6f * d
    }
    private val numFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Ui.HEAVY
        color = Ui.white(28)
    }
    private val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.white(34) }
    private val glassEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Ui.white(140)
        strokeWidth = 1.5f * d
    }
    private val orbitLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Ui.white(150)
        strokeWidth = 1.3f * d
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 7000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // outlined step number
        val label = when (kind) {
            1 -> "01"
            2 -> "02"
            3 -> "03"
            else -> "GO"
        }
        numLine.textSize = h * 0.56f
        numFill.textSize = h * 0.56f
        val base = h * 0.80f
        c.drawText(label, 0f, base, numFill)
        c.drawText(label, 0f, base, numLine)

        // glass tile
        val tile = RectF(w * 0.54f, h * 0.08f, w, h * 0.92f)
        val corner = 28f * d
        c.drawRoundRect(tile, corner, corner, glass)
        c.drawRoundRect(tile, corner, corner, glassEdge)

        val cx = tile.centerX()
        val cy = tile.centerY()
        val g = min(tile.height() * 0.24f, tile.width() * 0.2f)

        // orbit ring
        val orbit = RectF(cx - g * 1.9f, cy - g * 0.75f, cx + g * 1.9f, cy + g * 0.75f)
        c.save()
        c.rotate(-20f, cx, cy)
        c.drawOval(orbit, orbitLine)
        c.restore()

        // glyph
        when (kind) {
            1 -> equalizer(c, cx, cy, g)
            2 -> power(c, cx, cy, g)
            3 -> picture(c, cx, cy, g)
            else -> c.drawPath(Ui.sparkle(cx, cy, g * 1.15f), Ui.chrome(cx, cy, g * 1.15f))
        }

        // little sparkle flying along the orbit
        val th = phase * 2f * PI.toFloat()
        val ox = (orbit.width() / 2f) * cos(th)
        val oy = (orbit.height() / 2f) * sin(th)
        val phi = (-20.0 * PI / 180.0).toFloat()
        val sx = cx + ox * cos(phi) - oy * sin(phi)
        val sy = cy + ox * sin(phi) + oy * cos(phi)
        val sr = g * 0.3f
        c.drawPath(Ui.sparkle(sx, sy, sr), Ui.chrome(sx, sy, sr))
    }

    private fun equalizer(c: Canvas, cx: Float, cy: Float, g: Float) {
        val bw = g * 0.26f
        val gap = g * 0.2f
        val total = 5 * bw + 4 * gap
        var x = cx - total / 2f
        val hs = floatArrayOf(0.7f, 1.2f, 0.9f, 1.5f, 0.6f)
        for (i in 0 until 5) {
            val k = 0.65f + 0.35f * sin(phase * 2f * PI.toFloat() * 2f + i * 1.3f)
            val bh = g * hs[i] * k
            c.drawRoundRect(RectF(x, cy - bh, x + bw, cy + bh), bw / 2f, bw / 2f, fill)
            x += bw + gap
        }
    }

    private fun power(c: Canvas, cx: Float, cy: Float, g: Float) {
        c.drawArc(RectF(cx - g, cy - g, cx + g, cy + g), -60f, 300f, false, stroke)
        c.drawLine(cx, cy - g * 1.15f, cx, cy - g * 0.1f, stroke)
    }

    private fun picture(c: Canvas, cx: Float, cy: Float, g: Float) {
        c.drawRoundRect(RectF(cx - g * 1.2f, cy - g * 0.9f, cx + g * 1.2f, cy + g * 0.9f), g * 0.25f, g * 0.25f, stroke)
        c.drawCircle(cx + g * 0.5f, cy - g * 0.3f, g * 0.2f, fill)
        val p = android.graphics.Path()
        p.moveTo(cx - g * 1.1f, cy + g * 0.7f)
        p.lineTo(cx - g * 0.3f, cy - g * 0.1f)
        p.lineTo(cx + g * 0.2f, cy + g * 0.4f)
        p.lineTo(cx + g * 0.6f, cy + g * 0.1f)
        p.lineTo(cx + g * 1.1f, cy + g * 0.7f)
        c.drawPath(p, stroke)
    }
}
