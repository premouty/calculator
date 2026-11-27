package com.example.domain.voice

import com.example.data.preferences.AngleUnit
import com.example.domain.math.EvaluationResult
import com.example.domain.math.ExpressionEvaluator
import com.example.domain.math.NumberFormatter

data class ParsedVoiceMath(
    val rawExpression: String,
    val formattedExpression: String,
    val previewResult: String
)

object SpokenMathParser {

    private val unitsMap = mapOf(
        "нуль" to 0L, "ноль" to 0L, "zero" to 0L,
        "один" to 1L, "одна" to 1L, "одне" to 1L, "одиниці" to 1L, "одиничка" to 1L, "one" to 1L,
        "два" to 2L, "дві" to 2L, "двох" to 2L, "two" to 2L,
        "три" to 3L, "трьох" to 3L, "three" to 3L,
        "чотири" to 4L, "чотирьох" to 4L, "четыре" to 4L, "four" to 4L,
        "пять" to 5L, "п'ять" to 5L, "пяти" to 5L, "п'яти" to 5L, "пятьох" to 5L, "five" to 5L,
        "шість" to 6L, "шесть" to 6L, "шести" to 6L, "six" to 6L,
        "сім" to 7L, "семь" to 7L, "семи" to 7L, "seven" to 7L,
        "вісім" to 8L, "восемь" to 8L, "восьми" to 8L, "eight" to 8L,
        "девять" to 9L, "дев'ять" to 9L, "девяти" to 9L, "дев'яти" to 9L, "nine" to 9L,
        "десять" to 10L, "десяти" to 10L, "ten" to 10L,
        "одинадцять" to 11L, "одинадцяти" to 11L, "одиннадцать" to 11L, "eleven" to 11L,
        "дванадцять" to 12L, "дванадцяти" to 12L, "двенадцать" to 12L, "twelve" to 12L,
        "тринадцять" to 13L, "тринадцяти" to 13L, "тринадцать" to 13L, "thirteen" to 13L,
        "чотирнадцять" to 14L, "чотирнадцяти" to 14L, "четырнадцать" to 14L, "fourteen" to 14L,
        "пятнадцять" to 15L, "п'ятнадцять" to 15L, "пятнадцяти" to 15L, "п'ятнадцяти" to 15L, "пятнадцать" to 15L, "fifteen" to 15L,
        "шістнадцять" to 16L, "шістнадцяти" to 16L, "шестнадцать" to 16L, "sixteen" to 16L,
        "сімнадцять" to 17L, "сімнадцяти" to 17L, "семнадцать" to 17L, "seventeen" to 17L,
        "вісімнадцять" to 18L, "вісімнадцяти" to 18L, "восемнадцать" to 18L, "eighteen" to 18L,
        "девятнадцять" to 19L, "дев'ятнадцять" to 19L, "девятнадцяти" to 19L, "дев'ятнадцяти" to 19L, "девятнадцать" to 19L, "nineteen" to 19L
    )

    private val tensMap = mapOf(
        "двадцять" to 20L, "двадцяти" to 20L, "двадцать" to 20L, "twenty" to 20L,
        "тридцять" to 30L, "тридцать" to 30L, "тридцяти" to 30L, "thirty" to 30L,
        "сорок" to 40L, "сорока" to 40L, "forty" to 40L,
        "пятдесят" to 50L, "п'ятдесят" to 50L, "пятдесяти" to 50L, "п'ятдесяти" to 50L, "пятьдесят" to 50L, "fifty" to 50L,
        "шістдесят" to 60L, "шестьдесят" to 60L, "шістдесяти" to 60L, "sixty" to 60L,
        "сімдесят" to 70L, "сімдесяти" to 70L, "семьдесят" to 70L, "seventy" to 70L,
        "вісімдесят" to 80L, "вісімдесяти" to 80L, "восемьдесят" to 80L, "eighty" to 80L,
        "девяносто" to 90L, "дев'яносто" to 90L, "девяноста" to 90L, "дев'яноста" to 90L, "ninety" to 90L
    )

