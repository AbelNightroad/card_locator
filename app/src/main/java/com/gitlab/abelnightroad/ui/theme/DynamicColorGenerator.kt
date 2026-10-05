package com.gitlab.abelnightroad.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow

object DynamicColorGenerator {

    fun generateComplementaryColors(
        baseThemeColor: Color,
        count: Int,
        saturationRange: ClosedFloatingPointRange<Float> = 0.30f..0.80f,
        lightnessRange: ClosedFloatingPointRange<Float> = 0.55f..0.68f
    ): List<Color> {
        if (count <= 0) return emptyList()

        val hsl = toHsl(baseThemeColor)
        val baseHue = hsl[0]
        val saturation = hsl[1].coerceIn(saturationRange)
        val lightness = hsl[2].coerceIn(lightnessRange)

        val angleStep = 360f / (count + 1)
        return (1..count)
            .map { i ->
                val newHue = (baseHue + i * angleStep) % 360f
                fromHsl(newHue, saturation, lightness)
            }
            .shuffled()
    }

    fun onColor(background: Color): Color =
        if (relativeLuminance(background) > 0.45) Color(0xFF1B1B1F) else Color.White

    internal fun toHsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val lightness = (max + min) / 2f
        if (max == min) return floatArrayOf(0f, 0f, lightness)

        val delta = max - min
        val saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
        val hue = when (max) {
            r -> (g - b) / delta + (if (g < b) 6f else 0f)
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        } * 60f
        return floatArrayOf(hue, saturation, lightness)
    }

    private fun fromHsl(hue: Float, saturation: Float, lightness: Float): Color {
        val chroma = (1f - abs(2f * lightness - 1f)) * saturation
        val sector = (((hue % 360f) + 360f) % 360f) / 60f
        val x = chroma * (1f - abs(sector % 2f - 1f))
        val (r1, g1, b1) = when (sector.toInt()) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        val m = lightness - chroma / 2f
        return Color(r1 + m, g1 + m, b1 + m, 1f)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double =
            if (channel <= 0.03928f) (channel / 12.92f).toDouble()
            else ((channel + 0.055f) / 1.055f).toDouble().pow(2.4)

        return 0.2126 * linearize(color.red) +
                0.7152 * linearize(color.green) +
                0.0722 * linearize(color.blue)
    }
}
