package com.example

import com.example.data.preferences.AngleUnit
import com.example.domain.voice.SpokenMathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SpokenMathParserTest {

    @Test
    fun `parses Ukrainian spoken arithmetic with compound numbers`() {
        val parsed = SpokenMathParser.parse(
            "скільки буде двісті тридцять п'ять плюс п'ятнадцять помножити на два",
            AngleUnit.DEG
        )
        assertNotNull(parsed)
        assertEquals("235+15×2", parsed!!.rawExpression)
        assertEquals("265", parsed.previewResult)
    }

    @Test
    fun `parses powers, squares, cubes, and ordinal exponents`() {
        val pow = SpokenMathParser.parse("два в степені десять", AngleUnit.DEG)
        assertNotNull(pow)
        assertEquals("2^10", pow!!.rawExpression)
        assertEquals("1 024", pow.previewResult)

        val square = SpokenMathParser.parse("п'ять в квадраті мінус три в кубі", AngleUnit.DEG)
        assertNotNull(square)
        assertEquals("5^2−3^3", square!!.rawExpression)
        assertEquals("-2", square.previewResult)

        val ordinal = SpokenMathParser.parse("два у третьому степені", AngleUnit.DEG)
        assertNotNull(ordinal)
        assertEquals("2^3", ordinal!!.rawExpression)
        assertEquals("8", ordinal.previewResult)
    }

    @Test
    fun `parses roots, trig functions, decimals, and parentheses`() {
        val root = SpokenMathParser.parse("корінь з шістнадцяти плюс синус тридцяти", AngleUnit.DEG)
        assertNotNull(root)
        assertEquals("√(16+sin(30))", root!!.rawExpression)

        val paren = SpokenMathParser.parse(
            "відкрити дужку два кома п'ять плюс один кома п'ять закрити дужку помножити на чотири",
            AngleUnit.DEG
        )
        assertNotNull(paren)
        assertEquals("(2.5+1.5)×4", paren!!.rawExpression)
        assertEquals("16", paren.previewResult)
    }

    @Test
    fun `returns null when speech has no math content`() {
        val parsed = SpokenMathParser.parse("привіт як справи сьогодні", AngleUnit.DEG)
        assertNull(parsed)
    }
}
