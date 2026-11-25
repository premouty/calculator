package com.example.domain.ocr

import com.example.data.preferences.AngleUnit
import com.example.domain.math.EvaluationResult
import com.example.domain.math.ExpressionEvaluator
import com.example.domain.math.NumberFormatter

data class OcrBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class OcrLineCandidate(
    val text: String,
    val box: OcrBox? = null
)

data class ScannedMathExpression(
    val id: String = "",
    val rawExpression: String,
    val formattedExpression: String,
    val previewResult: String,
    val boundingBox: OcrBox? = null
)

object MathExpressionOcrParser {

    private val functionKeywords = listOf(
        "sin⁻¹", "cos⁻¹", "tan⁻¹",
        "asin", "acos", "atan",
        "sqrt", "sin", "cos", "tan",
        "ln", "log"
    )

    fun extractBestExpression(
        lines: List<OcrLineCandidate>,
        reticleBox: OcrBox?,
        angleUnit: AngleUnit
    ): ScannedMathExpression? {
        if (lines.isEmpty()) return null

        val prioritizedLines = if (reticleBox != null) {
            val inside = lines.filter { line ->
                val b = line.box ?: return@filter false
                b.centerX in reticleBox.left..reticleBox.right &&
                    b.centerY in reticleBox.top..reticleBox.bottom
            }
            inside.ifEmpty {
                // Fallback to lines that overlap the reticle horizontally & vertically
                lines.filter { line ->
                    val b = line.box ?: return@filter false
                    b.right >= reticleBox.left && b.left <= reticleBox.right &&
                        b.bottom >= reticleBox.top && b.top <= reticleBox.bottom
                }
            }
        } else {
            lines
        }

        var bestCandidate: ScannedMathExpression? = null
        var bestScore = Int.MIN_VALUE

        for (line in prioritizedLines) {
            val parsed = parseLine(line.text, angleUnit) ?: continue
            val hasOpOrFunc = hasOperatorOrFunction(parsed.expr)
            val score = buildScore(parsed.expr, hasOpOrFunc, parsed.hasValidEval)
            if (score > bestScore) {
                bestScore = score
                bestCandidate = ScannedMathExpression(
                    rawExpression = parsed.expr,
                    formattedExpression = NumberFormatter.formatExpressionForDisplay(parsed.expr),
                    previewResult = parsed.preview,
                    boundingBox = line.box
                )
            }
        }

        // Also try merging adjacent horizontal/vertical lines inside reticle if single lines failed
        if (bestCandidate == null && prioritizedLines.size in 2..3) {
            val combined = prioritizedLines.joinToString(" ") { it.text }
            val parsed = parseLine(combined, angleUnit)
            if (parsed != null && hasOperatorOrFunction(parsed.expr)) {
                bestCandidate = ScannedMathExpression(
                    rawExpression = parsed.expr,
                    formattedExpression = NumberFormatter.formatExpressionForDisplay(parsed.expr),
                    previewResult = parsed.preview,
                    boundingBox = prioritizedLines.firstOrNull()?.box
                )
            }
        }

        return bestCandidate
    }

    fun extractAllExpressions(
        lines: List<OcrLineCandidate>,
        angleUnit: AngleUnit
    ): List<ScannedMathExpression> {
        if (lines.isEmpty()) return emptyList()

        val allParsed = mutableListOf<Pair<ParsedCandidate, OcrBox?>>()
        for (line in lines) {
            val candidates = parseLineAll(line.text, angleUnit)
            for (candidate in candidates) {
                allParsed.add(candidate to line.box)
            }
        }

        if (allParsed.isEmpty()) return emptyList()

        // Prefer real expressions with operators/functions over stray standalone numbers (e.g. page numbers)
        val withOps = allParsed.filter { hasOperatorOrFunction(it.first.expr) }
        val filtered = if (withOps.isNotEmpty()) withOps else allParsed

        // Deduplicate overlapping identical expressions while keeping distinct occurrences
        val deduped = mutableListOf<Pair<ParsedCandidate, OcrBox?>>()
        for (entry in filtered) {
            val isDuplicate = deduped.any { existing ->
                existing.first.expr == entry.first.expr &&
                    boxesOverlapSignificantly(existing.second, entry.second)
            }
            if (!isDuplicate) {
                deduped.add(entry)
            }
        }

        // Sort in natural reading order: top-to-bottom, then left-to-right
        val sorted = deduped.sortedWith(
            compareBy<Pair<ParsedCandidate, OcrBox?>>(
                { it.second?.top ?: Float.MAX_VALUE },
                { it.second?.left ?: Float.MAX_VALUE }
            )
        )

        return sorted.mapIndexed { index, (candidate, box) ->
            ScannedMathExpression(
                id = "expr_${index}_${candidate.expr}",
                rawExpression = candidate.expr,
                formattedExpression = NumberFormatter.formatExpressionForDisplay(candidate.expr),
                previewResult = candidate.preview,
                boundingBox = box
            )
        }
    }

