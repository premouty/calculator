package com.example.ai

import android.graphics.Bitmap
import com.example.data.preferences.AngleUnit
import com.example.domain.ocr.MathExpressionOcrParser
import com.example.domain.ocr.OcrBox
import com.example.domain.ocr.ScannedMathExpression
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

class CameraMathAiAnalyzer {

    private val expressionItemSchema = Schema.obj(
        properties = mapOf(
            "expression" to Schema.string(
                description = "Calculator-ready math expression or standalone number using +, -, *, /, ^, sqrt(...), sin(...), cos(...), tan(...), asin(...), acos(...), atan(...), ln(...), log(...), pi, e, %, !, and parentheses. Omit '= answer', problem labels like '1)', and non-math words."
            ),
            "ymin" to Schema.integer(description = "Top edge normalized to 0..1000"),
            "xmin" to Schema.integer(description = "Left edge normalized to 0..1000"),
            "ymax" to Schema.integer(description = "Bottom edge normalized to 0..1000"),
            "xmax" to Schema.integer(description = "Right edge normalized to 0..1000")
        )
    )

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "expressions" to Schema.array(
                items = expressionItemSchema,
                description = "All math expressions and standalone numbers visible in reading order."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@CameraMathAiAnalyzer.responseSchema
                temperature = 0.1f
            }
        )
    }

    suspend fun analyzePhoto(
        bitmap: Bitmap,
        angleUnit: AngleUnit
    ): List<ScannedMathExpression> {
        val inferenceBitmap = withContext(Dispatchers.Default) {
            scaleDownForInference(bitmap, maxDimension = 1280)
        }
        val shouldRecycleCopy = inferenceBitmap !== bitmap

        return try {
            val response = model.generateContent(
                content {
                    image(inferenceBitmap)
                    text(
                        """
                        Extract all mathematical expressions, equations, and meaningful standalone numbers visible in this image (printed or handwritten).
                        Rules:
                        - Convert stacked fractions into parenthesized division, e.g., (a+b)/(c+d) or (3)/(4).
                        - Convert radicals into sqrt(...), exponents into ^ (e.g., 2^3), trig into sin(...), cos(...), tan(...), asin(...), acos(...), atan(...), logarithms into ln(...) or log(...), constants into pi or e.
                        - Use standard calculator operators +, -, *, / and decimal dots '.' (convert European decimal commas to dots, ignore thousands separators).
                        - If an equation shows '= result' or '= ?' (like '25 + 15 = 40'), return ONLY the left-hand expression ('25 + 15').
                        - Strip problem numbering prefixes like '1)', '2.', 'a)' and any surrounding text.
                        - For each detected expression, provide its tight bounding box normalized to [ymin, xmin, ymax, xmax] on a 0..1000 scale.
                        - Return an empty expressions array if no numbers or math expressions are present.
                        """.trimIndent()
                    )
                }
            )

            val jsonString = response.text
                ?: throw IllegalStateException("Модель повернула порожню відповідь.")

            withContext(Dispatchers.Default) {
                parseStructuredResponse(
                    jsonString = jsonString,
                    targetWidth = bitmap.width.toFloat().coerceAtLeast(1f),
                    targetHeight = bitmap.height.toFloat().coerceAtLeast(1f),
                    angleUnit = angleUnit
                )
            }
        } finally {
            if (shouldRecycleCopy && !inferenceBitmap.isRecycled) {
                inferenceBitmap.recycle()
            }
        }
    }

    internal fun parseStructuredResponse(
        jsonString: String,
        targetWidth: Float,
        targetHeight: Float,
        angleUnit: AngleUnit
    ): List<ScannedMathExpression> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("expressions") ?: return emptyList()

        val parsed = mutableListOf<ScannedMathExpression>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val rawExpr = item.optString("expression", "").trim()
            if (rawExpr.isEmpty()) continue

            val box = extractBoundingBox(item, targetWidth, targetHeight)
            val normalized = MathExpressionOcrParser.normalizeAiExpression(
                rawExpr = rawExpr,
                angleUnit = angleUnit,
                boundingBox = box,
                id = "ai_${i}_${rawExpr.hashCode()}"
            ) ?: continue

            val isDuplicate = parsed.any { existing ->
                existing.rawExpression == normalized.rawExpression &&
                    boxesOverlap(existing.boundingBox, normalized.boundingBox)
            }
            if (!isDuplicate) {
                parsed.add(normalized)
            }
        }

        val withOps = parsed.filter { hasOperatorOrFunction(it.rawExpression) }
        val preferred = if (withOps.isNotEmpty()) withOps else parsed

        return preferred
            .sortedWith(
                compareBy<ScannedMathExpression>(
                    { it.boundingBox?.top ?: Float.MAX_VALUE },
                    { it.boundingBox?.left ?: Float.MAX_VALUE }
                )
            )
            .mapIndexed { idx, expr ->
                expr.copy(id = "ai_${idx}_${expr.rawExpression}")
            }
    }

    private fun extractBoundingBox(
        item: JSONObject,
        targetWidth: Float,
        targetHeight: Float
    ): OcrBox? {
        if (!item.has("ymin") || !item.has("xmin") || !item.has("ymax") || !item.has("xmax")) {
            return null
        }
        val yMin = item.optDouble("ymin", Double.NaN).toFloat()
        val xMin = item.optDouble("xmin", Double.NaN).toFloat()
        val yMax = item.optDouble("ymax", Double.NaN).toFloat()
        val xMax = item.optDouble("xmax", Double.NaN).toFloat()
        if (yMin.isNaN() || xMin.isNaN() || yMax.isNaN() || xMax.isNaN()) return null

        val normLeft = (minOf(xMin, xMax) / 1000f).coerceIn(0f, 1f)
        val normTop = (minOf(yMin, yMax) / 1000f).coerceIn(0f, 1f)
        val normRight = (maxOf(xMin, xMax) / 1000f).coerceIn(normLeft, 1f)
        val normBottom = (maxOf(yMin, yMax) / 1000f).coerceIn(normTop, 1f)

        if (normRight <= normLeft || normBottom <= normTop) return null

        return OcrBox(
            left = normLeft * targetWidth,
            top = normTop * targetHeight,
            right = normRight * targetWidth,
            bottom = normBottom * targetHeight
        )
    }

    private fun boxesOverlap(a: OcrBox?, b: OcrBox?): Boolean {
        if (a == null || b == null) return a == null && b == null
        return minOf(a.right, b.right) > maxOf(a.left, b.left) &&
            minOf(a.bottom, b.bottom) > maxOf(a.top, b.top)
    }

    private fun hasOperatorOrFunction(expr: String): Boolean {
        return expr.any {
            it == '+' || it == '−' || it == '×' || it == '÷' ||
                it == '^' || it == '%' || it == '!' || it == '√'
        } || expr.contains("sin") || expr.contains("cos") || expr.contains("tan") ||
            expr.contains("ln") || expr.contains("log")
    }

    private fun scaleDownForInference(source: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(source.width, source.height)
        if (maxSide <= maxDimension) return source
        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val targetW = (source.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (source.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