    private val hundredsMap = mapOf(
        "сто" to 100L, "ста" to 100L, "hundred" to 100L,
        "двісті" to 200L, "двести" to 200L, "двохсот" to 200L,
        "триста" to 300L, "трьохсот" to 300L,
        "чотириста" to 400L, "чотирьохсот" to 400L, "четыреста" to 400L,
        "пятсот" to 500L, "п'ятсот" to 500L, "пятисот" to 500L, "п'ятисот" to 500L, "пятьсот" to 500L,
        "шістсот" to 600L, "шестисот" to 600L, "шестьсот" to 600L,
        "сімсот" to 700L, "семисот" to 700L, "семьсот" to 700L,
        "вісімсот" to 800L, "восьмисот" to 800L, "восемьсот" to 800L,
        "девятсот" to 900L, "дев'ятсот" to 900L, "девятисот" to 900L, "дев'ятисот" to 900L, "девятьсот" to 900L
    )

    private val scalesMap = mapOf(
        "тисяча" to 1_000L, "тисячі" to 1_000L, "тисяч" to 1_000L, "тысяча" to 1_000L, "тысячи" to 1_000L, "thousand" to 1_000L,
        "мільйон" to 1_000_000L, "мільйони" to 1_000_000L, "мільйонів" to 1_000_000L, "миллион" to 1_000_000L, "million" to 1_000_000L
    )

    private val ordinalPowerMap = mapOf(
        "першому" to "1", "первом" to "1",
        "другому" to "2", "втором" to "2",
        "третьому" to "3", "третьем" to "3",
        "четвертому" to "4", "четвертом" to "4",
        "пятому" to "5", "п'ятому" to "5", "пятом" to "5",
        "шостому" to "6", "шестом" to "6",
        "сьомому" to "7", "седьмом" to "7",
        "восьмому" to "8", "восьмом" to "8",
        "девятому" to "9", "дев'ятому" to "9", "девятом" to "9",
        "десятому" to "10", "десятом" to "10"
    )

    private fun wordBound(pattern: String): Regex {
        return Regex("(?<![\\p{L}\\p{N}_])(?:$pattern)(?![\\p{L}\\p{N}_])")
    }

