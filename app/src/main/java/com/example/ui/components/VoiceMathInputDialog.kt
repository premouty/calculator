package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.RoundedCorner
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.audio.CalcSoundType
import com.example.data.preferences.AngleUnit
import com.example.domain.voice.ParsedVoiceMath
import com.example.domain.voice.SpokenMathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceMathInputOverlay(
    visible: Boolean,
    angleUnit: AngleUnit,
    onInsertExpression: (String) -> Unit,
    onEvaluateExpression: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(180, easing = FastOutSlowInEasing)),
        modifier = modifier.fillMaxSize()
    ) {
        VoiceMathOverlayContent(
            angleUnit = angleUnit,
            onInsertExpression = onInsertExpression,
            onEvaluateExpression = onEvaluateExpression,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun VoiceMathOverlayContent(
    angleUnit: AngleUnit,
    onInsertExpression: (String) -> Unit,
    onEvaluateExpression: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playSound = LocalPlayCalcSound.current
    val performHaptic = LocalPerformCalcHaptic.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRequestedPermissionOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        hasRequestedPermissionOnce = true
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudioPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission && !hasRequestedPermissionOnce) {
            hasRequestedPermissionOnce = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var isListening by remember { mutableStateOf(false) }
    var rawRmsLevel by remember { mutableFloatStateOf(0f) }

    // Continuous 60fps animation state driven smoothly by live voice RMS
    var flowPhase by remember { mutableFloatStateOf(0f) }
    var smoothedRms by remember { mutableFloatStateOf(0f) }
    var peakEnergy by remember { mutableFloatStateOf(0f) }
    val latestRms by rememberUpdatedState(rawRmsLevel)
    val latestListening by rememberUpdatedState(isListening)

    LaunchedEffect(Unit) {
        var lastTimeNanos = 0L
        while (true) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos == 0L) lastTimeNanos = nowNanos
                val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastTimeNanos = nowNanos

                val targetRms = if (latestListening) latestRms else 0f
                val lerpRate = if (targetRms > smoothedRms) 14f else 5.5f
                smoothedRms += (targetRms - smoothedRms) * (lerpRate * dt).coerceAtMost(1f)
                peakEnergy = maxOf(smoothedRms, peakEnergy - 1.5f * dt).coerceIn(0f, 1f)

                val speed = if (latestListening) {
                    0.85f + smoothedRms * 2.8f
                } else {
                    0.38f
                }
                flowPhase = (flowPhase + dt * speed) % (2f * PI.toFloat() * 12f)
            }
        }
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }

    // Harmonious, gently softened Gemini-inspired tones blended into Material You palette
    val auroraBlue = remember(primaryColor, surfaceColor, isDark) {
        val vivid = lerp(primaryColor, Color(0xFF5D93FF), if (isDark) 0.28f else 0.22f)
        lerp(vivid, surfaceColor, if (isDark) 0.10f else 0.14f)
    }
    val auroraViolet = remember(tertiaryColor, surfaceColor, isDark) {
        val vivid = lerp(tertiaryColor, Color(0xFFA877F5), if (isDark) 0.30f else 0.24f)
        lerp(vivid, surfaceColor, if (isDark) 0.10f else 0.14f)
    }
    val auroraCyan = remember(secondaryColor, surfaceColor, isDark) {
        val vivid = lerp(secondaryColor, Color(0xFF4EC7ED), if (isDark) 0.26f else 0.20f)
        lerp(vivid, surfaceColor, if (isDark) 0.12f else 0.16f)
    }
    val auroraPink = remember(tertiaryColor, surfaceColor, isDark) {
        val vivid = lerp(tertiaryColor, Color(0xFFF27CA9), if (isDark) 0.25f else 0.20f)
        lerp(vivid, surfaceColor, if (isDark) 0.12f else 0.16f)
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val fallbackCornerPx = remember(density) { with(density) { 20.dp.toPx() } }
    var deviceCornerRadii by remember(view, fallbackCornerPx) {
        mutableStateOf(readDeviceCornerRadii(view, fallbackCornerPx))
    }

    // Pre-allocated drawing structures so 60/120fps ambient animation causes zero GC churn
    val auroraPalette = remember(auroraBlue, auroraViolet, auroraPink, auroraCyan) {
        arrayOf(auroraBlue, auroraViolet, auroraPink, auroraCyan)
    }
    val perimeterStops = remember { Array(9) { 0f to Color.Transparent } }
    val outerScreenPath = remember { Path() }
    val rimStrokePath = remember { Path() }
    var cachedW by remember { mutableFloatStateOf(-1f) }
    var cachedH by remember { mutableFloatStateOf(-1f) }
    var cachedStroke by remember { mutableFloatStateOf(-1f) }
    var cachedRadii by remember { mutableStateOf(ScreenCornerRadii()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val updated = readDeviceCornerRadii(view, fallbackCornerPx)
                if (updated != deviceCornerRadii) {
                    deviceCornerRadii = updated
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // Full-screen edge-to-edge Gemini-style Material Expressive ambient aurora
        // Adapts precisely to the user's phone rounded corners while keeping the center display clear
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val energy = smoothedRms
            val halo = peakEnergy
            val activeBoost = if (latestListening) 1f else 0.42f
            val baseEdgeAlpha = (if (isDark) 0.25f else 0.19f) * activeBoost + energy * 0.26f
            val borderThickness = (16.dp + (energy * 18f + halo * 5f).dp).toPx()
            val rimStrokePx = (1.8.dp + (energy * 1.7f).dp).toPx()
            val radii = deviceCornerRadii

            // Rebuild cached rounded-screen paths only if dimensions, corner radii, or stroke change
            if (w != cachedW || h != cachedH || radii != cachedRadii || kotlin.math.abs(rimStrokePx - cachedStroke) > 0.25f) {
                cachedW = w
                cachedH = h
                cachedRadii = radii
                cachedStroke = rimStrokePx

                val maxHalf = minOf(w, h) * 0.5f
                val tl = radii.topLeft.coerceIn(0f, maxHalf)
                val tr = radii.topRight.coerceIn(0f, maxHalf)
                val br = radii.bottomRight.coerceIn(0f, maxHalf)
                val bl = radii.bottomLeft.coerceIn(0f, maxHalf)

                outerScreenPath.reset()
                outerScreenPath.addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = w,
                        bottom = h,
                        topLeftCornerRadius = CornerRadius(tl, tl),
                        topRightCornerRadius = CornerRadius(tr, tr),
                        bottomRightCornerRadius = CornerRadius(br, br),
                        bottomLeftCornerRadius = CornerRadius(bl, bl)
                    )
                )

                val halfStroke = rimStrokePx * 0.5f
                rimStrokePath.reset()
                rimStrokePath.addRoundRect(
                    RoundRect(
                        left = halfStroke,
                        top = halfStroke,
                        right = w - halfStroke,
                        bottom = h - halfStroke,
                        topLeftCornerRadius = CornerRadius((tl - halfStroke).coerceAtLeast(0f), (tl - halfStroke).coerceAtLeast(0f)),
                        topRightCornerRadius = CornerRadius((tr - halfStroke).coerceAtLeast(0f), (tr - halfStroke).coerceAtLeast(0f)),
                        bottomRightCornerRadius = CornerRadius((br - halfStroke).coerceAtLeast(0f), (br - halfStroke).coerceAtLeast(0f)),
                        bottomLeftCornerRadius = CornerRadius((bl - halfStroke).coerceAtLeast(0f), (bl - halfStroke).coerceAtLeast(0f))
                    )
                )
            }

            clipPath(outerScreenPath) {
                // 1. Lightweight bottom/perimeter radial blooms (3 focused blooms instead of full-screen overlap)
                val minDim = minOf(w, h)
                val bloomAlpha = ((if (isDark) 0.16f else 0.12f) * activeBoost + energy * 0.17f)
                    .coerceIn(0f, 0.38f)

                for (idx in 0..2) {
                    val angle = flowPhase * (0.52f + idx * 0.11f) + idx * 2.094f
                    val cx = w * (0.5f + 0.44f * cos(angle))
                    val cy = if (idx == 2) {
                        h * (0.12f + 0.08f * sin(angle))
                    } else {
                        h * (0.86f + 0.11f * sin(angle * 0.9f))
                    }
                    val bloomRadius = (minDim * (0.34f + energy * 0.16f + halo * 0.05f)).coerceAtLeast(1f)
                    val bloomColor = auroraPalette[idx]
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to bloomColor.copy(alpha = bloomAlpha),
                            0.55f to bloomColor.copy(alpha = bloomAlpha * 0.30f),
                            1.0f to Color.Transparent,
                            center = Offset(cx, cy),
                            radius = bloomRadius
                        ),
                        center = Offset(cx, cy),
                        radius = bloomRadius
                    )
                }

                // 2. Soft inward edge bands on all 4 sides (keeps center completely clear)
                val wave1 = 0.5f + 0.5f * sin(flowPhase * 1.05f)
                val wave2 = 0.5f + 0.5f * cos(flowPhase * 0.88f)
                val topColor = lerp(auroraPalette[0], auroraPalette[1], wave1)
                val bottomColor = lerp(auroraPalette[1], auroraPalette[3], wave2)
                val leftColor = lerp(auroraPalette[3], auroraPalette[2], wave1)
                val rightColor = lerp(auroraPalette[2], auroraPalette[0], wave2)

                // Top soft edge glow
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to topColor.copy(alpha = baseEdgeAlpha * 0.68f),
                        0.5f to topColor.copy(alpha = baseEdgeAlpha * 0.18f),
                        1.0f to Color.Transparent,
                        startY = 0f,
                        endY = borderThickness * 1.3f
                    ),
                    topLeft = Offset.Zero,
                    size = Size(w, borderThickness * 1.3f)
                )
                // Left soft edge glow
                drawRect(
                    brush = Brush.horizontalGradient(
                        0.0f to leftColor.copy(alpha = baseEdgeAlpha * 0.65f),
                        0.55f to leftColor.copy(alpha = baseEdgeAlpha * 0.16f),
                        1.0f to Color.Transparent,
                        startX = 0f,
                        endX = borderThickness * 1.15f
                    ),
                    topLeft = Offset.Zero,
                    size = Size(borderThickness * 1.15f, h)
                )
                // Right soft edge glow
                drawRect(
                    brush = Brush.horizontalGradient(
                        0.0f to Color.Transparent,
                        0.45f to rightColor.copy(alpha = baseEdgeAlpha * 0.16f),
                        1.0f to rightColor.copy(alpha = baseEdgeAlpha * 0.65f),
                        startX = w - borderThickness * 1.15f,
                        endX = w
                    ),
                    topLeft = Offset(w - borderThickness * 1.15f, 0f),
                    size = Size(borderThickness * 1.15f, h)
                )
                // Bottom rising Gemini mist behind the floating dock
                val bottomMistHeight = h * (0.22f + energy * 0.08f)
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.48f to bottomColor.copy(alpha = baseEdgeAlpha * 0.22f),
                        0.85f to bottomColor.copy(alpha = baseEdgeAlpha * 0.56f),
                        1.0f to auroraBlue.copy(alpha = (baseEdgeAlpha * 0.80f).coerceAtMost(0.58f)),
                        startY = h - bottomMistHeight,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - bottomMistHeight),
                    size = Size(w, bottomMistHeight)
                )
            }

            // 3. Seamless rotating rim stroke matching the device's physical rounded corners
            val rimAlpha = ((if (isDark) 0.52f else 0.42f) * activeBoost + energy * 0.26f)
                .coerceIn(0f, 0.78f)
            val shift = ((flowPhase * 0.14f) % 1f + 1f) % 1f
            val stopCount = perimeterStops.size - 1
            for (step in 0..stopCount) {
                val t = step.toFloat() / stopCount.toFloat()
                val u = (((t - shift) % 1f + 1f) % 1f) * auroraPalette.size
                val idx0 = u.toInt() % auroraPalette.size
                val idx1 = (idx0 + 1) % auroraPalette.size
                val frac = u - u.toInt()
                perimeterStops[step] = t to lerp(auroraPalette[idx0], auroraPalette[idx1], frac).copy(alpha = rimAlpha)
            }

            drawPath(
                path = rimStrokePath,
                brush = Brush.sweepGradient(
                    colorStops = perimeterStops,
                    center = Offset(w / 2f, h / 2f)
                ),
                style = Stroke(width = rimStrokePx)
            )
        }

        // Sleek floating bottom dock — keeps calculator display above unobscured
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val dockShape = RoundedCornerShape(30.dp)
            Surface(
                shape = dockShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                    alpha = if (isDark) 0.94f else 0.96f
                ),
                tonalElevation = 8.dp,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume taps inside dock */ }
                    )
                    .let { mod ->
                        val dockRimColors = remember(auroraBlue, auroraViolet, auroraCyan) {
                            listOf(auroraBlue, auroraViolet, auroraCyan)
                        }
                        mod.drawBehind {
                            val pulse = smoothedRms
                            val rimPx = (1.dp + (pulse * 0.8f).dp).toPx()
                            val corner = 30.dp.toPx()
                            val rimAlpha = if (latestListening) 0.36f + pulse * 0.32f else 0.22f
                            drawRoundRect(
                                brush = Brush.horizontalGradient(colors = dockRimColors),
                                topLeft = Offset(rimPx / 2f, rimPx / 2f),
                                size = Size(size.width - rimPx, size.height - rimPx),
                                cornerRadius = CornerRadius(corner, corner),
                                alpha = rimAlpha,
                                style = Stroke(width = rimPx)
                            )
                        }
                    }
                    .slideInDockAnimation()
                    .testTag("voice_math_input_dialog")
            ) {
                if (!hasAudioPermission) {
                    VoicePermissionContent(
                        canShowRationale = context.findVoiceActivity()?.let {
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                it,
                                Manifest.permission.RECORD_AUDIO
                            )
                        } ?: true,
                        hasRequestedOnce = hasRequestedPermissionOnce,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        },
                        onDismiss = onDismiss
                    )
                } else {
                    VoiceRecognitionDockContent(
                        angleUnit = angleUnit,
                        flowPhase = { flowPhase },
                        smoothedRms = { smoothedRms },
                        peakEnergy = { peakEnergy },
                        auroraColors = listOf(auroraBlue, auroraViolet, auroraCyan, auroraPink),
                        onListeningChanged = { listening -> isListening = listening },
                        onRmsChanged = { rms -> rawRmsLevel = rms },
                        onInsertExpression = { expr ->
                            playSound(CalcSoundType.OPERATOR)
                            performHaptic(CalcSoundType.OPERATOR)
                            onInsertExpression(expr)
                        },
                        onEvaluateExpression = { expr ->
                            playSound(CalcSoundType.EQUALS)
                            performHaptic(CalcSoundType.EQUALS)
                            onEvaluateExpression(expr)
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.slideInDockAnimation(): Modifier {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val slideY by animateFloatAsState(
        targetValue = if (appeared) 0f else 48f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "voice_dock_slide"
    )
    return this.graphicsLayer { translationY = slideY }
}

@Composable
private fun VoiceRecognitionDockContent(
    angleUnit: AngleUnit,
    flowPhase: () -> Float,
    smoothedRms: () -> Float,
    peakEnergy: () -> Float,
    auroraColors: List<Color>,
    onListeningChanged: (Boolean) -> Unit,
    onRmsChanged: (Float) -> Unit,
    onInsertExpression: (String) -> Unit,
    onEvaluateExpression: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val latestAngleUnit by rememberUpdatedState(angleUnit)
    val isAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }

    var isListening by remember { mutableStateOf(false) }
    var spokenText by remember { mutableStateOf("") }
    var parsedMath by remember { mutableStateOf<ParsedVoiceMath?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val noMatchMsg = stringResource(R.string.voice_error_no_match)

    val speechRecognizer = remember(context, isAvailable) {
        if (isAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    val updateListening = { listening: Boolean ->
        isListening = listening
        onListeningChanged(listening)
        if (!listening) onRmsChanged(0f)
    }

    val startListening: () -> Unit = {
        if (speechRecognizer != null) {
            errorMessage = null
            spokenText = ""
            parsedMath = null
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uk-UA")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            updateListening(true)
            runCatching {
                speechRecognizer.startListening(intent)
            }.onFailure { err ->
                updateListening(false)
                errorMessage = err.localizedMessage ?: noMatchMsg
            }
        }
    }

    DisposableEffect(speechRecognizer) {
        if (speechRecognizer == null) {
            onDispose { }
        } else {
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    updateListening(true)
                    errorMessage = null
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) {
                    onRmsChanged(((rmsdB + 2f) / 11f).coerceIn(0f, 1f))
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    updateListening(false)
                }

                override fun onError(error: Int) {
                    updateListening(false)
                    if (spokenText.isBlank()) {
                        errorMessage = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> context.getString(R.string.voice_error_no_match)
                            else -> context.getString(R.string.voice_error_generic, error)
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    updateListening(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (matches.isEmpty()) {
                        if (spokenText.isBlank()) {
                            errorMessage = noMatchMsg
                        }
                        return
                    }
                    var bestTranscript = matches.first()
                    var bestParsed: ParsedVoiceMath? = null
                    for (candidate in matches) {
                        val parsed = SpokenMathParser.parse(candidate, latestAngleUnit)
                        if (parsed != null) {
                            bestTranscript = candidate
                            bestParsed = parsed
                            break
                        }
                    }
                    spokenText = bestTranscript
                    parsedMath = bestParsed
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        spokenText = partial
                        parsedMath = SpokenMathParser.parse(partial, latestAngleUnit)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }

            speechRecognizer.setRecognitionListener(listener)
            startListening()

            onDispose {
                updateListening(false)
                runCatching { speechRecognizer.cancel() }
                runCatching { speechRecognizer.destroy() }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact Expressive Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = auroraColors.first(),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            val rms = smoothedRms()
                            val scale = 1f + rms * 0.22f
                            scaleX = scale
                            scaleY = scale
                            rotationZ = if (isListening) (flowPhase() * 14f) % 360f else 0f
                        }
                )
                Text(
                    text = stringResource(R.string.voice_input_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = {
                    runCatching { speechRecognizer?.cancel() }
                    onDismiss()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(36.dp)
                    .testTag("voice_dialog_close")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (!isAvailable) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.voice_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            return
        }

        // Expandable Transcript & Parsed Expression Card (stays hidden until words appear)
        AnimatedVisibility(
            visible = spokenText.isNotBlank(),
            enter = fadeIn(tween(150)) + expandVertically(),
            exit = fadeOut(tween(120)) + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${stringResource(R.string.voice_spoken_label)} «$spokenText»",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("voice_spoken_transcript")
                    )

                    val activeParsed = parsedMath
                    if (activeParsed != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = activeParsed.formattedExpression,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("voice_parsed_expression")
                            )
                            if (activeParsed.previewResult.isNotEmpty()) {
                                Text(
                                    text = "= ${activeParsed.previewResult}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else if (!isListening) {
                        Text(
                            text = stringResource(R.string.voice_no_math_found),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Reactive Gemini-style Expressive Waveform + Morphing Mic Orb
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Morphing Expressive Mic Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp)
            ) {
                // Organic multi-lobed rotating aurora ring behind mic orb (pre-allocated Path)
                val orbPath = remember { Path() }
                val orbSweepColors = remember(auroraColors) {
                    listOf(auroraColors[0], auroraColors[1], auroraColors[2], auroraColors[0])
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rms = smoothedRms()
                    val halo = peakEnergy()
                    val phase = flowPhase()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = size.minDimension * 0.34f
                    val dynamicAmp = if (isListening) {
                        (2.8.dp + (rms * 6.5f + halo * 2f).dp).toPx()
                    } else {
                        1.5.dp.toPx()
                    }

                    // Soft radial aura
                    val auraRadius = (baseRadius + dynamicAmp * 1.85f).coerceAtLeast(1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.3f to auroraColors[1].copy(alpha = if (isListening) 0.28f + rms * 0.28f else 0.12f),
                            1.0f to Color.Transparent,
                            center = center,
                            radius = auraRadius
                        ),
                        center = center,
                        radius = auraRadius
                    )

                    // Morphing expressive 6-lobe flower blob
                    if (isListening) {
                        orbPath.reset()
                        val steps = 48
                        for (i in 0..steps) {
                            val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                            val lobe = sin(theta * 5f + phase * 3.2f) * 0.5f +
                                cos(theta * 3f - phase * 2.1f) * 0.5f
                            val r = baseRadius + dynamicAmp * (0.45f + 0.55f * lobe)
                            val x = center.x + r * cos(theta)
                            val y = center.y + r * sin(theta)
                            if (i == 0) orbPath.moveTo(x, y) else orbPath.lineTo(x, y)
                        }
                        orbPath.close()
                        drawPath(
                            path = orbPath,
                            brush = Brush.sweepGradient(
                                colors = orbSweepColors,
                                center = center
                            ),
                            alpha = 0.40f + rms * 0.30f
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            val s = if (isListening) 1f + smoothedRms() * 0.12f else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .clip(CircleShape)
                        .clickable {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                updateListening(false)
                            } else {
                                startListening()
                            }
                        }
                        .testTag("voice_mic_orb_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = stringResource(
                                if (isListening) R.string.voice_stop_listening else R.string.cd_voice_input
                            ),
                            tint = if (isListening) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Status + Live Audio-Reactive Expressive Bars
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = when {
                        isListening -> stringResource(R.string.voice_listening)
                        errorMessage != null -> errorMessage!!
                        spokenText.isBlank() -> stringResource(R.string.voice_tap_to_speak)
                        else -> stringResource(R.string.voice_speak_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (errorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isListening) {
                    // Harmonic multi-bar expressive audio visualizer
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                    ) {
                        val barCount = 14
                        val phase = flowPhase()
                        val rms = smoothedRms()
                        val gap = size.width / (barCount * 1.9f)
                        val barWidth = ((size.width - gap * (barCount - 1)) / barCount)
                            .coerceIn(3.dp.toPx(), 6.dp.toPx())
                        val totalContentW = barCount * barWidth + (barCount - 1) * gap
                        val startX = 0f.coerceAtLeast((size.width - totalContentW) * 0f)
                        val midY = size.height / 2f

                        for (i in 0 until barCount) {
                            val normIdx = i.toFloat() / (barCount - 1)
                            val centerWeight = 1f - kotlin.math.abs(normIdx - 0.5f) * 1.15f
                            val wave = 0.5f + 0.5f * sin(phase * 4.8f + i * 0.72f)
                            val idleHeight = 4.dp.toPx() + 3.dp.toPx() * wave
                            val voiceHeight = (size.height - 2.dp.toPx()) *
                                (0.25f + 0.75f * wave) *
                                (0.35f + 0.65f * centerWeight.coerceIn(0.25f, 1f)) *
                                rms
                            val barH = (idleHeight + voiceHeight).coerceIn(4.dp.toPx(), size.height)
                            val x = startX + i * (barWidth + gap)
                            val barColor = lerp(
                                auroraColors[0],
                                auroraColors[1],
                                normIdx
                            ).copy(alpha = 0.55f + rms * 0.45f)

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, midY - barH / 2f),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }

                    if (spokenText.isBlank()) {
                        Text(
                            text = stringResource(R.string.voice_speak_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Actions once listening finishes
        val finalParsed = parsedMath
        if (!isListening && finalParsed != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onInsertExpression(finalParsed.rawExpression) },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("voice_insert_button")
                ) {
                    Text(
                        text = stringResource(R.string.voice_apply),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { onEvaluateExpression(finalParsed.rawExpression) },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("voice_evaluate_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.voice_evaluate),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = startListening,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .testTag("voice_retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.voice_retry),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else if (!isListening && (errorMessage != null || spokenText.isNotBlank())) {
            Button(
                onClick = startListening,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("voice_retry_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.voice_retry))
            }
        }
    }
}

@Composable
private fun VoicePermissionContent(
    canShowRationale: Boolean,
    hasRequestedOnce: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = stringResource(R.string.voice_permission_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.voice_permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        val showSettingsFallback = hasRequestedOnce && !canShowRationale
        Button(
            onClick = if (showSettingsFallback) onOpenSettings else onRequestPermission,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(
                    if (showSettingsFallback) R.string.voice_permission_settings
                    else R.string.voice_permission_grant
                )
            )
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.height(44.dp)
        ) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

private data class ScreenCornerRadii(
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f
)

private fun readDeviceCornerRadii(view: View, fallbackPx: Float): ScreenCornerRadii {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val insets = view.rootWindowInsets
        if (insets != null) {
            val tl = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat()
            val tr = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius?.toFloat()
            val br = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius?.toFloat()
            val bl = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius?.toFloat()
            if (tl != null || tr != null || br != null || bl != null) {
                return ScreenCornerRadii(
                    topLeft = tl ?: 0f,
                    topRight = tr ?: 0f,
                    bottomRight = br ?: 0f,
                    bottomLeft = bl ?: 0f
                )
            }
        }
    }
    return ScreenCornerRadii(fallbackPx, fallbackPx, fallbackPx, fallbackPx)
}

private tailrec fun Context.findVoiceActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findVoiceActivity()
    else -> null
}
