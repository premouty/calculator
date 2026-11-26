package com.example

import com.example.data.preferences.AngleUnit
import com.example.domain.math.EvaluationResult
import com.example.domain.math.ExpressionEvaluator
import com.example.domain.math.MathErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BigNumberEngineTest {

    @Test
    fun `exact decimal addition avoids floating point drift`() {
        val result = ExpressionEvaluator.evaluate("0.1+0.2", AngleUnit.DEG)
        assertTrue(result is EvaluationResult.Success)
        val success = result as EvaluationResult.Success
        assertEquals(0, success.value.compareTo(BigDecimal("0.3")))
        assertEquals("0.3", success.formatted.plainText)
    }

    @Test
    fun `large integer multiplication preserves all digits`() {
        val result = ExpressionEvaluator.evaluate(
            "99999999999999999999×99999999999999999999",
            AngleUnit.DEG
        )
        assertTrue(result is EvaluationResult.Success)
        val success = result as EvaluationResult.Success
        assertEquals(
            "9999999999999999999800000000000000000001",
            success.formatted.plainText
        )
        assertTrue(success.formatted.isScientificNotation)
        assertEquals(40, success.formatted.digitCount)
    }

    @Test
    fun `large factorial computes 100 factorial accurately`() {
        val result = ExpressionEvaluator.evaluate("100!", AngleUnit.DEG)
        assertTrue(result is EvaluationResult.Success)
        val success = result as EvaluationResult.Success
        assertEquals(158, success.formatted.digitCount)
        assertTrue(success.formatted.plainText.startsWith("93326215443944152681699238856266700490715968264381"))
        assertTrue(success.formatted.plainText.endsWith("000000000000000000000000"))
    }

    @Test
    fun `large power computes 2 to 200 exactly`() {
        val result = ExpressionEvaluator.evaluate("2^200", AngleUnit.DEG)
        assertTrue(result is EvaluationResult.Success)
        val success = result as EvaluationResult.Success
        assertEquals(
            "1606938044258990275541962092341162602522202993782792835301376",
            success.formatted.plainText
        )
    }

    @Test
    fun `trigonometric functions snap exact degree angles`() {
        val sin180 = ExpressionEvaluator.evaluate("sin(180)", AngleUnit.DEG) as EvaluationResult.Success
        assertEquals(0, sin180.value.compareTo(BigDecimal.ZERO))

        val cos90 = ExpressionEvaluator.evaluate("cos(90)", AngleUnit.DEG) as EvaluationResult.Success
        assertEquals(0, cos90.value.compareTo(BigDecimal.ZERO))

        val sin30 = ExpressionEvaluator.evaluate("sin(30)", AngleUnit.DEG) as EvaluationResult.Success
        assertEquals("0.5", sin30.formatted.plainText.take(3))
    }

    @Test
    fun `implicit multiplication and additive percentage work intuitively`() {
        val implicit = ExpressionEvaluator.evaluate("3(4+5)", AngleUnit.DEG) as EvaluationResult.Success
        assertEquals("27", implicit.formatted.plainText)

        val percent = ExpressionEvaluator.evaluate("200+15%", AngleUnit.DEG) as EvaluationResult.Success
        assertEquals("230", percent.formatted.plainText)
    }

    @Test
    fun `division by zero and negative sqrt return domain errors`() {
        val divZero = ExpressionEvaluator.evaluate("42÷0", AngleUnit.DEG)
        assertTrue(divZero is EvaluationResult.Error && divZero.errorType == MathErrorType.DIVISION_BY_ZERO)

        val negSqrt = ExpressionEvaluator.evaluate("√(−9)", AngleUnit.DEG)
        assertTrue(negSqrt is EvaluationResult.Error && negSqrt.errorType == MathErrorType.DOMAIN_ERROR)
    }

    @Test
    fun `lowercase e followed by operator evaluates as Euler constant`() {
        val result = ExpressionEvaluator.evaluate("2e+3", AngleUnit.DEG)
        assertTrue(result is EvaluationResult.Success)
        val success = result as EvaluationResult.Success
        val expected = BigDecimal("8.4365636569")
        assertTrue((success.value - expected).abs() < BigDecimal("0.0001"))
    }
}
