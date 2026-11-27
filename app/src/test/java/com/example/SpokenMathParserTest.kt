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
    fun `parses all Ukrainian and Russian parenthesis phrases regardless of case and spacing`() {
        val uaVariants = listOf(
            "ДУЖКА ВІДКРИВАЄТЬСЯ 2 плюс 3 ДУЖКА ЗАКРИВАЄТЬСЯ",
            "Відкрити дужку 2 плюс 3 Закрити дужку",
            "Відкрита  дужка 2 плюс 3 Закрита  дужка",
            "Ліва дужка 2 плюс 3 Права дужка"
        )
        for (input in uaVariants) {
            val parsed = SpokenMathParser.parse(input, AngleUnit.DEG)
            assertNotNull("Expected non-null parse for: $input", parsed)
            assertEquals("(2+3)", parsed!!.rawExpression)
            assertEquals("5", parsed.previewResult)
        }

        val ruVariants = listOf(
            "Скобка открывается 4 плюс 6 Скобка закрывается",
            "ОТКРЫТЬ СКОБКУ 4 плюс 6 ЗАКРЫТЬ СКОБКУ"
        )
        for (input in ruVariants) {
            val parsed = SpokenMathParser.parse(input, AngleUnit.DEG)
            assertNotNull("Expected non-null parse for: $input", parsed)
            assertEquals("(4+6)", parsed!!.rawExpression)
            assertEquals("10", parsed.previewResult)
        }
    }

    @Test
    fun `handles whitespace and implicit multiplication before opening parenthesis`() {
        val spokenMult = SpokenMathParser.parse(
            "два відкрити дужку три плюс чотири закрити дужку",
            AngleUnit.DEG
        )
        assertNotNull(spokenMult)
        assertEquals("2×(3+4)", spokenMult!!.rawExpression)
        assertEquals("14", spokenMult.previewResult)

        val symbolicMult = SpokenMathParser.parse("2  ( 3 + 4 )", AngleUnit.DEG)
        assertNotNull(symbolicMult)
        assertEquals("2×(3+4)", symbolicMult!!.rawExpression)
        assertEquals("14", symbolicMult.previewResult)

        val partialParen = SpokenMathParser.parse("2(", AngleUnit.DEG)
        assertNotNull(partialParen)
        assertEquals("2×()", partialParen!!.rawExpression)

        val adjacentParens = SpokenMathParser.parse(
            "ліва дужка 2 плюс 3 права дужка дужка відкривається 4 плюс 5 дужка закривається",
            AngleUnit.DEG
        )
        assertNotNull(adjacentParens)
        assertEquals("(2+3)×(4+5)", adjacentParens!!.rawExpression)
        assertEquals("45", adjacentParens.previewResult)
    }

    @Test
    fun `returns null when speech has no math content`() {
        val parsed = SpokenMathParser.parse("привіт як справи сьогодні", AngleUnit.DEG)
        assertNull(parsed)
    }
}