    fun parse(spokenText: String, angleUnit: AngleUnit): ParsedVoiceMath? {
        if (spokenText.isBlank()) return null

        var s = spokenText
            .lowercase()
            .replace('’', '\'')
            .replace('ʼ', '\'')
            .replace('\u00A0', ' ')

        // Strip conversational prefixes/suffixes
        s = s.replace(
            wordBound("скільки буде|скільки це|порахуй|обчисли|дорівнює|буде|равно|будь ласка|equals|what is|calculate"),
            " "
        )

        // Multi-word math phrases -> symbolic placeholders before word-by-word number assembly
        s = s
            .replace(wordBound("квадратний корінь із|квадратний корінь з|корінь квадратний із|корінь квадратний з|корінь із|корінь з|квадратный корень из|корень из|square root of|sqrt"), " √( ")
            .replace(
                wordBound(
                    "дужка\\s+відкривається|відкрити\\s+дужку|відкрий\\s+дужку|відкрита\\s+дужка|ліва\\s+дужка|" +
                        "скобка\\s+открывается|открыть\\s+скобку|открытая\\s+скобка|левая\\s+скобка|" +
                        "open\\s+parenthesis|open\\s+bracket"
                ),
                " ( "
            )
            .replace(
                wordBound(
                    "дужка\\s+закривається|закрити\\s+дужку|закрий\\s+дужку|закрита\\s+дужка|права\\s+дужка|" +
                        "скобка\\s+закрывается|закрыть\\s+скобку|закрытая\\s+скобка|правая\\s+скобка|" +
                        "close\\s+parenthesis|close\\s+bracket"
                ),
                " ) "
            )
            .replace(wordBound("натуральний логарифм|натуральный логарифм"), " ln( ")
            .replace(wordBound("десятковий логарифм|десятичный логарифм|логарифм"), " log( ")
            .replace(wordBound("арксинус|arcsin"), " sin⁻¹( ")
            .replace(wordBound("арккосинус|arccos"), " cos⁻¹( ")
            .replace(wordBound("арктангенс|arctan|arctg"), " tan⁻¹( ")
            .replace(wordBound("синус|sin"), " sin( ")
            .replace(wordBound("косинус|cos"), " cos( ")
            .replace(wordBound("тангенс|tan|tg"), " tan( ")
            .replace(wordBound("в квадраті|у квадраті|до квадрата|в квадрате|squared"), " ^ 2 ")
            .replace(wordBound("в кубі|у кубі|до куба|в кубе|cubed"), " ^ 3 ")
            .replace(Regex("(?<![\\p{L}\\p{N}_])(?:у|в|до)\\s+(першому|другому|третьому|четвертому|п'?ятому|шостому|сьомому|восьмому|дев'?ятому|десятому|первом|втором|третьем|четвертом|пятом|шестом|седьмом|восьмом|девятом|десятом)\\s+(?:степені|степеня|степінь|степени)(?![\\p{L}\\p{N}_])")) { match ->
                val exp = ordinalPowerMap[match.groupValues[1]] ?: ""
                if (exp.isNotEmpty()) " ^ $exp " else " ^ "
            }
            .replace(wordBound("у степені|в степені|до степеня|степінь|в степени|степень|to the power of|power"), " ^ ")
            .replace(wordBound("помножити на|множити на|помножити|умножить на|умножить|multiplied by|times"), " × ")
            .replace(wordBound("поділити на|розділити на|ділити на|поділити|ділити|разделить на|поделить на|делить на|divided by|over"), " ÷ ")
            .replace(wordBound("плюс|додати|прибавить|plus|add"), " + ")
            .replace(wordBound("мінус|відняти|минус|вычесть|minus|subtract"), " − ")
            .replace(wordBound("число пі|пі|пи|pi"), " π ")
            .replace(wordBound("півтора"), " 1.5 ")
            .replace(wordBound("відсотків від|відсотка від|відсоток від|процентів від|процента від|процент від|percent of"), " % × ")
            .replace(wordBound("відсотків|відсотка|відсоток|процентів|процента|процент|percent"), " % ")
            .replace(wordBound("факторіал|факториал|factorial"), " ! ")
            .replace(wordBound("цілих і|цілих|целых и|целых|кома|крапка|запятая|точка|point|dot"), " . ")
            .replace(wordBound("десятих|сотих|тисячних|десятых|сотых|тысячных"), " ")
            .replace(wordBound("корінь|корень"), " √( ")

        // Replace standalone spoken multiplication 'x' or 'на' between numbers/parens when speech recognizer emits 'x'
        s = s.replace(Regex("(?<=[0-9)πe%])\\s+(?:на|x|х)\\s+(?=[0-9(πe√])"), " × ")

        // Tokenize and assemble Ukrainian/Russian/English number words into numerals
        val tokens = tokenizeWordsAndSymbols(s)
        val assembled = assembleNumberWords(tokens)
        val joined = assembled.joinToString(" ")
            .replace(Regex("\\s+\\.\\s+"), ".")
            .replace(Regex("(?<=\\d)\\s+\\."), ".")
            .replace(Regex("\\.\\s+(?=\\d)"), ".")

        // Fix prefix factorial: "! 5" -> "5!"
        val factorialFixed = joined.replace(Regex("!\\s+(\\d+(?:\\.\\d+)?)"), "$1!")

        val normalizedRaw = normalizeSpokenExpression(factorialFixed) ?: return null

        val balancedRaw = buildString {
            append(normalizedRaw)
            val openCount = normalizedRaw.count { it == '(' }
            val closeCount = normalizedRaw.count { it == ')' }
            repeat((openCount - closeCount).coerceAtLeast(0)) {
                append(')')
            }
        }

        val evalPreview = when (val res = ExpressionEvaluator.evaluate(balancedRaw, angleUnit)) {
            is EvaluationResult.Success -> res.formatted.displayText
            else -> ""
        }

        return ParsedVoiceMath(
            rawExpression = balancedRaw,
            formattedExpression = NumberFormatter.formatExpressionForDisplay(balancedRaw),
            previewResult = evalPreview
        )
    }

