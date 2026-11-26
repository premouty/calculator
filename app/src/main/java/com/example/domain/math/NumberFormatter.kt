package com.example.domain.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class FormattedNumber(
    val displayText: String,
    val plainText: String,
    val isScientificNotation: Boolean,
    val digitCount: Int
)

object NumberFormatter {
    private val LARGE_THRESHOLD = BigDecimal("1E15")
    private val SMALL_THRESHOLD = BigDecimal("1E-8")
    private val SCI_MC = MathContext(11, RoundingMode.HALF_UP)
    private val NORMAL_ROUND_MC = MathContext(15, RoundingMode.HALF_UP)
    private val PLAIN_FLOAT_MC = MathContext(32, RoundingMode.HALF_UP)

    private val SUPERSCRIPT_DIGITS = mapOf(
        '0' to '⁰',
        '1' to '¹',
        '2' to '²',
        '3' to '³',
        '4' to '⁴',
        '5' to '⁵',
        '6' to '⁶',
        '7' to '⁷',
        '8' to '⁸',
        '9' to '⁹',
        '-' to '⁻'
    )

    fun formatResult(value: BigDecimal): FormattedNumber {
        val cleaned = BigNumberMath.cleanZero(value)
        if (cleaned.compareTo(BigDecimal.ZERO) == 0) {
            return FormattedNumber(
                displayText = "0",
                plainText = "0",
                isScientificNotation = false,
                digitCount = 1
            )
        }

        val absVal = cleaned.abs()
        val isExactInteger = cleaned.scale() <= 0

        val plainExact = if (isExactInteger) {
            cleaned.toBigInteger().toString()
        } else {
            val roundedPlain = BigNumberMath.cleanZero(cleaned.round(PLAIN_FLOAT_MC))
            roundedPlain.toPlainString()
        }
        val digitCount = plainExact.count { it.isDigit() }

        val needsScientific = absVal >= LARGE_THRESHOLD || absVal < SMALL_THRESHOLD
        if (needsScientific) {
            val sci = BigNumberMath.cleanZero(cleaned.round(SCI_MC))
            val exp = sci.precision() - sci.scale() - 1
            val mantissa = BigNumberMath.cleanZero(sci.movePointLeft(exp))
            val mantissaStr = formatGroupedNumberString(mantissa.toPlainString())
            val expSup = toSuperscript(exp.toString())
            return FormattedNumber(
                displayText = "$mantissaStr × 10$expSup",
                plainText = plainExact,
                isScientificNotation = true,
                digitCount = digitCount
            )
        }

        val displayDecimal = if (isExactInteger) {
            cleaned
        } else {
            BigNumberMath.cleanZero(cleaned.round(NORMAL_ROUND_MC))
        }
        val displayStr = formatGroupedNumberString(displayDecimal.toPlainString())
        return FormattedNumber(
            displayText = displayStr,
            plainText = plainExact,
            isScientificNotation = false,
            digitCount = digitCount
        )
    }

    /**
     * Inserts spaces every 3 digits in integer parts of numbers inside an expression
     * for clean, legible display (e.g., "1234567+8900" -> "1 234 567+8 900").
     */
    fun formatExpressionForDisplay(rawExpression: String): String {
        if (rawExpression.isEmpty()) return ""
        val sb = StringBuilder(rawExpression.length + 8)
        var i = 0
        while (i < rawExpression.length) {
            val ch = rawExpression[i]
            if (ch.isDigit() || ch == '.') {
                val start = i
                var hasDot = false
                while (i < rawExpression.length) {
                    val c = rawExpression[i]
                    if (c.isDigit()) {
                        i++
                    } else if (c == '.' && !hasDot) {
                        hasDot = true
                        i++
                    } else if (c == 'E' && i > start && i + 1 < rawExpression.length) {
                        // Scientific literal e.g. 1.5E10 or 1.5E-10
                        val next = rawExpression[i + 1]
                        if (next.isDigit() || ((next == '-' || next == '+') && i + 2 < rawExpression.length && rawExpression[i + 2].isDigit())) {
                            sb.append(formatGroupedNumberString(rawExpression.substring(start, i)))
                            sb.append(rawExpression[i]) // 'E'
                            i++
                            if (i < rawExpression.length && (rawExpression[i] == '-' || rawExpression[i] == '+')) {
                                sb.append(rawExpression[i])
                                i++
                            }
                            while (i < rawExpression.length && rawExpression[i].isDigit()) {
                                sb.append(rawExpression[i])
                                i++
                            }
                            break
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
                if (i > start) {
                    val token = rawExpression.substring(start, i)
                    if (!token.contains('E')) {
                        sb.append(formatGroupedNumberString(token))
                    }
                }
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }

    fun formatGroupedNumberString(plain: String): String {
        if (plain.isEmpty()) return plain
        val sign = if (plain.startsWith("-")) "-" else ""
        val unsigned = if (sign.isNotEmpty()) plain.substring(1) else plain
        val dotIndex = unsigned.indexOf('.')
        val intPart = if (dotIndex >= 0) unsigned.substring(0, dotIndex) else unsigned
        val fracPart = if (dotIndex >= 0) unsigned.substring(dotIndex) else ""

        if (intPart.length <= 3) return "$sign$intPart$fracPart"

        val grouped = StringBuilder()
        var count = 0
        for (idx in intPart.length - 1 downTo 0) {
            grouped.append(intPart[idx])
            count++
            if (count % 3 == 0 && idx > 0) {
                grouped.append('\u2009') // thin space for crisp typographic grouping
            }
        }
        return "$sign${grouped.reverse()}$fracPart"
    }

    private fun toSuperscript(text: String): String {
        return buildString {
            for (c in text) {
                append(SUPERSCRIPT_DIGITS[c] ?: c)
            }
        }
    }
}
