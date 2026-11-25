package com.example

import com.example.data.preferences.AngleUnit
import com.example.domain.ocr.MathExpressionOcrParser
import com.example.domain.ocr.OcrBox
import com.example.domain.ocr.OcrLineCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MathExpressionOcrParserTest {

    @Test
    fun `normalizes standard printed arithmetic and strips equals sign`() {
        val result = MathExpressionOcrParser.extractBestExpression(
            lines = listOf(OcrLineCandidate("25 + 15 = ?")),
            reticleBox = null,
            angleUnit = AngleUnit.DEG
        )
        assertNotNull(result)
        assertEquals("25+15", result!!.rawExpression)
        assertEquals("40", result.previewResult)
    }

    @Test
    fun `normalizes multiplication x, division colon, and decimal commas`() {
        val result = MathExpressionOcrParser.extractBestExpression(
            lines = listOf(OcrLineCandidate("12 x 4 : 2 + 3,5")),
            reticleBox = null,
            angleUnit = AngleUnit.DEG
        )
        assertNotNull(result)
        assertEquals("12×4÷2+3.5", result!!.rawExpression)
        assertEquals("27.5", result.previewResult)
    }

    @Test
    fun `strips problem numbering prefix and extracts expression`() {
        val result = MathExpressionOcrParser.extractBestExpression(
            lines = listOf(OcrLineCandidate("1) 48 - 12 * 2 = 24")),
            reticleBox = null,
            angleUnit = AngleUnit.DEG
        )
        assertNotNull(result)
        assertEquals("48−12×2", result!!.rawExpression)
        assertEquals("24", result.previewResult)
    }

    @Test
    fun `supports scientific functions and sqrt from OCR`() {
        val result = MathExpressionOcrParser.extractBestExpression(
            lines = listOf(OcrLineCandidate("sin(30) + sqrt(16)")),
            reticleBox = null,
            angleUnit = AngleUnit.DEG
        )
        assertNotNull(result)
        assertEquals("sin(30)+√(16)", result!!.rawExpression)
        assertEquals("4.5", result.previewResult)
    }

    @Test
    fun `prioritizes lines inside target reticle over outside noise`() {
        val reticle = OcrBox(left = 50f, top = 200f, right = 450f, bottom = 350f)
        val outsidePageNum = OcrLineCandidate("999 + 1", OcrBox(20f, 20f, 120f, 60f))
        val insideProblem = OcrLineCandidate("7 * 8", OcrBox(120f, 240f, 300f, 300f))

        val result = MathExpressionOcrParser.extractBestExpression(
            lines = listOf(outsidePageNum, insideProblem),
            reticleBox = reticle,
            angleUnit = AngleUnit.DEG
        )
        assertNotNull(result)
        assertEquals("7×8", result!!.rawExpression)
        assertEquals("56", result.previewResult)
    }

    @Test
    fun `ignores non-mathematical text`() {
        val result = MathExpressionOcrParser.extractBestExpression(
            lines = listOf(OcrLineCandidate("Розв'яжіть задачу на сторінці")),
            reticleBox = null,
            angleUnit = AngleUnit.DEG
        )
        assertNull(result)
    }

    @Test
    fun `extractAllExpressions returns multiple expressions in reading order and skips stray numbers`() {
        val lines = listOf(
            OcrLineCandidate("Сторінка 142", OcrBox(300f, 20f, 450f, 50f)),
            OcrLineCandidate("2) 48 - 12 * 2", OcrBox(40f, 220f, 260f, 260f)),
            OcrLineCandidate("1) 25 + 15 = ?", OcrBox(40f, 110f, 250f, 150f)),
            OcrLineCandidate("sin(30) + sqrt(16)", OcrBox(40f, 330f, 300f, 370f))
        )

        val results = MathExpressionOcrParser.extractAllExpressions(
            lines = lines,
            angleUnit = AngleUnit.DEG
        )

        assertEquals(3, results.size)
        assertEquals("25+15", results[0].rawExpression)
        assertEquals("40", results[0].previewResult)
        assertEquals("48−12×2", results[1].rawExpression)
        assertEquals("24", results[1].previewResult)
        assertEquals("sin(30)+√(16)", results[2].rawExpression)
        assertEquals("4.5", results[2].previewResult)
    }

    @Test
    fun `extractAllExpressions splits two numbered expressions on one line`() {
        val lines = listOf(
            OcrLineCandidate("1) 12 + 5   2) 8 * 4", OcrBox(20f, 100f, 420f, 140f))
        )

        val results = MathExpressionOcrParser.extractAllExpressions(
            lines = lines,
            angleUnit = AngleUnit.DEG
        )

        assertEquals(2, results.size)
        assertEquals("12+5", results[0].rawExpression)
        assertEquals("8×4", results[1].rawExpression)
    }

    @Test
    fun `normalizeAiExpression handles fractions, functions, and strips right-hand answer`() {
        val box = OcrBox(10f, 20f, 200f, 60f)
        val result = MathExpressionOcrParser.normalizeAiExpression(
            rawExpr = "(12 + 8) / 4 + sqrt(9) = 8",
            angleUnit = AngleUnit.DEG,
            boundingBox = box
        )

        assertNotNull(result)
        assertEquals("(12+8)÷4+√(9)", result!!.rawExpression)
        assertEquals("8", result.previewResult)
        assertEquals(box, result.boundingBox)
    }

    @Test
    fun `normalizeAiExpression handles standalone numbers including single digits`() {
        val result = MathExpressionOcrParser.normalizeAiExpression(
            rawExpr = "42",
            angleUnit = AngleUnit.DEG
        )

        assertNotNull(result)
        assertEquals("42", result!!.rawExpression)
        assertEquals("", result.previewResult)
    }

    @Test
    fun `normalizeAiExpression handles LaTeX symbols and invalid expressions`() {
        val latex = MathExpressionOcrParser.normalizeAiExpression(
            rawExpr = "2 \\times \\sqrt{25} - \\pi",
            angleUnit = AngleUnit.DEG
        )
        assertNotNull(latex)
        assertEquals("2×√(25)−π", latex!!.rawExpression)

        val invalid = MathExpressionOcrParser.normalizeAiExpression(
            rawExpr = "hello world",
            angleUnit = AngleUnit.DEG
        )
        assertNull(invalid)
    }

    @Test
    fun `extractAllExpressions merges horizontal split fragments and preserves parenthesized divisor`() {
        val lines = listOf(
            OcrLineCandidate("25 +", OcrBox(20f, 100f, 100f, 140f)),
            OcrLineCandidate("15", OcrBox(112f, 100f, 160f, 140f)),
            OcrLineCandidate("(12 + 8) / 4", OcrBox(20f, 200f, 240f, 240f))
        )

        val results = MathExpressionOcrParser.extractAllExpressions(lines, AngleUnit.DEG)
        assertEquals(2, results.size)
        assertEquals("25+15", results[0].rawExpression)
        assertEquals("40", results[0].previewResult)
        assertEquals("(12+8)÷4", results[1].rawExpression)
        assertEquals("5", results[1].previewResult)
    }

    @Test
    fun `extractAllExpressions reconstructs spatial superscripts and vertical stacked fractions`() {
        val lines = listOf(
            // 2^3 where 3 is raised and smaller to the upper-right of 2
            OcrLineCandidate("2", OcrBox(20f, 100f, 50f, 150f)),
            OcrLineCandidate("3", OcrBox(54f, 92f, 74f, 122f)),
            // Stacked fraction (12 + 8) / (4)
            OcrLineCandidate("12 + 8", OcrBox(40f, 220f, 160f, 255f)),
            OcrLineCandidate("------", OcrBox(35f, 260f, 165f, 268f)),
            OcrLineCandidate("4", OcrBox(85f, 272f, 115f, 308f))
        )

        val results = MathExpressionOcrParser.extractAllExpressions(lines, AngleUnit.DEG)
        assertEquals(2, results.size)
        assertEquals("2^3", results[0].rawExpression)
        assertEquals("8", results[0].previewResult)
        assertEquals("(12+8)÷(4)", results[1].rawExpression)
        assertEquals("5", results[1].previewResult)
    }

    @Test
    fun `extractAllExpressions recovers ML Kit typos, Unicode superscripts, and Ukrainian aliases`() {
        val lines = listOf(
            OcrLineCandidate("V(16) + 2⁴", OcrBox(20f, 80f, 200f, 120f)),
            OcrLineCandidate("lg(100) + tg(45)", OcrBox(20f, 160f, 240f, 200f))
        )

        val results = MathExpressionOcrParser.extractAllExpressions(lines, AngleUnit.DEG)
        assertEquals(2, results.size)
        assertEquals("√(16)+2^4", results[0].rawExpression)
        assertEquals("20", results[0].previewResult)
        assertEquals("log(100)+tan(45)", results[1].rawExpression)
        assertEquals("3", results[1].previewResult)
    }
}
