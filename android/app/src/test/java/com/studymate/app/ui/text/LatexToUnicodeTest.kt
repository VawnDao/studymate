package com.studymate.app.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexToUnicodeTest {

    @Test
    fun `chemical equation uses subscripts and arrow`() {
        assertEquals(
            "Phương trình: 6CO₂ + 6H₂O → C₆H₁₂O₆ + 6O₂ nhé",
            LatexToUnicode.convert("Phương trình: \$6CO_2 + 6H_2O \\rightarrow C_6H_{12}O_6 + 6O_2\$ nhé"),
        )
    }

    @Test
    fun `superscripts, fractions, roots and symbols`() {
        assertEquals("x² + y² = r²", LatexToUnicode.convert("\$x^2 + y^2 = r^2\$"))
        assertEquals("a/b", LatexToUnicode.convert("\$\\frac{a}{b}\$"))
        assertEquals("√x", LatexToUnicode.convert("\$\\sqrt{x}\$"))
        assertEquals("2 × π · r", LatexToUnicode.convert("\$2 \\times \\pi \\cdot r\$"))
        assertEquals("Δ ≥ 0", LatexToUnicode.convert("\$\\Delta \\geq 0\$"))
    }

    @Test
    fun `unmappable scripts stay readable`() {
        assertEquals("x^(y)", LatexToUnicode.convert("\$x^{y}\$"))
    }

    @Test
    fun `text without math is unchanged`() {
        val plain = "Pha sáng diễn ra ở màng thylakoid (giá 20$)."
        assertEquals(plain, LatexToUnicode.convert(plain))
        assertFalse(LatexToUnicode.containsMath(plain))
        assertTrue(LatexToUnicode.containsMath("\$x^2\$"))
    }
}