    private fun normalizeSpokenExpression(raw: String): String? {
        val standardized = raw
            .replace('*', '×')
            .replace('/', '÷')
            .replace('-', '−')
            .replace(':', '÷')
            .replace(',', '.')

        val validTokenRegex = Regex("sin⁻¹\\(|cos⁻¹\\(|tan⁻¹\\(|sin\\(|cos\\(|tan\\(|log\\(|ln\\(|√\\(|\\d+(?:\\.\\d+)?|[+−×÷^()%!.]|π|(?<![\\p{L}])e(?![\\p{L}])")
        val cleaned = buildString {
            for (match in validTokenRegex.findAll(standardized)) {
                append(match.value)
            }
        }
        if (cleaned.isEmpty()) return null
        val withImplicitMult = cleaned.replace(
            Regex("(?<=[0-9)πe%!])(?=\\(|√\\(|sin⁻¹\\(|cos⁻¹\\(|tan⁻¹\\(|sin\\(|cos\\(|tan\\(|ln\\(|log\\()"),
            "×"
        )
        val hasOperand = withImplicitMult.any { it.isDigit() || it == 'π' || it == 'e' }
        return if (hasOperand) withImplicitMult else null
    }

    private fun tokenizeWordsAndSymbols(input: String): List<String> {
        val out = mutableListOf<String>()
        val regex = Regex("sin⁻¹\\(|cos⁻¹\\(|tan⁻¹\\(|sin\\(|cos\\(|tan\\(|log\\(|ln\\(|√\\(|\\d+(?:[.,]\\d+)?|[+−×÷^()%!.]|[a-zа-яіїєґ']+")
        for (match in regex.findAll(input)) {
            out.add(match.value)
        }
        return out
    }

    private fun assembleNumberWords(tokens: List<String>): List<String> {
        val result = mutableListOf<String>()
        var total = 0L
        var currentChunk = 0L
        var hasActiveNumberWord = false

        fun flushNumber() {
            if (hasActiveNumberWord) {
                result.add((total + currentChunk).toString())
                total = 0L
                currentChunk = 0L
                hasActiveNumberWord = false
            }
        }

        for (rawTok in tokens) {
            val tok = rawTok.replace(',', '.')
            when {
                tok in hundredsMap -> {
                    currentChunk += hundredsMap.getValue(tok)
                    hasActiveNumberWord = true
                }
                tok in tensMap -> {
                    currentChunk += tensMap.getValue(tok)
                    hasActiveNumberWord = true
                }
                tok in unitsMap -> {
                    currentChunk += unitsMap.getValue(tok)
                    hasActiveNumberWord = true
                }
                tok in scalesMap -> {
                    val scale = scalesMap.getValue(tok)
                    val mult = if (currentChunk == 0L) 1L else currentChunk
                    total += mult * scale
                    currentChunk = 0L
                    hasActiveNumberWord = true
                }
                tok == "і" || tok == "та" || tok == "и" || tok == "and" -> {
                    // Connector inside compound numbers ("сто і п'ять")
                    if (!hasActiveNumberWord) {
                        // ignore connector outside numbers
                    }
                }
                else -> {
                    flushNumber()
                    result.add(tok)
                }
            }
        }
        flushNumber()
        return result
    }
}
