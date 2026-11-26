package com.example.domain.math

import com.example.data.preferences.AngleUnit
import java.math.BigDecimal

sealed interface EvaluationResult {
    data object Idle : EvaluationResult
    data class Success(
        val value: BigDecimal,
        val formatted: FormattedNumber
    ) : EvaluationResult
    data class Error(val errorType: MathErrorType) : EvaluationResult
}

object ExpressionEvaluator {

    private sealed interface Token {
        data class NumberLit(val value: BigDecimal) : Token
        data class Constant(val symbol: Char) : Token
        data class Func(val name: String) : Token // e.g., "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt"
        data class Op(val symbol: Char) : Token // +, -, *, /, ^
        data object Percent : Token
        data object Factorial : Token
        data object LParen : Token
        data object RParen : Token
    }

    fun evaluate(rawExpression: String, angleUnit: AngleUnit): EvaluationResult {
        val trimmed = rawExpression.trim()
        if (trimmed.isEmpty()) return EvaluationResult.Idle

        return try {
            val tokens = tokenize(trimmed)
            if (tokens.isEmpty()) return EvaluationResult.Idle
            val normalizedTokens = insertImplicitMultiplication(autoCloseParens(tokens))

            // If expression is just a single literal number (e.g. "42"), don't show redundant preview
            if (normalizedTokens.size == 1 && normalizedTokens[0] is Token.NumberLit) {
                return EvaluationResult.Idle
            }

            val parser = Parser(normalizedTokens, angleUnit)
            val result = parser.parseExpression()
            if (parser.hasMore()) {
                return EvaluationResult.Error(MathErrorType.SYNTAX_ERROR)
            }
            val formatted = NumberFormatter.formatResult(result)
            EvaluationResult.Success(result, formatted)
        } catch (e: MathEvaluationException) {
            EvaluationResult.Error(e.errorType)
        } catch (_: ArithmeticException) {
            EvaluationResult.Error(MathErrorType.OVERFLOW)
        } catch (_: Exception) {
            EvaluationResult.Error(MathErrorType.SYNTAX_ERROR)
        }
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val ch = expr[i]
            when {
                ch.isWhitespace() || ch == '\u2009' -> {
                    i++
                }
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var hasDot = false
                    while (i < expr.length) {
                        val c = expr[i]
                        if (c.isDigit()) {
                            i++
                        } else if (c == '.' && !hasDot) {
                            hasDot = true
                            i++
                        } else if (c == 'E' && i + 1 < expr.length) {
                            val next = expr[i + 1]
                            if (next.isDigit() || ((next == '+' || next == '-') && i + 2 < expr.length && expr[i + 2].isDigit())) {
                                i++ // consume E
                                if (expr[i] == '+' || expr[i] == '-') i++
                                while (i < expr.length && expr[i].isDigit()) i++
                            }
                            break
                        } else {
                            break
                        }
                    }
                    val numStr = expr.substring(start, i)
                    if (numStr == ".") throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
                    val bd = BigDecimal(numStr)
                    tokens.add(Token.NumberLit(bd))
                }
                ch == 'π' -> {
                    tokens.add(Token.Constant('π'))
                    i++
                }
                ch == 'e' -> {
                    tokens.add(Token.Constant('e'))
                    i++
                }
                ch == '√' -> {
                    tokens.add(Token.Func("sqrt"))
                    i++
                }
                ch == '(' -> {
                    tokens.add(Token.LParen)
                    i++
                }
                ch == ')' -> {
                    tokens.add(Token.RParen)
                    i++
                }
                ch == '%' -> {
                    tokens.add(Token.Percent)
                    i++
                }
                ch == '!' -> {
                    tokens.add(Token.Factorial)
                    i++
                }
                ch == '+' -> {
                    tokens.add(Token.Op('+'))
                    i++
                }
                ch == '−' || ch == '-' -> {
                    tokens.add(Token.Op('-'))
                    i++
                }
                ch == '×' || ch == '*' -> {
                    tokens.add(Token.Op('*'))
                    i++
                }
                ch == '÷' || ch == '/' -> {
                    tokens.add(Token.Op('/'))
                    i++
                }
                ch == '^' -> {
                    tokens.add(Token.Op('^'))
                    i++
                }
                ch.isLetter() -> {
                    val start = i
                    while (i < expr.length && (expr[i].isLetter() || expr[i] == '⁻' || expr[i] == '¹')) {
                        i++
                    }
                    val name = when (val raw = expr.substring(start, i)) {
                        "sin⁻¹", "asin" -> "asin"
                        "cos⁻¹", "acos" -> "acos"
                        "tan⁻¹", "atan" -> "atan"
                        "sin", "cos", "tan", "ln", "log", "sqrt" -> raw
                        else -> throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
                    }
                    tokens.add(Token.Func(name))
                }
                else -> throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
            }
        }
        return tokens
    }

    private fun autoCloseParens(tokens: List<Token>): List<Token> {
        var openCount = 0
        for (t in tokens) {
            if (t is Token.LParen) openCount++
            else if (t is Token.RParen && openCount > 0) openCount--
        }
        if (openCount <= 0) return tokens
        val out = tokens.toMutableList()
        repeat(openCount) { out.add(Token.RParen) }
        return out
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.size < 2) return tokens
        val out = mutableListOf<Token>()
        for (i in tokens.indices) {
            val curr = tokens[i]
            if (i > 0) {
                val prev = tokens[i - 1]
                val prevEndsValue = prev is Token.NumberLit ||
                    prev is Token.Constant ||
                    prev is Token.RParen ||
                    prev is Token.Percent ||
                    prev is Token.Factorial

                val currStartsValue = curr is Token.NumberLit ||
                    curr is Token.Constant ||
                    curr is Token.Func ||
                    curr is Token.LParen

                if (prevEndsValue && currStartsValue) {
                    out.add(Token.Op('*'))
                }
            }
            out.add(curr)
        }
        return out
    }

    private class Parser(
        private val tokens: List<Token>,
        private val angleUnit: AngleUnit
    ) {
        private var pos = 0

        fun hasMore(): Boolean = pos < tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): BigDecimal {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                    consume()
                    val rightInfo = parseTermWithTrailingPercent()
                    val rightValue = if (rightInfo.isDirectPercent) {
                        // Standard calculator intuitive percent: A + B% = A + (A * B / 100)
                        BigNumberMath.divide(
                            BigNumberMath.multiply(left, rightInfo.valueBeforePercent),
                            BigDecimal(100)
                        )
                    } else {
                        rightInfo.finalValue
                    }
                    left = if (t.symbol == '+') {
                        BigNumberMath.add(left, rightValue)
                    } else {
                        BigNumberMath.subtract(left, rightValue)
                    }
                } else {
                    break
                }
            }
            return left
        }

        private data class TermPercentResult(
            val finalValue: BigDecimal,
            val valueBeforePercent: BigDecimal,
            val isDirectPercent: Boolean
        )

        private fun parseTermWithTrailingPercent(): TermPercentResult {
            // Check if this term is a single factor ending with % (like 10%)
            val startPos = pos
            val valResult = parseTerm()
            // Inspect if the immediately preceding token was Percent and there were no * or / at this term level
            var depth = 0
            var hasMulDiv = false
            for (idx in startPos until pos) {
                val tk = tokens[idx]
                if (tk is Token.LParen) depth++
                else if (tk is Token.RParen) depth--
                else if (depth == 0 && tk is Token.Op && (tk.symbol == '*' || tk.symbol == '/')) {
                    hasMulDiv = true
                    break
                }
            }
            if (!hasMulDiv && pos > startPos && tokens[pos - 1] is Token.Percent) {
                // valResult was already divided by 100, so valueBeforePercent = valResult * 100
                val rawBefore = BigNumberMath.multiply(valResult, BigDecimal(100))
                return TermPercentResult(valResult, rawBefore, isDirectPercent = true)
            }
            return TermPercentResult(valResult, valResult, isDirectPercent = false)
        }

        private fun parseTerm(): BigDecimal {
            var left = parsePower()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '*' || t.symbol == '/')) {
                    consume()
                    val right = parsePower()
                    left = if (t.symbol == '*') {
                        BigNumberMath.multiply(left, right)
                    } else {
                        BigNumberMath.divide(left, right)
                    }
                } else {
                    break
                }
            }
            return left
        }

        private fun parsePower(): BigDecimal {
            val base = parseUnary()
            val t = peek()
            if (t is Token.Op && t.symbol == '^') {
                consume()
                val exponent = parsePower() // Right-associative
                return BigNumberMath.pow(base, exponent)
            }
            return base
        }

        private fun parseUnary(): BigDecimal {
            val t = peek()
            if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                consume()
                val v = parseUnary()
                return if (t.symbol == '-') v.negate() else v
            }
            return parsePostfix()
        }

        private fun parsePostfix(): BigDecimal {
            var value = parsePrimary()
            while (true) {
                when (peek()) {
                    is Token.Percent -> {
                        consume()
                        value = BigNumberMath.divide(value, BigDecimal(100))
                    }
                    is Token.Factorial -> {
                        consume()
                        value = BigNumberMath.factorial(value)
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parsePrimary(): BigDecimal {
            val t = peek() ?: throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
            return when (t) {
                is Token.NumberLit -> {
                    consume()
                    t.value
                }
                is Token.Constant -> {
                    consume()
                    when (t.symbol) {
                        'π' -> BigNumberMath.PI
                        'e' -> BigNumberMath.E
                        else -> throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
                    }
                }
                is Token.LParen -> {
                    consume()
                    val inner = parseExpression()
                    if (peek() !is Token.RParen) {
                        throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
                    }
                    consume()
                    inner
                }
                is Token.Func -> {
                    consume()
                    val arg = if (peek() is Token.LParen) {
                        consume()
                        val inner = parseExpression()
                        if (peek() !is Token.RParen) {
                            throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
                        }
                        consume()
                        inner
                    } else {
                        // Allow e.g. √9 without parens
                        parseUnary()
                    }
                    val isDeg = angleUnit == AngleUnit.DEG
                    when (t.name) {
                        "sin" -> BigNumberMath.sin(arg, isDeg)
                        "cos" -> BigNumberMath.cos(arg, isDeg)
                        "tan" -> BigNumberMath.tan(arg, isDeg)
                        "asin" -> BigNumberMath.asin(arg, isDeg)
                        "acos" -> BigNumberMath.acos(arg, isDeg)
                        "atan" -> BigNumberMath.atan(arg, isDeg)
                        "ln" -> BigNumberMath.ln(arg)
                        "log" -> BigNumberMath.log10(arg)
                        "sqrt" -> BigNumberMath.sqrt(arg)
                        else -> throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
                    }
                }
                else -> throw MathEvaluationException(MathErrorType.SYNTAX_ERROR)
            }
        }
    }
}
