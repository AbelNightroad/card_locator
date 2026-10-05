package com.gitlab.abelnightroad.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicColorGeneratorTest {

    @Test
    fun `returns requested count of distinct colors`() {
        val colors = DynamicColorGenerator.generateComplementaryColors(Color(0xFF88C0D0), 8)
        assertEquals(8, colors.size)
        assertEquals(8, colors.map { it.toArgb() }.toSet().size)
    }

    @Test
    fun `non positive count returns empty list`() {
        assertTrue(DynamicColorGenerator.generateComplementaryColors(Color(0xFF88C0D0), 0).isEmpty())
        assertTrue(DynamicColorGenerator.generateComplementaryColors(Color(0xFF88C0D0), -3).isEmpty())
    }

    @Test
    fun `lightness stays inside readable band for any theme color`() {
        val bases = listOf(
            Color(0xFF1E1E2E),
            Color(0xFFECEFF4),
            Color(0xFF8839EF),
            Color(0xFF2E3440),
            Color(0xFFFFFFFF)
        )
        bases.forEach { base ->
            DynamicColorGenerator.generateComplementaryColors(base, 12).forEach { color ->
                val lightness = DynamicColorGenerator.toHsl(color)[2]
                assertTrue("L=$lightness outside band for base=$base", lightness in 0.54f..0.69f)
            }
        }
    }

    @Test
    fun `onColor picks readable text for light and dark backgrounds`() {
        assertEquals(Color(0xFF1B1B1F), DynamicColorGenerator.onColor(Color(0xFFB0C4D8)))
        assertEquals(Color.White, DynamicColorGenerator.onColor(Color(0xFF2A2A3A)))
    }
}
