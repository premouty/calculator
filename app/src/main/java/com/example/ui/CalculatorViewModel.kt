package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CameraMathAiAnalyzer
import com.example.audio.CalcSoundEngine
import com.example.audio.CalcSoundType
import com.example.audio.CalcVibrator
import com.example.data.local.CalculationRecord
import com.example.data.local.CalculatorDatabase
import com.example.data.preferences.AngleUnit
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.HistoryRepository
import com.example.domain.math.EvaluationResult
import com.example.domain.math.ExpressionEvaluator
import com.example.domain.math.FormattedNumber
import com.example.domain.math.MathErrorType
import com.example.domain.math.NumberFormatter
import com.example.domain.ocr.ScannedMathExpression
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "CalculatorViewModel"

sealed interface CameraScanAnalysisState {
    data object Idle : CameraScanAnalysisState
    data object Loading : CameraScanAnalysisState
    data class Success(
        val expressions: List<ScannedMathExpression>,
        val usedAi: Boolean
    ) : CameraScanAnalysisState
    data class Error(val message: String) : CameraScanAnalysisState
}

data class CalculatorUiState(
    val rawExpression: String = "",
    val formattedExpression: String = "",
    val livePreview: String = "",
    val livePreviewPlain: String = "",
    val lastCompletedExpression: String = "",
    val activeResultMeta: FormattedNumber? = null,
    val isResultDisplayed: Boolean = false,
    val errorType: MathErrorType? = null,
    val errorTriggerId: Long = 0L,
    val isScientificExpanded: Boolean = false,
    val isInverseMode: Boolean = false,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isTopHistoryOpen: Boolean = false,
    val isFullHistorySheetOpen: Boolean = false,
    val isCameraScannerOpen: Boolean = false,
    val isVoiceInputDialogOpen: Boolean = false,
    val cameraScanAnalysisState: CameraScanAnalysisState = CameraScanAnalysisState.Idle,
    val bigNumberViewerRecord: Pair<String, String>? = null, // title/expression to full plain digits
    val history: List<CalculationRecord> = emptyList()
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CalculatorDatabase.getInstance(application)
    private val historyRepository = HistoryRepository(database.calculationDao())
    private val prefsRepository = UserPreferencesRepository(application)
    private val soundEngine = CalcSoundEngine(application)
    private val calcVibrator = CalcVibrator(application)
    private val cameraMathAiAnalyzer = CameraMathAiAnalyzer()

    private val _state = MutableStateFlow(CalculatorUiState())
    private val _rawExpression = MutableStateFlow("")
    private val _optimisticThemeMode = MutableStateFlow<ThemeMode?>(null)
    private var evalJob: Job? = null
    private var cameraAiJob: Job? = null

    val uiState: StateFlow<CalculatorUiState> = combine(
        _state,
        historyRepository.historyStream,
        prefsRepository.preferencesFlow,
        _optimisticThemeMode
    ) { state, history, prefs, optimisticTheme ->
        state.copy(
            history = history,
            themeMode = optimisticTheme ?: prefs.themeMode,
            angleUnit = prefs.angleUnit,
            isScientificExpanded = prefs.isScientificExpanded,
            isSoundEnabled = prefs.isSoundEnabled,
            isVibrationEnabled = prefs.isVibrationEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalculatorUiState()
    )

    fun playSound(type: CalcSoundType) {
        soundEngine.play(type, uiState.value.isSoundEnabled)
    }

    fun performHaptic(type: CalcSoundType, view: View? = null) {
        calcVibrator.vibrate(type, uiState.value.isVibrationEnabled, view)
    }

    override fun onCleared() {
        soundEngine.release()
        super.onCleared()
    }

    private val functionTokens = listOf(
        "sin⁻¹(", "cos⁻¹(", "tan⁻¹(",
        "sin(", "cos(", "tan(",
        "ln(", "log(", "√("
    )

    fun onDigit(digit: String) {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        val updated = if (digit == "0") {
            val token = currentNumberToken(current)
            if (token == "0") current else current + digit
        } else {
            val token = currentNumberToken(current)
            if (token == "0") current.dropLast(1) + digit else current + digit
        }
        updateExpression(updated, isResult = false)
    }

    fun onDecimalPoint() {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        val token = currentNumberToken(current)
        if (token.contains('.')) return

        val last = current.lastOrNull()
        val updated = when {
            current.isEmpty() || last == '(' || isBinaryOp(last) -> current + "0."
            last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!' -> current + "×0."
            else -> current + "."
        }
        updateExpression(updated, isResult = false)
    }

    fun onOperator(op: String) {
        val state = _state.value
        val current = if (state.isResultDisplayed) {
            state.activeResultMeta?.plainText ?: _rawExpression.value
        } else {
            _rawExpression.value
        }

        if (current.isEmpty()) {
            if (op == "−") {
                updateExpression("−", isResult = false)
            }
            return
        }

        val last = current.last()
        val updated = when {
            last == '(' -> if (op == "−") current + op else current
            current == "−" && op != "−" -> ""
            isBinaryOp(last) -> {
                if (op == "−" && (last == '×' || last == '÷' || last == '^')) {
                    current + op
                } else {
                    // Replace trailing operator(s)
                    val trimmedOps = current.dropLastWhile { isBinaryOp(it) }
                    if (trimmedOps.isEmpty() || trimmedOps.last() == '(') {
                        if (op == "−") trimmedOps + op else trimmedOps
                    } else {
                        trimmedOps + op
                    }
                }
            }
            else -> current + op
        }
        updateExpression(updated, isResult = false)
    }

    fun onPercent() {
        val state = _state.value
        val current = if (state.isResultDisplayed) {
            state.activeResultMeta?.plainText ?: _rawExpression.value
        } else {
            _rawExpression.value
        }
        val last = current.lastOrNull() ?: return
        if (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '!') {
            updateExpression(current + "%", isResult = false)
        }
    }

    fun onFactorial() {
        val state = _state.value
        val current = if (state.isResultDisplayed) {
            state.activeResultMeta?.plainText ?: _rawExpression.value
        } else {
            _rawExpression.value
        }
        val last = current.lastOrNull() ?: return
        if (last.isDigit() || last == ')' || last == '!') {
            updateExpression(current + "!", isResult = false)
        }
    }

    fun onSmartParenthesis() {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        val openCount = current.count { it == '(' }
        val closeCount = current.count { it == ')' }
        val last = current.lastOrNull()

        val shouldClose = openCount > closeCount && last != null &&
            (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!')
        val nextChar = if (shouldClose) ")" else "("
        updateExpression(current + nextChar, isResult = false)
    }

    fun onOpenParenthesis() {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        updateExpression(current + "(", isResult = false)
    }

    fun onCloseParenthesis() {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        val openCount = current.count { it == '(' }
        val closeCount = current.count { it == ')' }
        val last = current.lastOrNull()
        if (openCount > closeCount && last != null && last != '(' && !isBinaryOp(last)) {
            updateExpression(current + ")", isResult = false)
        }
    }

    fun onConstant(symbol: String) {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        updateExpression(current + symbol, isResult = false)
    }

    fun onFunction(funcPrefix: String) {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        updateExpression(current + funcPrefix, isResult = false)
    }

    fun onSquare() {
        val state = _state.value
        val current = if (state.isResultDisplayed) {
            state.activeResultMeta?.plainText ?: _rawExpression.value
        } else {
            _rawExpression.value
        }
        val last = current.lastOrNull() ?: return
        if (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '!') {
            updateExpression(current + "^2", isResult = false)
        }
    }

    fun onExp10() {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        updateExpression(current + "10^(", isResult = false)
    }

    fun onExpE() {
        val current = if (_state.value.isResultDisplayed) "" else _rawExpression.value
        updateExpression(current + "e^(", isResult = false)
    }

    fun onToggleSign() {
        val state = _state.value
        val current = if (state.isResultDisplayed) {
            state.activeResultMeta?.plainText ?: _rawExpression.value
        } else {
            _rawExpression.value
        }
        if (current.isEmpty()) {
            updateExpression("−", isResult = false)
            return
        }
        if (current.startsWith("−") && current.drop(1).all { it.isDigit() || it == '.' }) {
            updateExpression(current.drop(1), isResult = false)
            return
        }
        if (current.all { it.isDigit() || it == '.' }) {
            updateExpression("−$current", isResult = false)
            return
        }
        // Toggle (− at the end or wrap trailing number
        if (current.endsWith("(−")) {
            updateExpression(current.dropLast(2), isResult = false)
        } else {
            updateExpression("$current(−", isResult = false)
        }
    }

    fun onBackspace() {
        val state = _state.value
        if (state.isResultDisplayed) {
            onAllClear()
            return
        }
        val current = _rawExpression.value
        if (current.isEmpty()) return

        val matchedFunc = functionTokens.firstOrNull { current.endsWith(it) }
        val updated = if (matchedFunc != null) {
            current.dropLast(matchedFunc.length)
        } else {
            current.dropLast(1)
        }
        updateExpression(updated, isResult = false)
    }

    fun onAllClear() {
        evalJob?.cancel()
        _rawExpression.value = ""
        _state.update {
            it.copy(
                rawExpression = "",
                formattedExpression = "",
                livePreview = "",
                livePreviewPlain = "",
                lastCompletedExpression = "",
                activeResultMeta = null,
                isResultDisplayed = false,
                errorType = null
            )
        }
    }

    fun onEquals() {
        val current = _rawExpression.value.trim()
        if (current.isEmpty()) return
        val angle = uiState.value.angleUnit

        evalJob?.cancel()
        evalJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                ExpressionEvaluator.evaluate(current, angle)
            }
            when (result) {
                is EvaluationResult.Idle -> Unit
                is EvaluationResult.Error -> {
                    _state.update {
                        it.copy(
                            errorType = result.errorType,
                            errorTriggerId = it.errorTriggerId + 1L
                        )
                    }
                }
                is EvaluationResult.Success -> {
                    val formattedExpr = NumberFormatter.formatExpressionForDisplay(current)
                    val record = CalculationRecord(
                        expression = formattedExpr,
                        plainResult = result.formatted.plainText,
                        formattedResult = result.formatted.displayText,
                        isScientificNotation = result.formatted.isScientificNotation,
                        digitCount = result.formatted.digitCount
                    )
                    historyRepository.saveCalculation(record)
                    _rawExpression.value = result.formatted.plainText
                    _state.update {
                        it.copy(
                            rawExpression = result.formatted.plainText,
                            formattedExpression = result.formatted.displayText,
                            livePreview = "",
                            livePreviewPlain = "",
                            lastCompletedExpression = formattedExpr,
                            activeResultMeta = result.formatted,
                            isResultDisplayed = true,
                            errorType = null
                        )
                    }
                }
            }
        }
    }

    fun toggleScientificExpanded() {
        val next = !uiState.value.isScientificExpanded
        viewModelScope.launch {
            prefsRepository.setScientificExpanded(next)
        }
    }

    fun toggleInverseMode() {
        _state.update { it.copy(isInverseMode = !it.isInverseMode) }
    }

    fun toggleAngleUnit() {
        val next = if (uiState.value.angleUnit == AngleUnit.DEG) AngleUnit.RAD else AngleUnit.DEG
        viewModelScope.launch {
            prefsRepository.setAngleUnit(next)
            recalculateLivePreview(_rawExpression.value, next)
        }
    }

    fun cycleThemeMode() {
        val next = when (uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    fun toggleSoundEnabled() {
        val next = !uiState.value.isSoundEnabled
        viewModelScope.launch {
            prefsRepository.setSoundEnabled(next)
        }
    }

    fun toggleVibrationEnabled(view: View? = null) {
        val next = !uiState.value.isVibrationEnabled
        if (next) {
            calcVibrator.vibrate(CalcSoundType.OPERATOR, enabled = true, view = view)
        }
        viewModelScope.launch {
            prefsRepository.setVibrationEnabled(next)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _optimisticThemeMode.value = mode
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
            _optimisticThemeMode.compareAndSet(mode, null)
        }
    }

    fun setTopHistoryOpen(open: Boolean) {
        _state.update { it.copy(isTopHistoryOpen = open) }
    }

    fun toggleTopHistory() {
        _state.update { it.copy(isTopHistoryOpen = !it.isTopHistoryOpen) }
    }

    fun setFullHistorySheetOpen(open: Boolean) {
        _state.update {
            it.copy(
                isFullHistorySheetOpen = open,
                isTopHistoryOpen = if (open) false else it.isTopHistoryOpen,
                isVoiceInputDialogOpen = if (open) false else it.isVoiceInputDialogOpen
            )
        }
    }

    fun setVoiceInputDialogOpen(open: Boolean) {
        _state.update {
            it.copy(
                isVoiceInputDialogOpen = open,
                isTopHistoryOpen = if (open) false else it.isTopHistoryOpen,
                isFullHistorySheetOpen = if (open) false else it.isFullHistorySheetOpen
            )
        }
    }

    fun applyVoiceExpression(rawExpr: String, evaluateImmediately: Boolean) {
        val cleaned = rawExpr.trim()
        if (cleaned.isEmpty()) return
        _state.update {
            it.copy(
                isVoiceInputDialogOpen = false,
                lastCompletedExpression = ""
            )
        }
        updateExpression(cleaned, isResult = false)
        if (evaluateImmediately) {
            onEquals()
        }
    }

    fun setCameraScannerOpen(open: Boolean) {
        if (!open) {
            cameraAiJob?.cancel()
        }
        _state.update {
            it.copy(
                isCameraScannerOpen = open,
                cameraScanAnalysisState = if (!open) CameraScanAnalysisState.Idle else it.cameraScanAnalysisState,
                isTopHistoryOpen = if (open) false else it.isTopHistoryOpen,
                isFullHistorySheetOpen = if (open) false else it.isFullHistorySheetOpen,
                isVoiceInputDialogOpen = if (open) false else it.isVoiceInputDialogOpen
            )
        }
    }

    fun analyzeCameraPhotoWithAi(bitmap: Bitmap) {
        val angle = uiState.value.angleUnit
        cameraAiJob?.cancel()
        cameraAiJob = viewModelScope.launch {
            _state.update { it.copy(cameraScanAnalysisState = CameraScanAnalysisState.Loading) }
            try {
                val results = withTimeout(60_000L) {
                    cameraMathAiAnalyzer.analyzePhoto(bitmap, angle)
                }
                _state.update {
                    it.copy(
                        cameraScanAnalysisState = CameraScanAnalysisState.Success(
                            expressions = results,
                            usedAi = true
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI math photo analysis timed out", e)
                _state.update {
                    it.copy(
                        cameraScanAnalysisState = CameraScanAnalysisState.Error(
                            "ШІ-аналіз тривав довше 60 секунд. Спробуйте ще раз або оберіть офлайн-скан."
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "AI math photo analysis failed", t)
                _state.update {
                    it.copy(
                        cameraScanAnalysisState = CameraScanAnalysisState.Error(
                            t.localizedMessage ?: "Не вдалося виконати ШІ-аналіз фото."
                        )
                    )
                }
            }
        }
    }

    fun setCameraScanOfflineResult(expressions: List<ScannedMathExpression>) {
        cameraAiJob?.cancel()
        _state.update {
            it.copy(
                cameraScanAnalysisState = CameraScanAnalysisState.Success(
                    expressions = expressions,
                    usedAi = false
                )
            )
        }
    }

    fun resetCameraScanAnalysis() {
        cameraAiJob?.cancel()
        _state.update {
            it.copy(cameraScanAnalysisState = CameraScanAnalysisState.Idle)
        }
    }

    fun applyScannedExpression(rawExpr: String) {
        val cleaned = rawExpr.trim()
        if (cleaned.isEmpty()) return
        cameraAiJob?.cancel()
        _state.update {
            it.copy(
                isCameraScannerOpen = false,
                cameraScanAnalysisState = CameraScanAnalysisState.Idle,
                lastCompletedExpression = ""
            )
        }
        updateExpression(cleaned, isResult = false)
    }

    fun openBigNumberViewer(expressionLabel: String, plainDigits: String) {
        _state.update { it.copy(bigNumberViewerRecord = expressionLabel to plainDigits) }
    }

    fun closeBigNumberViewer() {
        _state.update { it.copy(bigNumberViewerRecord = null) }
    }

    fun useHistoryExpression(record: CalculationRecord) {
        val cleanExpr = record.expression.replace("\u2009", "")
        _state.update {
            it.copy(
                isTopHistoryOpen = false,
                isFullHistorySheetOpen = false,
                lastCompletedExpression = ""
            )
        }
        updateExpression(cleanExpr, isResult = false)
    }

    fun useHistoryResult(record: CalculationRecord) {
        val state = _state.value
        val current = if (state.isResultDisplayed) "" else _rawExpression.value
        val last = current.lastOrNull()
        val updated = if (current.isEmpty() || last == '(' || isBinaryOp(last)) {
            current + record.plainResult
        } else {
            current + "×" + record.plainResult
        }
        _state.update {
            it.copy(
                isTopHistoryOpen = false,
                isFullHistorySheetOpen = false
            )
        }
        updateExpression(updated, isResult = false)
    }

    fun deleteHistoryRecord(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteRecord(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    private fun updateExpression(newRaw: String, isResult: Boolean) {
        _rawExpression.value = newRaw
        val formatted = NumberFormatter.formatExpressionForDisplay(newRaw)
        val angle = uiState.value.angleUnit

        if (newRaw.isEmpty()) {
            evalJob?.cancel()
            _state.update {
                it.copy(
                    rawExpression = "",
                    formattedExpression = "",
                    livePreview = "",
                    livePreviewPlain = "",
                    isResultDisplayed = isResult,
                    activeResultMeta = if (isResult) it.activeResultMeta else null,
                    errorType = null,
                    lastCompletedExpression = ""
                )
            }
            return
        }

        if (!isHeavyExpression(newRaw)) {
            evalJob?.cancel()
            val (preview, previewPlain) = when (val res = ExpressionEvaluator.evaluate(newRaw, angle)) {
                is EvaluationResult.Success -> res.formatted.displayText to res.formatted.plainText
                else -> "" to ""
            }
            _state.update {
                it.copy(
                    rawExpression = newRaw,
                    formattedExpression = formatted,
                    livePreview = preview,
                    livePreviewPlain = previewPlain,
                    isResultDisplayed = isResult,
                    activeResultMeta = if (isResult) it.activeResultMeta else null,
                    errorType = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    rawExpression = newRaw,
                    formattedExpression = formatted,
                    isResultDisplayed = isResult,
                    activeResultMeta = if (isResult) it.activeResultMeta else null,
                    errorType = null
                )
            }
            recalculateLivePreview(newRaw, angle)
        }
    }

    private fun recalculateLivePreview(expr: String, angleUnit: AngleUnit) {
        evalJob?.cancel()
        if (expr.isEmpty()) {
            _state.update { it.copy(livePreview = "", livePreviewPlain = "") }
            return
        }
        if (!isHeavyExpression(expr)) {
            val (preview, previewPlain) = when (val res = ExpressionEvaluator.evaluate(expr, angleUnit)) {
                is EvaluationResult.Success -> res.formatted.displayText to res.formatted.plainText
                else -> "" to ""
            }
            _state.update { it.copy(livePreview = preview, livePreviewPlain = previewPlain) }
            return
        }
        evalJob = viewModelScope.launch {
            val res = withContext(Dispatchers.Default) {
                ExpressionEvaluator.evaluate(expr, angleUnit)
            }
            val (preview, previewPlain) = when (res) {
                is EvaluationResult.Success -> res.formatted.displayText to res.formatted.plainText
                else -> "" to ""
            }
            _state.update { it.copy(livePreview = preview, livePreviewPlain = previewPlain) }
        }
    }

    private fun isHeavyExpression(expr: String): Boolean {
        return expr.length > 24 ||
            expr.contains('!') ||
            expr.contains('^') ||
            expr.any { it.isLetter() }
    }

    private fun currentNumberToken(expr: String): String {
        val sb = StringBuilder()
        for (i in expr.length - 1 downTo 0) {
            val c = expr[i]
            if (c.isDigit() || c == '.') sb.append(c) else break
        }
        return sb.reverse().toString()
    }

    private fun isBinaryOp(ch: Char?): Boolean {
        return ch == '+' || ch == '−' || ch == '-' || ch == '×' || ch == '*' || ch == '÷' || ch == '/' || ch == '^'
    }
}
