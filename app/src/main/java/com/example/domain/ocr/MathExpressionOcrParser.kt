package com.example.domain.ocr

import com.example.data.preferences.AngleUnit
import com.example.domain.math.EvaluationResult
import com.example.domain.math.ExpressionEvaluator
import com.example.domain.math.MathErrorType
import com.example.domain.math.NumberFormatter
import kotlin.math.abs

data class OcrBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)
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

    private val superscriptMap = mapOf(
        '⁰' to '0',
        '¹' to '1',
        '²' to '2',
        '³' to '3',
        '⁴' to '4',
        '⁵' to '5',
        '⁶' to '6',
        '⁷' to '7',
        '⁸' to '8',
        '⁹' to '9',
        '⁻' to '−',
        '⁺' to '+'
    )

    private val problemStartRegex = Regex("^\\s*(?:\\d{1,2}|[a-zA-Zа-яА-ЯіІїЇєЄ])[).]\\s+")
    private val fractionBarRegex = Regex("^\\s*[-—–_=─━]{2,}\\s*$")

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
                lines.filter { line ->
                    val b = line.box ?: return@filter false
                    b.right >= reticleBox.left && b.left <= reticleBox.right &&
                        b.bottom >= reticleBox.top && b.top <= reticleBox.bottom
                }
            }
        } else {
            lines
        }

        val expanded = augmentWithSpatialLines(prioritizedLines)
        var bestCandidate: ScannedMathExpression? = null
        var bestScore = Int.MIN_VALUE

        for (line in expanded) {
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

        val expandedLines = augmentWithSpatialLines(lines)
        val allParsed = mutableListOf<Triple<ParsedCandidate, OcrBox?, Int>>()

        for (line in expandedLines) {
            val candidates = parseLineAll(line.text, angleUnit)
            for (candidate in candidates) {
                val hasOp = hasOperatorOrFunction(candidate.expr)
                val score = buildScore(candidate.expr, hasOp, candidate.hasValidEval)
                allParsed.add(Triple(candidate, line.box, score))
            }
        }

        if (allParsed.isEmpty()) return emptyList()

        // Prefer real expressions with operators/functions over stray standalone numbers
        val withOps = allParsed.filter { hasOperatorOrFunction(it.first.expr) }
        val filtered = if (withOps.isNotEmpty()) withOps else allParsed

        // Sort highest score first when deduplicating overlapping fragments so merged expressions win over partials
        val byScoreDesc = filtered.sortedByDescending { it.third }
        val deduped = mutableListOf<Pair<ParsedCandidate, OcrBox?>>()
        for ((candidate, box, _) in byScoreDesc) {
            val overlapsExisting = deduped.any { (existingCandidate, existingBox) ->
                val overlaps = boxesOverlapSignificantly(existingBox, box)
                if (!overlaps) return@any false
                existingCandidate.expr == candidate.expr ||
                    (existingCandidate.expr.contains(candidate.expr) &&
                        boxContainedMostlyInside(box, existingBox))
            }
            if (!overlapsExisting) {
                deduped.add(candidate to box)
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
                    id = id.ifEmpty { "ai_$cleaned" },
                    rawExpression = cleaned,
                    formattedExpression = NumberFormatter.formatExpressionForDisplay(cleaned),
                    previewResult = preview,
                    boundingBox = boundingBox
                )
            }
        }

        return best
    }

    /**
     * Reconstructs math expressions split across multiple ML Kit OCR boxes:
     * 1. Spatial superscripts (e.g. small "3" slightly above/right of "2" -> "2^3")
     * 2. Same-row fragments (e.g. "25 +" and "15" on the same baseline -> "25 + 15")
     * 3. Vertical stacked fractions separated by a fraction bar ("12 + 8" / "---" / "4" -> "(12+8)÷(4)")
     */
    private fun augmentWithSpatialLines(lines: List<OcrLineCandidate>): List<OcrLineCandidate> {
        val withBoxes = lines.filter { it.box != null && it.text.isNotBlank() }
        if (withBoxes.size <= 1) return lines

        val augmented = lines.toMutableList()

        // 1. Vertical fractions separated by a horizontal bar line
        val barLines = withBoxes.filter { fractionBarRegex.matches(it.text) }
        val nonBarLines = withBoxes.filterNot { fractionBarRegex.matches(it.text) }
        for (bar in barLines) {
            val bBox = bar.box ?: continue
            val topCandidate = nonBarLines
                .filter { cand ->
                    val c = cand.box ?: return@filter false
                    c.bottom <= bBox.centerY &&
                        (bBox.centerY - c.bottom) <= maxOf(c.height, 24f) * 1.8f &&
                        horizontalOverlapRatio(c, bBox) >= 0.35f &&
                        !problemStartRegex.containsMatchIn(cand.text)
                }
                .maxByOrNull { it.box!!.bottom }

            val bottomCandidate = nonBarLines
                .filter { cand ->
                    val c = cand.box ?: return@filter false
                    c.top >= bBox.centerY &&
                        (c.top - bBox.centerY) <= maxOf(c.height, 24f) * 1.8f &&
                        horizontalOverlapRatio(c, bBox) >= 0.35f &&
                        !problemStartRegex.containsMatchIn(cand.text)
                }
                .minByOrNull { it.box!!.top }

            if (topCandidate != null && bottomCandidate != null) {
                val combinedBox = unionBoxes(
                    listOfNotNull(topCandidate.box, bBox, bottomCandidate.box)
                )
                augmented.add(
                    OcrLineCandidate(
                        text = "(${topCandidate.text.trim()}) / (${bottomCandidate.text.trim()})",
                        box = combinedBox
                    )
                )
            }
        }

        // 2. Spatial superscripts & same-row horizontal fragment merging
        val sortedReading = nonBarLines.sortedWith(
            compareBy<OcrLineCandidate>({ it.box!!.top }, { it.box!!.left })
        )

        val rowMerged = mutableListOf<OcrLineCandidate>()
        val usedForRow = BooleanArray(sortedReading.size)

        for (i in sortedReading.indices) {
            if (usedForRow[i]) continue
            var current = sortedReading[i]
            var currBox = current.box ?: continue
            var mergedAny = false

            // Greedily attach subsequent fragments on the same row or superscripts
            var changed = true
            while (changed) {
                changed = false
                var bestNextIdx = -1
                var bestDistance = Float.MAX_VALUE
                var isSuperscriptAttach = false

                for (j in sortedReading.indices) {
                    if (i == j || usedForRow[j]) continue
                    val next = sortedReading[j]
                    val nextBox = next.box ?: continue
                    if (nextBox.centerX <= currBox.left) continue

                    val hGap = nextBox.left - currBox.right
                    val refHeight = maxOf(currBox.height, nextBox.height)
                    if (hGap < -refHeight * 0.35f || hGap > refHeight * 2.0f) continue

                    // Check superscript placement: smaller box shifted to upper-right
                    val supText = next.text.trim()
                    val looksLikeSup = supText.length <= 6 &&
                        supText.matches(Regex("[+−-]?[0-9a-zA-Z()]+"))
                    val isUpperRight = nextBox.height <= currBox.height * 0.78f &&
                        nextBox.centerY <= currBox.top + currBox.height * 0.45f &&
                        hGap <= currBox.height * 1.3f

                    if (looksLikeSup && isUpperRight && currBox.width >= nextBox.width * 0.5f) {
                        if (hGap < bestDistance) {
                            bestDistance = hGap
                            bestNextIdx = j
                            isSuperscriptAttach = true
                        }
                        continue
                    }

                    // Otherwise check same-baseline fragment
                    val overlapY = minOf(currBox.bottom, nextBox.bottom) - maxOf(currBox.top, nextBox.top)
                    val minH = minOf(currBox.height, nextBox.height).coerceAtLeast(1f)
                    val sameRow = overlapY >= minH * 0.42f ||
                        abs(currBox.centerY - nextBox.centerY) <= maxOf(currBox.height, nextBox.height) * 0.45f

                    if (!sameRow) continue
                    if (problemStartRegex.containsMatchIn(next.text)) continue

                    val leftEndsWithOp = current.text.trimEnd().lastOrNull()?.let {
                        it == '+' || it == '-' || it == '−' || it == '*' || it == '×' ||
                            it == '/' || it == '÷' || it == '^' || it == '(' || it == '='
                    } ?: false
                    val rightStartsWithOp = next.text.trimStart().firstOrNull()?.let {
                        it == '+' || it == '-' || it == '−' || it == '*' || it == '×' ||
                            it == '/' || it == '÷' || it == '^' || it == ')' || it == '='
                    } ?: false

                    if (hGap <= refHeight * 1.4f || leftEndsWithOp || rightStartsWithOp) {
                        if (hGap < bestDistance) {
                            bestDistance = hGap
                            bestNextIdx = j
                            isSuperscriptAttach = false
                        }
                    }
                }

                if (bestNextIdx >= 0) {
                    val next = sortedReading[bestNextIdx]
                    val nextBox = next.box!!
                    usedForRow[bestNextIdx] = true
                    mergedAny = true
                    changed = true
                    val nextTrimmed = next.text.trim()
                    val newText = if (isSuperscriptAttach) {
                        val cleanExp = if (nextTrimmed.all { it.isDigit() }) {
                            nextTrimmed
                        } else {
                            "($nextTrimmed)"
                        }
                        "${current.text.trimEnd()}^$cleanExp"
                    } else {
                        "${current.text.trimEnd()} $nextTrimmed"
                    }
                    currBox = unionBoxes(listOf(currBox, nextBox))
                    current = OcrLineCandidate(newText, currBox)
                }
            }

            if (mergedAny) {
                rowMerged.add(current)
            }
        }

        augmented.addAll(rowMerged)
        return augmented
    }

    private fun unionBoxes(boxes: List<OcrBox>): OcrBox {
        return OcrBox(
            left = boxes.minOf { it.left },
            top = boxes.minOf { it.top },
            right = boxes.maxOf { it.right },
            bottom = boxes.maxOf { it.bottom }
        )
    }

    private fun horizontalOverlapRatio(a: OcrBox, b: OcrBox): Float {
        val overlap = (minOf(a.right, b.right) - maxOf(a.left, b.left)).coerceAtLeast(0f)
        val minW = minOf(a.width, b.width).coerceAtLeast(1f)
        return overlap / minW
    }

    private fun boxContainedMostlyInside(inner: OcrBox?, outer: OcrBox?): Boolean {
        if (inner == null || outer == null) return false
        val interL = maxOf(inner.left, outer.left)
        val interT = maxOf(inner.top, outer.top)
        val interR = minOf(inner.right, outer.right)
        val interB = minOf(inner.bottom, outer.bottom)
        if (interR <= interL || interB <= interT) return false
        val interArea = (interR - interL) * (interB - interT)
        val innerArea = (inner.width * inner.height).coerceAtLeast(1f)
        return interArea / innerArea >= 0.65f
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

        val preNormalized = preprocessMathText(rawText, splitMultipleProblems = true)
        val chunks = preNormalized.split(';').filter { it.isNotBlank() }
        val results = mutableListOf<ParsedCandidate>()

        for (chunk in chunks) {
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
                val (syntaxValid, preview) = when (evalResult) {
                    is EvaluationResult.Success -> true to evalResult.formatted.displayText
                    is EvaluationResult.Idle -> (!hasOp) to ""
                    is EvaluationResult.Error -> {
                        val allowNonSyntaxError = hasOp &&
                            evalResult.errorType != MathErrorType.SYNTAX_ERROR
                        allowNonSyntaxError to ""
                    }
                }

                if (!syntaxValid && hasOp) continue

                results.add(ParsedCandidate(cleaned, preview, evalResult is EvaluationResult.Success))
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
            // Turn problem numbering at line start or after multi-space/semicolon into separators
            s = s.replace(Regex("(?:^|(?<=\\s{2,}|;))\\d{1,2}[).]\\s+"), " ; ")
            s = s.replace(Regex("(?:^|(?<=\\s{2,}|;))[a-zA-Zа-яА-ЯіІїЇєЄ][).]\\s+"), " ; ")
        }

        // Convert Unicode superscript runs (e.g., 2³, 10⁻², 5¹²) into ^(...)
        s = convertUnicodeSuperscripts(s)

        s = s
            .replace('\u2009', ' ')
            .replace('\u00A0', ' ')
            .replace('≈', '=')
            .replace('~', '=')
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
            .replace('[', '(')
            .replace(']', ')')
            .replace('{', '(')
            .replace('}', ')')

        // Common ML Kit OCR misreads for math functions before normalizing keywords
        s = s.replace(Regex("(?<![\\d.])(?:1n|In)(?=\\s*[0-9(πe])"), "ln")
            .replace(Regex("\\b(?:1og|l0g|Iog)\\b"), "log")
            .replace(Regex("\\b(?:s1n|5in)\\b"), "sin")
            .replace(Regex("\\bc0s\\b"), "cos")
            // ML Kit Latin recognizer often outputs 'V' or 'v' for radical sign √
            .replace(Regex("(?<=^|[\\s+−×÷^(,=])[vV](?=\\s*(?:\\(|\\d))"), "√")

        // Normalize named functions & constants (case-insensitive, including Ukrainian aliases tg/arctg/lg)
        s = s.replace(Regex("(?i)\\barcsin\\b"), "sin⁻¹")
            .replace(Regex("(?i)\\barccos\\b"), "cos⁻¹")
            .replace(Regex("(?i)\\barctan\\b"), "tan⁻¹")
            .replace(Regex("(?i)\\barctg\\b"), "tan⁻¹")
            .replace(Regex("(?i)\\basin\\b"), "sin⁻¹")
            .replace(Regex("(?i)\\bacos\\b"), "cos⁻¹")
            .replace(Regex("(?i)\\batan\\b"), "tan⁻¹")
            .replace(Regex("(?i)\\bsqrt"), "√")
            .replace(Regex("(?i)\\bsin\\b"), "sin")
            .replace(Regex("(?i)\\bcos\\b"), "cos")
            .replace(Regex("(?i)\\btan\\b"), "tan")
            .replace(Regex("(?i)\\btg\\b"), "tan")
            .replace(Regex("(?i)\\bln\\b"), "ln")
            .replace(Regex("(?i)\\blog\\b"), "log")
            .replace(Regex("(?i)\\blg\\b"), "log")
            .replace(Regex("(?i)\\bpi\\b"), "π")

        // Underscore between operands is often misread minus
        s = s.replace(Regex("(?<=[0-9)πe])\\s*_\\s*(?=[0-9(πe√])"), "−")

        // Spaced dot between operands is multiplication ("5 . 3" -> "5×3"), keep "5.3" as decimal
        s = s.replace(Regex("(?<=[0-9)πe])\\s+\\.\\s+(?=[0-9(πe√])"), "×")

        // Division colon in arithmetic context: "12 : 4" or "12:4" -> "12÷4"
        s = s.replace(Regex("(?<=[0-9)πe])\\s*:\\s*(?=[0-9(πe√])"), "÷")

        // Multiplication 'x'/'X' or Cyrillic 'х'/'Х' between operands
        s = s.replace(Regex("(?<=[0-9)%!πe])\\s*[xXхХ]\\s*(?=[0-9(πe√a-zA-Z])"), "×")

        // Ukrainian/European decimal comma -> dot
        s = s.replace(Regex("\\b\\d{1,3}(,\\d{3})+\\b")) { match ->
            match.value.replace(",", "")
        }
        s = s.replace(Regex("(?<=\\d),(?=\\d)"), ".")

        // Split two separate expressions placed side-by-side on the same OCR line
        if (splitMultipleProblems) {
            s = s.replace(Regex("(?<=[0-9)πe%!])\\s{2,}(?=[0-9(πe√a-zA-Z])"), " ; ")
        }

        // Remove single space inside numbers (thousands separators like "1 000")
        s = s.replace(Regex("(?<=\\d) (?=\\d{3}(?:\\D|$))"), "")

        // Fix common OCR lookalikes when directly adjacent to digits/math operators
        s = fixOcrMathCharLookalikes(s)

        return s
    }

    private fun convertUnicodeSuperscripts(input: String): String {
        if (input.none { it in superscriptMap }) return input
        val sb = StringBuilder(input.length + 4)
        var i = 0
        while (i < input.length) {
            if (input[i] in superscriptMap) {
                val exp = StringBuilder()
                while (i < input.length && input[i] in superscriptMap) {
                    exp.append(superscriptMap[input[i]])
                    i++
                }
                val expStr = exp.toString()
                sb.append('^')
                if (expStr.length > 1 && !expStr.all { it.isDigit() }) {
                    sb.append('(').append(expStr).append(')')
                } else {
                    sb.append(expStr)
                }
            } else {
                sb.append(input[i])
                i++
            }
        }
        return sb.toString()
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
        while (s.isNotEmpty() && (s.first() == '+' || s.first() == '×' || s.first() == '÷' ||
                s.first() == '^' || s.first() == '%' || s.first() == '!' || s.first() == ')')
        ) {
            s = s.drop(1)
        }
        while (s.isNotEmpty() && (s.last() == '+' || s.last() == '−' || s.last() == '×' ||
                s.last() == '÷' || s.last() == '^' || s.last() == '(' || s.last() == '.')
        ) {
            s = s.dropLast(1)
        }
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
