package com.coverlock.app

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface

/** Colors, fonts and shapes shared by every screen. Look: black and white, misty mountains, serif italic titles, mono labels. */
object Ui {
    val ACCENT = Color.rgb(18, 18, 20)      // graphite: chips and small buttons on paper cards
    val BLACK = Color.rgb(5, 5, 6)
    val INK = Color.rgb(12, 12, 13)
    val PAPER = Color.rgb(232, 231, 227)    // warm off-white
    val ICE = Color.rgb(206, 207, 209)      // silver

    val HEAVY: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    val SERIF: Typeface = Typeface.create("serif", Typeface.ITALIC)
    val LIGHT: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    val MONO: Typeface = Typeface.create("monospace", Typeface.BOLD)

    fun white(alpha: Int): Int = Color.argb(alpha, 255, 255, 255)
    fun black(alpha: Int): Int = Color.argb(alpha, 0, 0, 0)

    /** Concave four-point star: the "chrome sparkle". */
    fun sparkle(cx: Float, cy: Float, r: Float): Path {
        val p = Path()
        p.moveTo(cx, cy - r)
        p.cubicTo(cx + r * 0.08f, cy - r * 0.30f, cx + r * 0.30f, cy - r * 0.08f, cx + r, cy)
        p.cubicTo(cx + r * 0.30f, cy + r * 0.08f, cx + r * 0.08f, cy + r * 0.30f, cx, cy + r)
        p.cubicTo(cx - r * 0.08f, cy + r * 0.30f, cx - r * 0.30f, cy + r * 0.08f, cx - r, cy)
        p.cubicTo(cx - r * 0.30f, cy - r * 0.08f, cx - r * 0.08f, cy - r * 0.30f, cx, cy - r)
        p.close()
        return p
    }

    /** Silvery white-to-ice gradient for sparkles. */
    fun chrome(cx: Float, cy: Float, r: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cx - r, cy - r, cx + r, cy + r,
            intArrayOf(Color.WHITE, ICE, Color.rgb(150, 151, 155), Color.WHITE),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
    }
}