    fun normalizeRawLine(rawText: String): String? {
        return parseLine(rawText, AngleUnit.DEG)?.expr
    }

    /**
     * Normalizes an expression or standalone number returned by Gemini Vision AI,
     * validates it with [ExpressionEvaluator], and maps it to a [ScannedMathExpression].
     */
    fun normalizeAiExpression(
        rawExpr: String,
        angleUnit: AngleUnit,
        boundingBox: OcrBox? = null,
        id: String = ""
    ): ScannedMathExpression? {
        if (rawExpr.isBlank()) return null

        // Light LaTeX cleanup if model ever emits stray TeX wrappers
        val texCleaned = rawExpr
            .replace(Regex("\\\\times"), "×")
            .replace(Regex("\\\\cdot"), "×")
            .replace(Regex("\\\\div"), "÷")
            .replace(Regex("\\\\sqrt\\s*\\{"), "√(")
            .replace(Regex("\\\\pi"), "π")
            .replace("$", "")
            .trim()

        val preNormalized = preprocessMathText(texCleaned, splitMultipleProblems = false)
        val beforeEquals = preNormalized.substringBefore('=').trim()
        if (beforeEquals.isBlank()) return null

        val segments = extractMathSegments(beforeEquals)
        if (segments.isEmpty()) return null

        // AI sends one expression per entry; prefer the highest-scoring valid segment
        var best: ScannedMathExpression? = null
        var bestScore = Int.MIN_VALUE

        for (seg in segments) {
            val cleaned = finalizeExpression(seg)
            if (cleaned.isEmpty()) continue
            if (!cleaned.any { it.isDigit() || it == 'π' || it == 'e' }) continue

            val hasOp = hasOperatorOrFunction(cleaned)
            val evalResult = ExpressionEvaluator.evaluate(cleaned, angleUnit)
            val (validEval, preview) = when (evalResult) {
                is EvaluationResult.Success -> true to (if (hasOp) evalResult.formatted.displayText else "")
                is EvaluationResult.Idle -> {
                    val isStandaloneNumberOrConst = !hasOp &&
                        (cleaned == "π" || cleaned == "e" || cleaned.toBigDecimalOrNull() != null)
                    isStandaloneNumberOrConst to ""
                }
                is EvaluationResult.Error -> false to ""
            }

            if (!validEval) continue

            val score = buildScore(cleaned, hasOp, true)
            if (score > bestScore) {
                bestScore = score
                best = ScannedMathExpression(
                    id = id.ifEmpty { "ai_${cleaned}" },
                    rawExpression = cleaned,
                    formattedExpression = NumberFormatter.formatExpressionForDisplay(cleaned),
                    previewResult = preview,
                    boundingBox = boundingBox
                )
            }
        }

        return best
    }

    private data class ParsedCandidate(
        val expr: String,
        val preview: String,
        val hasValidEval: Boolean
    )

    private fun parseLine(
        rawText: String,
        angleUnit: AngleUnit
    ): ParsedCandidate? {
        val candidates = parseLineAll(rawText, angleUnit)
        if (candidates.isEmpty()) return null

        var best: ParsedCandidate? = null
        var bestScore = Int.MIN_VALUE
        for (c in candidates) {
            val hasOp = hasOperatorOrFunction(c.expr)
            val score = buildScore(c.expr, hasOp, c.hasValidEval)
            if (score > bestScore) {
                bestScore = score
                best = c
            }
        }
        return best
    }

