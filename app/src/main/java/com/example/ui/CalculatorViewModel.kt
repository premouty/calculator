package com.example.ui

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CalcSoundEngine
import com.example.audio.CalcSoundType
import com.example.audio.CalcVibrator
import com.example.audio.DEFAULT_HAPTIC_DURATION_MS
import com.example.audio.DEFAULT_HAPTIC_STRENGTH
import com.example.audio.HapticIntensity
import com.example.audio.HapticStyle
import com.example.data.local.CalculationRecord
import com.example.data.local.CalculatorDatabase
import com.example.data.preferences.AngleUnit
import com.example.data.preferences.ButtonShapeStyle
import com.example.data.preferences.DEFAULT_SOUND_VOLUME
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.HistoryRepository
import com.example.domain.math.EvaluationResult
import com.example.domain.math.ExpressionEvaluator
import com.example.domain.math.FormattedNumber
import com.example.domain.math.MathErrorType
import com.example.domain.math.NumberFormatter
import com.example.ui.theme.DEFAULT_CUSTOM_ACCENT_ARGB
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FeedbackPermissionTarget {
    SOUND,
    VIBRATION,
    BOTH
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
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isSoundEnabled: Boolean = false,
    val isVibrationEnabled: Boolean = false,
    val isFeedbackPermissionGranted: Boolean = false,
    val soundVolume: Float = DEFAULT_SOUND_VOLUME,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val hapticStyle: HapticStyle = HapticStyle.ADAPTIVE,
    val hapticStrength: Float = DEFAULT_HAPTIC_STRENGTH,
    val hapticDurationMs: Int = DEFAULT_HAPTIC_DURATION_MS,
    val hapticAccentEquals: Boolean = true,
    val buttonShapeStyle: ButtonShapeStyle = ButtonShapeStyle.PILL,
    val useDynamicColor: Boolean = true,
    val customAccentArgb: Long = DEFAULT_CUSTOM_ACCENT_ARGB,
    val showLivePreview: Boolean = true,
    val ambientDisplayGlow: Boolean = false,
    val pendingFeedbackPermission: FeedbackPermissionTarget? = null,
    val isTopHistoryOpen: Boolean = false,
    val isFullHistorySheetOpen: Boolean = false,
    val isVoiceInputDialogOpen: Boolean = false,
    val isSettingsSheetOpen: Boolean = false,
    val bigNumberViewerRecord: Pair<String, String>? = null, // title/expression to full plain digits
    val history: List<CalculationRecord> = emptyList()
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CalculatorDatabase.getInstance(application)
    private val historyRepository = HistoryRepository(database.calculationDao())
    private val prefsRepository = UserPreferencesRepository(application)
    private val soundEngine = CalcSoundEngine(application)
    private val calcVibrator = CalcVibrator(application)

    private val initialPrefs = prefsRepository.readInitialPreferencesSync()
    private val _optimisticPrefs = MutableStateFlow(initialPrefs)
    private var hasUserModifiedTheme = false
    private var hasUserModifiedAngle = false
    private var hasUserModifiedScientific = false
    private var hasUserModifiedSound = false
    private var hasUserModifiedVibration = false
    private var hasUserModifiedFeedbackPermission = false
    private var hasUserModifiedSoundVolume = false
    private var hasUserModifiedHapticIntensity = false
    private var hasUserModifiedHapticStyle = false
    private var hasUserModifiedHapticStrength = false
    private var hasUserModifiedHapticDuration = false
    private var hasUserModifiedHapticAccentEquals = false
    private var hasUserModifiedButtonShape = false
    private var hasUserModifiedDynamicColor = false
    private var hasUserModifiedCustomAccent = false
    private var hasUserModifiedLivePreview = false
    private var hasUserModifiedAmbientGlow = false

    private val _state = MutableStateFlow(
        CalculatorUiState(
            themeMode = initialPrefs.themeMode,
            angleUnit = initialPrefs.angleUnit,
            isScientificExpanded = initialPrefs.isScientificExpanded,
            isSoundEnabled = initialPrefs.isSoundEnabled,
            isVibrationEnabled = initialPrefs.isVibrationEnabled,
            isFeedbackPermissionGranted = initialPrefs.isFeedbackPermissionGranted,
            soundVolume = initialPrefs.soundVolume,
            hapticIntensity = initialPrefs.hapticIntensity,
            hapticStyle = initialPrefs.hapticStyle,
            hapticStrength = initialPrefs.hapticStrength,
            hapticDurationMs = initialPrefs.hapticDurationMs,
            hapticAccentEquals = initialPrefs.hapticAccentEquals,
            buttonShapeStyle = initialPrefs.buttonShapeStyle,
            useDynamicColor = initialPrefs.useDynamicColor,
            customAccentArgb = initialPrefs.customAccentArgb,
            showLivePreview = initialPrefs.showLivePreview,
            ambientDisplayGlow = initialPrefs.ambientDisplayGlow
        )
    )
    private val _rawExpression = MutableStateFlow("")
    private var evalJob: Job? = null

    init {
        viewModelScope.launch {
            prefsRepository.preferencesFlow.collect { dsPrefs ->
                _optimisticPrefs.update { current ->
                    current.copy(
                        themeMode = if (hasUserModifiedTheme) current.themeMode else dsPrefs.themeMode,
                        angleUnit = if (hasUserModifiedAngle) current.angleUnit else dsPrefs.angleUnit,
                        isScientificExpanded = if (hasUserModifiedScientific) current.isScientificExpanded else dsPrefs.isScientificExpanded,
                        isSoundEnabled = if (hasUserModifiedSound) current.isSoundEnabled else dsPrefs.isSoundEnabled,
                        isVibrationEnabled = if (hasUserModifiedVibration) current.isVibrationEnabled else dsPrefs.isVibrationEnabled,
                        isFeedbackPermissionGranted = if (hasUserModifiedFeedbackPermission) {
                            current.isFeedbackPermissionGranted
                        } else {
                            dsPrefs.isFeedbackPermissionGranted
                        },
                        soundVolume = if (hasUserModifiedSoundVolume) current.soundVolume else dsPrefs.soundVolume,
                        hapticIntensity = if (hasUserModifiedHapticIntensity) current.hapticIntensity else dsPrefs.hapticIntensity,
                        hapticStyle = if (hasUserModifiedHapticStyle) current.hapticStyle else dsPrefs.hapticStyle,
                        hapticStrength = if (hasUserModifiedHapticStrength) current.hapticStrength else dsPrefs.hapticStrength,
                        hapticDurationMs = if (hasUserModifiedHapticDuration) current.hapticDurationMs else dsPrefs.hapticDurationMs,
                        hapticAccentEquals = if (hasUserModifiedHapticAccentEquals) current.hapticAccentEquals else dsPrefs.hapticAccentEquals,
                        buttonShapeStyle = if (hasUserModifiedButtonShape) current.buttonShapeStyle else dsPrefs.buttonShapeStyle,
                        useDynamicColor = if (hasUserModifiedDynamicColor) current.useDynamicColor else dsPrefs.useDynamicColor,
                        customAccentArgb = if (hasUserModifiedCustomAccent) current.customAccentArgb else dsPrefs.customAccentArgb,
                        showLivePreview = if (hasUserModifiedLivePreview) current.showLivePreview else dsPrefs.showLivePreview,
                        ambientDisplayGlow = if (hasUserModifiedAmbientGlow) current.ambientDisplayGlow else dsPrefs.ambientDisplayGlow
                    )
                }
            }
        }
    }

    val uiState: StateFlow<CalculatorUiState> = combine(
        _state,
        historyRepository.historyStream,
        _optimisticPrefs
    ) { state, history, prefs ->
        state.copy(
            history = history,
            themeMode = prefs.themeMode,
            angleUnit = prefs.angleUnit,
            isScientificExpanded = prefs.isScientificExpanded,
            isSoundEnabled = prefs.isSoundEnabled,
            isVibrationEnabled = prefs.isVibrationEnabled,
            isFeedbackPermissionGranted = prefs.isFeedbackPermissionGranted,
            soundVolume = prefs.soundVolume,
            hapticIntensity = prefs.hapticIntensity,
            hapticStyle = prefs.hapticStyle,
            hapticStrength = prefs.hapticStrength,
            hapticDurationMs = prefs.hapticDurationMs,
            hapticAccentEquals = prefs.hapticAccentEquals,
            buttonShapeStyle = prefs.buttonShapeStyle,
            useDynamicColor = prefs.useDynamicColor,
            customAccentArgb = prefs.customAccentArgb,
            showLivePreview = prefs.showLivePreview,
            ambientDisplayGlow = prefs.ambientDisplayGlow
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalculatorUiState(
            themeMode = initialPrefs.themeMode,
            angleUnit = initialPrefs.angleUnit,
            isScientificExpanded = initialPrefs.isScientificExpanded,
            isSoundEnabled = initialPrefs.isSoundEnabled,
            isVibrationEnabled = initialPrefs.isVibrationEnabled,
            isFeedbackPermissionGranted = initialPrefs.isFeedbackPermissionGranted,
            soundVolume = initialPrefs.soundVolume,
            hapticIntensity = initialPrefs.hapticIntensity,
            hapticStyle = initialPrefs.hapticStyle,
            hapticStrength = initialPrefs.hapticStrength,
            hapticDurationMs = initialPrefs.hapticDurationMs,
            hapticAccentEquals = initialPrefs.hapticAccentEquals,
            buttonShapeStyle = initialPrefs.buttonShapeStyle,
            useDynamicColor = initialPrefs.useDynamicColor,
            customAccentArgb = initialPrefs.customAccentArgb,
            showLivePreview = initialPrefs.showLivePreview,
            ambientDisplayGlow = initialPrefs.ambientDisplayGlow
        )
    )

    fun playSound(type: CalcSoundType) {
        val prefs = _optimisticPrefs.value
        soundEngine.play(type, prefs.isSoundEnabled, prefs.soundVolume)
    }

    fun performHaptic(type: CalcSoundType, view: View? = null) {
        val prefs = _optimisticPrefs.value
        calcVibrator.vibrate(
            type = type,
            enabled = prefs.isVibrationEnabled,
            intensity = prefs.hapticIntensity,
            style = prefs.hapticStyle,
            strength = prefs.hapticStrength,
            durationMs = prefs.hapticDurationMs,
            accentEquals = prefs.hapticAccentEquals,
            view = view
        )
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
        val angle = _optimisticPrefs.value.angleUnit

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
        hasUserModifiedScientific = true
        val next = !_optimisticPrefs.value.isScientificExpanded
        _optimisticPrefs.update { it.copy(isScientificExpanded = next) }
        viewModelScope.launch {
            prefsRepository.setScientificExpanded(next)
        }
    }

    fun toggleInverseMode() {
        _state.update { it.copy(isInverseMode = !it.isInverseMode) }
    }

    fun toggleAngleUnit() {
        hasUserModifiedAngle = true
        val next = if (_optimisticPrefs.value.angleUnit == AngleUnit.DEG) AngleUnit.RAD else AngleUnit.DEG
        _optimisticPrefs.update { it.copy(angleUnit = next) }
        viewModelScope.launch {
            prefsRepository.setAngleUnit(next)
            recalculateLivePreview(_rawExpression.value, next)
        }
    }

    fun cycleThemeMode() {
        val next = when (_optimisticPrefs.value.themeMode) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
        }
        setThemeMode(next)
    }

    fun toggleSoundEnabled() {
        val current = _optimisticPrefs.value
        val next = !current.isSoundEnabled
        if (next && !current.isFeedbackPermissionGranted) {
            _state.update { it.copy(pendingFeedbackPermission = FeedbackPermissionTarget.SOUND) }
            return
        }
        hasUserModifiedSound = true
        _optimisticPrefs.update { it.copy(isSoundEnabled = next) }
        if (next) {
            soundEngine.play(CalcSoundType.UTILITY, enabled = true, volume = current.soundVolume)
        }
        viewModelScope.launch {
            prefsRepository.setSoundEnabled(next)
        }
    }

    fun toggleVibrationEnabled(view: View? = null) {
        val current = _optimisticPrefs.value
        val next = !current.isVibrationEnabled
        if (next && !current.isFeedbackPermissionGranted) {
            _state.update { it.copy(pendingFeedbackPermission = FeedbackPermissionTarget.VIBRATION) }
            return
        }
        hasUserModifiedVibration = true
        _optimisticPrefs.update { it.copy(isVibrationEnabled = next) }
        if (next) {
            calcVibrator.vibrate(
                type = CalcSoundType.OPERATOR,
                enabled = true,
                intensity = current.hapticIntensity,
                style = current.hapticStyle,
                strength = current.hapticStrength,
                durationMs = current.hapticDurationMs,
                accentEquals = current.hapticAccentEquals,
                view = view
            )
        }
        viewModelScope.launch {
            prefsRepository.setVibrationEnabled(next)
        }
    }

    fun setFeedbackEnabled(soundEnabled: Boolean, vibrationEnabled: Boolean, view: View? = null) {
        val current = _optimisticPrefs.value
        if ((soundEnabled || vibrationEnabled) && !current.isFeedbackPermissionGranted) {
            val target = when {
                soundEnabled && vibrationEnabled -> FeedbackPermissionTarget.BOTH
                soundEnabled -> FeedbackPermissionTarget.SOUND
                else -> FeedbackPermissionTarget.VIBRATION
            }
            _state.update { it.copy(pendingFeedbackPermission = target) }
            return
        }
        hasUserModifiedSound = true
        hasUserModifiedVibration = true
        _optimisticPrefs.update {
            it.copy(
                isSoundEnabled = soundEnabled,
                isVibrationEnabled = vibrationEnabled
            )
        }
        if (soundEnabled) {
            soundEngine.play(CalcSoundType.UTILITY, enabled = true, volume = current.soundVolume)
        }
        if (vibrationEnabled) {
            calcVibrator.vibrate(
                type = CalcSoundType.OPERATOR,
                enabled = true,
                intensity = current.hapticIntensity,
                style = current.hapticStyle,
                strength = current.hapticStrength,
                durationMs = current.hapticDurationMs,
                accentEquals = current.hapticAccentEquals,
                view = view
            )
        }
        viewModelScope.launch {
            prefsRepository.setFeedbackEnabled(soundEnabled, vibrationEnabled)
        }
    }

    fun confirmFeedbackPermission(view: View? = null) {
        val target = _state.value.pendingFeedbackPermission ?: FeedbackPermissionTarget.BOTH
        val current = _optimisticPrefs.value
        val enableSound = current.isSoundEnabled ||
            target == FeedbackPermissionTarget.SOUND ||
            target == FeedbackPermissionTarget.BOTH
        val enableVibration = current.isVibrationEnabled ||
            target == FeedbackPermissionTarget.VIBRATION ||
            target == FeedbackPermissionTarget.BOTH

        hasUserModifiedFeedbackPermission = true
        hasUserModifiedSound = true
        hasUserModifiedVibration = true

        _state.update { it.copy(pendingFeedbackPermission = null) }
        _optimisticPrefs.update {
            it.copy(
                isFeedbackPermissionGranted = true,
                isSoundEnabled = enableSound,
                isVibrationEnabled = enableVibration
            )
        }

        if (enableSound) {
            soundEngine.play(CalcSoundType.UTILITY, enabled = true, volume = current.soundVolume)
        }
        if (enableVibration) {
            calcVibrator.vibrate(
                type = CalcSoundType.OPERATOR,
                enabled = true,
                intensity = current.hapticIntensity,
                style = current.hapticStyle,
                strength = current.hapticStrength,
                durationMs = current.hapticDurationMs,
                accentEquals = current.hapticAccentEquals,
                view = view
            )
        }

        viewModelScope.launch {
            prefsRepository.grantFeedbackPermissionAndEnable(
                soundEnabled = enableSound,
                vibrationEnabled = enableVibration
            )
        }
    }

    fun dismissFeedbackPermission() {
        _state.update { it.copy(pendingFeedbackPermission = null) }
    }

    fun setThemeMode(mode: ThemeMode) {
        hasUserModifiedTheme = true
        _optimisticPrefs.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
        }
    }

    fun setSoundVolume(volume: Float) {
        val clamped = volume.coerceIn(0.05f, 1.0f)
        hasUserModifiedSoundVolume = true
        _optimisticPrefs.update { it.copy(soundVolume = clamped) }
        viewModelScope.launch {
            prefsRepository.setSoundVolume(clamped)
        }
    }

    fun previewSoundVolume() {
        val prefs = _optimisticPrefs.value
        if (prefs.isSoundEnabled) {
            soundEngine.play(CalcSoundType.DIGIT, enabled = true, volume = prefs.soundVolume)
        }
    }

    fun setHapticIntensity(intensity: HapticIntensity, view: View? = null) {
        val (presetStrength, presetDuration) = when (intensity) {
            HapticIntensity.LIGHT -> 0.45f to 8
            HapticIntensity.MEDIUM -> 0.70f to 12
            HapticIntensity.STRONG -> 1.0f to 20
        }
        hasUserModifiedHapticIntensity = true
        hasUserModifiedHapticStrength = true
        hasUserModifiedHapticDuration = true
        _optimisticPrefs.update {
            it.copy(
                hapticIntensity = intensity,
                hapticStrength = presetStrength,
                hapticDurationMs = presetDuration
            )
        }
        val prefs = _optimisticPrefs.value
        if (prefs.isVibrationEnabled) {
            calcVibrator.vibrate(
                type = CalcSoundType.OPERATOR,
                enabled = true,
                intensity = intensity,
                style = prefs.hapticStyle,
                strength = presetStrength,
                durationMs = presetDuration,
                accentEquals = prefs.hapticAccentEquals,
                view = view
            )
        }
        viewModelScope.launch {
            prefsRepository.setHapticPreset(intensity, presetStrength, presetDuration)
        }
    }

    fun setHapticStyle(style: HapticStyle, view: View? = null) {
        hasUserModifiedHapticStyle = true
        _optimisticPrefs.update { it.copy(hapticStyle = style) }
        val prefs = _optimisticPrefs.value
        if (prefs.isVibrationEnabled) {
            calcVibrator.vibrate(
                type = CalcSoundType.OPERATOR,
                enabled = true,
                intensity = prefs.hapticIntensity,
                style = style,
                strength = prefs.hapticStrength,
                durationMs = prefs.hapticDurationMs,
                accentEquals = prefs.hapticAccentEquals,
                view = view
            )
        }
        viewModelScope.launch {
            prefsRepository.setHapticStyle(style)
        }
    }

    fun setHapticStrength(strength: Float) {
        val clamped = strength.coerceIn(0.15f, 1.0f)
        hasUserModifiedHapticStrength = true
        _optimisticPrefs.update { it.copy(hapticStrength = clamped) }
        viewModelScope.launch {
            prefsRepository.setHapticStrength(clamped)
        }
    }

    fun setHapticDurationMs(durationMs: Int) {
        val clamped = durationMs.coerceIn(4, 40)
        hasUserModifiedHapticDuration = true
        _optimisticPrefs.update { it.copy(hapticDurationMs = clamped) }
        viewModelScope.launch {
            prefsRepository.setHapticDurationMs(clamped)
        }
    }

    fun setHapticAccentEquals(enabled: Boolean, view: View? = null) {
        hasUserModifiedHapticAccentEquals = true
        _optimisticPrefs.update { it.copy(hapticAccentEquals = enabled) }
        val prefs = _optimisticPrefs.value
        if (prefs.isVibrationEnabled) {
            calcVibrator.vibrate(
                type = CalcSoundType.EQUALS,
                enabled = true,
                intensity = prefs.hapticIntensity,
                style = prefs.hapticStyle,
                strength = prefs.hapticStrength,
                durationMs = prefs.hapticDurationMs,
                accentEquals = enabled,
                view = view
            )
        }
        viewModelScope.launch {
            prefsRepository.setHapticAccentEquals(enabled)
        }
    }

    fun testHaptic(type: CalcSoundType, view: View? = null) {
        val prefs = _optimisticPrefs.value
        calcVibrator.vibrate(
            type = type,
            enabled = true,
            intensity = prefs.hapticIntensity,
            style = prefs.hapticStyle,
            strength = prefs.hapticStrength,
            durationMs = prefs.hapticDurationMs,
            accentEquals = prefs.hapticAccentEquals,
            view = view,
            forcePreview = true
        )
    }

    fun setButtonShapeStyle(style: ButtonShapeStyle) {
        hasUserModifiedButtonShape = true
        _optimisticPrefs.update { it.copy(buttonShapeStyle = style) }
        viewModelScope.launch {
            prefsRepository.setButtonShapeStyle(style)
        }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        hasUserModifiedDynamicColor = true
        _optimisticPrefs.update { it.copy(useDynamicColor = enabled) }
        viewModelScope.launch {
            prefsRepository.setUseDynamicColor(enabled)
        }
    }

    fun setCustomAccentArgb(argb: Long) {
        hasUserModifiedCustomAccent = true
        _optimisticPrefs.update { it.copy(customAccentArgb = argb) }
        viewModelScope.launch {
            prefsRepository.setCustomAccentArgb(argb)
        }
    }

    fun setShowLivePreview(enabled: Boolean) {
        hasUserModifiedLivePreview = true
        _optimisticPrefs.update { it.copy(showLivePreview = enabled) }
        viewModelScope.launch {
            prefsRepository.setShowLivePreview(enabled)
        }
    }

    fun setAmbientDisplayGlow(enabled: Boolean) {
        hasUserModifiedAmbientGlow = true
        _optimisticPrefs.update { it.copy(ambientDisplayGlow = enabled) }
        viewModelScope.launch {
            prefsRepository.setAmbientDisplayGlow(enabled)
        }
    }

    fun resetCustomizationsToDefaults() {
        hasUserModifiedSoundVolume = true
        hasUserModifiedHapticIntensity = true
        hasUserModifiedHapticStyle = true
        hasUserModifiedHapticStrength = true
        hasUserModifiedHapticDuration = true
        hasUserModifiedHapticAccentEquals = true
        hasUserModifiedButtonShape = true
        hasUserModifiedDynamicColor = true
        hasUserModifiedCustomAccent = true
        hasUserModifiedLivePreview = true
        hasUserModifiedAmbientGlow = true
        _optimisticPrefs.update {
            it.copy(
                soundVolume = DEFAULT_SOUND_VOLUME,
                hapticIntensity = HapticIntensity.MEDIUM,
                hapticStyle = HapticStyle.ADAPTIVE,
                hapticStrength = DEFAULT_HAPTIC_STRENGTH,
                hapticDurationMs = DEFAULT_HAPTIC_DURATION_MS,
                hapticAccentEquals = true,
                buttonShapeStyle = ButtonShapeStyle.PILL,
                useDynamicColor = true,
                customAccentArgb = DEFAULT_CUSTOM_ACCENT_ARGB,
                showLivePreview = true,
                ambientDisplayGlow = false
            )
        }
        viewModelScope.launch {
            prefsRepository.resetCustomizationsToDefaults()
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
                isVoiceInputDialogOpen = if (open) false else it.isVoiceInputDialogOpen,
                isSettingsSheetOpen = if (open) false else it.isSettingsSheetOpen
            )
        }
    }

    fun setVoiceInputDialogOpen(open: Boolean) {
        _state.update {
            it.copy(
                isVoiceInputDialogOpen = open,
                isTopHistoryOpen = if (open) false else it.isTopHistoryOpen,
                isFullHistorySheetOpen = if (open) false else it.isFullHistorySheetOpen,
                isSettingsSheetOpen = if (open) false else it.isSettingsSheetOpen
            )
        }
    }

    fun setSettingsSheetOpen(open: Boolean) {
        _state.update {
            it.copy(
                isSettingsSheetOpen = open,
                isTopHistoryOpen = if (open) false else it.isTopHistoryOpen,
                isFullHistorySheetOpen = if (open) false else it.isFullHistorySheetOpen,
                isVoiceInputDialogOpen = if (open) false else it.isVoiceInputDialogOpen
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
        val angle = _optimisticPrefs.value.angleUnit

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