    private fun parseLineAll(
        rawText: String,
        angleUnit: AngleUnit
    ): List<ParsedCandidate> {
        if (rawText.isBlank()) return emptyList()

        // Handle lines that might contain multiple equations like "1) 2 + 2 = 4   2) 3 * 5 = 15"
        val preNormalized = preprocessMathText(rawText)
        val chunks = preNormalized.split(';').filter { it.isNotBlank() }
        val results = mutableListOf<ParsedCandidate>()

        for (chunk in chunks) {
            // Drop right-hand side after '=' (e.g. "25 + 15 = 40" or "25 + 15 = ?")
            val beforeEquals = chunk.substringBefore('=').trim()
            if (beforeEquals.isBlank()) continue

            val segments = extractMathSegments(beforeEquals)
            for (seg in segments) {
                val cleaned = finalizeExpression(seg)
                if (cleaned.isEmpty()) continue
                if (!cleaned.any { it.isDigit() || it == 'π' || it == 'e' }) continue

                val hasOp = hasOperatorOrFunction(cleaned)
                val digitCount = cleaned.count { it.isDigit() }
                if (!hasOp && digitCount < 2 && !cleaned.contains('.')) continue

                val evalResult = ExpressionEvaluator.evaluate(cleaned, angleUnit)
                val (validEval, preview) = when (evalResult) {
                    is EvaluationResult.Success -> true to evalResult.formatted.displayText
                    is EvaluationResult.Idle -> false to ""
                    is EvaluationResult.Error -> false to ""
                }

                if (!validEval && hasOp) continue

                results.add(ParsedCandidate(cleaned, preview, validEval))
            }
        }

        return results
    }

    private fun boxesOverlapSignificantly(a: OcrBox?, b: OcrBox?): Boolean {
        if (a == null || b == null) return a == null && b == null
        val overlapLeft = maxOf(a.left, b.left)
        val overlapTop = maxOf(a.top, b.top)
        val overlapRight = minOf(a.right, b.right)
        val overlapBottom = minOf(a.bottom, b.bottom)
        return overlapRight > overlapLeft && overlapBottom > overlapTop
    }

    private fun preprocessMathText(
        input: String,
        splitMultipleProblems: Boolean = true
    ): String {
        var s = input
        if (splitMultipleProblems) {
            // Turn problem numbering like "1) 25 + 5", "2. 14 * 3", "a) 8 + 2" into explicit separators
            s = s.replace(Regex("(?:^|\\s)\\d{1,2}[).]\\s+"), " ; ")
            s = s.replace(Regex("(?:^|\\s)[a-zA-Zа-яА-ЯіІїЇєЄ][).]\\s+"), " ; ")
        }

        s = s
            .replace('\u2009', ' ')
            .replace('\u00A0', ' ')
            .replace('–', '−')
            .replace('—', '−')
            .replace('‐', '−')
            .replace('-', '−')
            .replace('*', '×')
            .replace('·', '×')
            .replace('•', '×')
            .replace('⋅', '×')
            .replace('✕', '×')
            .replace('✖', '×')
            .replace('/', '÷')
            .replace('⁄', '÷')
            .replace("²", "^2")
            .replace("³", "^3")
            .replace('[', '(')
            .replace(']', ')')
            .replace('{', '(')
            .replace('}', ')')

        // Normalize named functions & constants (case-insensitive)
        s = s.replace(Regex("(?i)\\barcsin\\b"), "sin⁻¹")
            .replace(Regex("(?i)\\barccos\\b"), "cos⁻¹")
            .replace(Regex("(?i)\\barctan\\b"), "tan⁻¹")
            .replace(Regex("(?i)\\basin\\b"), "sin⁻¹")
            .replace(Regex("(?i)\\bacos\\b"), "cos⁻¹")
            .replace(Regex("(?i)\\batan\\b"), "tan⁻¹")
            .replace(Regex("(?i)\\bsqrt"), "√")
            .replace(Regex("(?i)\\bsin\\b"), "sin")
            .replace(Regex("(?i)\\bcos\\b"), "cos")
            .replace(Regex("(?i)\\btan\\b"), "tan")
            .replace(Regex("(?i)\\bln\\b"), "ln")
            .replace(Regex("(?i)\\blog\\b"), "log")
            .replace(Regex("(?i)\\bpi\\b"), "π")

        // Division colon in arithmetic context: "12 : 4" or "12:4" -> "12÷4"
        s = s.replace(Regex("(?<=[0-9)πe])\\s*:\\s*(?=[0-9(πe√])"), "÷")

        // Multiplication 'x' or 'X' between operands: "12 x 4", "2x3", "(2+1)x4", "2x(3+4)"
        s = s.replace(Regex("(?<=[0-9)%!πe])\\s*[xX]\\s*(?=[0-9(πe√a-zA-Z])"), "×")

        // Ukrainian/European decimal comma -> dot (when single comma between digits, or strip 3-digit grouping)
        s = s.replace(Regex("\\b\\d{1,3}(,\\d{3})+\\b")) { match ->
            match.value.replace(",", "")
        }
        s = s.replace(Regex("(?<=\\d),(?=\\d)"), ".")

        // Split two separate expressions placed side-by-side on the same OCR line (e.g. "12 + 5   8 * 4")
        if (splitMultipleProblems) {
            s = s.replace(Regex("(?<=[0-9)πe%!])\\s{2,}(?=[0-9(πe√a-zA-Z])"), " ; ")
        }

        // Remove single space inside numbers (thousands separators like "1 000")
        s = s.replace(Regex("(?<=\\d) (?=\\d{3}(?:\\D|$))"), "")

        // Fix common OCR lookalikes when directly adjacent to digits/math operators (not inside words)
        s = fixOcrMathCharLookalikes(s)

        return s
    }

    private fun fixOcrMathCharLookalikes(input: String): String {
        val chars = input.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            val prev = chars.getOrNull(i - 1)
            val next = chars.getOrNull(i + 1)
            val adjacentToMath = (prev != null && (prev.isDigit() || isMathOpOrParen(prev))) ||
                (next != null && (next.isDigit() || isMathOpOrParen(next)))

            if (!adjacentToMath) continue
            val isPartOfWord = (prev != null && prev.isLetter() && prev != 'π' && prev != 'e') ||
                (next != null && next.isLetter() && next != 'π' && next != 'e')
            if (isPartOfWord) continue

            when (c) {
                'O', 'o' -> chars[i] = '0'
                'l', 'I', '|' -> chars[i] = '1'
            }
        }
        return String(chars)
    }

    private fun extractMathSegments(text: String): List<String> {
        val segments = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0

        fun flush() {
            if (current.isNotEmpty()) {
                segments.add(current.toString())
                current.clear()
            }
        }

        while (i < text.length) {
            val matchedFunc = functionKeywords.firstOrNull { kw ->
                text.startsWith(kw, startIndex = i)
            }
            if (matchedFunc != null) {
                current.append(matchedFunc)
                i += matchedFunc.length
                continue
            }

            val ch = text[i]
            val isEulerConstant = ch == 'e' &&
                text.getOrNull(i - 1)?.isLetter() != true &&
                text.getOrNull(i + 1)?.isLetter() != true
            when {
                ch.isDigit() || ch == '.' || ch == 'π' || isEulerConstant || ch == '√' ||
                    ch == '+' || ch == '−' || ch == '×' || ch == '÷' || ch == '^' ||
                    ch == '(' || ch == ')' || ch == '%' || ch == '!' -> {
                    current.append(ch)
                    i++
                }
                ch.isWhitespace() -> {
                    // Preserve spaces only transiently; collapse them out if inside a continuous math expression
                    i++
                }
                else -> {
                    flush()
                    i++
                }
            }
        }
        flush()
        return segments
    }

    private fun finalizeExpression(raw: String): String {
        var s = raw.trim()
        // Strip leading invalid binary operators (+, ×, ÷, ^, %, !)
        while (s.isNotEmpty() && (s.first() == '+' || s.first() == '×' || s.first() == '÷' ||
                s.first() == '^' || s.first() == '%' || s.first() == '!' || s.first() == ')')
        ) {
            s = s.drop(1)
        }
        // Strip trailing incomplete binary operators or open parens
        while (s.isNotEmpty() && (s.last() == '+' || s.last() == '−' || s.last() == '×' ||
                s.last() == '÷' || s.last() == '^' || s.last() == '(' || s.last() == '.')
        ) {
            s = s.dropLast(1)
        }
        // Balance excess closing parens
        var depth = 0
        val out = StringBuilder()
        for (ch in s) {
            when (ch) {
                '(' -> {
                    depth++
                    out.append(ch)
                }
                ')' -> {
                    if (depth > 0) {
                        depth--
                        out.append(ch)
                    }
                }
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    private fun hasOperatorOrFunction(expr: String): Boolean {
        return expr.any {
            it == '+' || it == '−' || it == '×' || it == '÷' ||
                it == '^' || it == '%' || it == '!' || it == '√'
        } || functionKeywords.any { expr.contains(it) }
    }

    private fun buildScore(expr: String, hasOpOrFunc: Boolean, hasValidEval: Boolean): Int {
        var score = expr.length
        if (hasOpOrFunc) score += 100
        if (hasValidEval) score += 200
        return score
    }

    private fun isMathOpOrParen(ch: Char): Boolean {
        return ch == '+' || ch == '−' || ch == '×' || ch == '÷' ||
            ch == '^' || ch == '(' || ch == ')' || ch == '%' || ch == '!' || ch == '.'
    }
}
