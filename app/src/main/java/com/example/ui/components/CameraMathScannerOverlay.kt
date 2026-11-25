package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.audio.CalcSoundType
import com.example.data.preferences.AngleUnit
import com.example.domain.ocr.MathExpressionOcrParser
import com.example.domain.ocr.OcrBox
import com.example.domain.ocr.OcrLineCandidate
import com.example.domain.ocr.ScannedMathExpression
import com.example.ui.CameraScanAnalysisState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private val DefaultCameraScanZone = Rect(left = 0.06f, top = 0.20f, right = 0.94f, bottom = 0.64f)

private enum class ZoneCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

@Composable
fun CameraMathScannerOverlay(
    visible: Boolean,
    angleUnit: AngleUnit,
    analysisState: CameraScanAnalysisState,
    onAnalyzeWithAi: (Bitmap) -> Unit,
    onSetOfflineResult: (List<ScannedMathExpression>) -> Unit,
    onResetAnalysis: () -> Unit,
    onTransferExpression: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(
            initialOffsetY = { it / 12 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = fadeOut(tween(170)) + slideOutVertically(
            targetOffsetY = { it / 14 },
            animationSpec = tween(170)
        )
    ) {
        BackHandler(onBack = onDismiss)
        CameraMathScannerContent(
            angleUnit = angleUnit,
            analysisState = analysisState,
            onAnalyzeWithAi = onAnalyzeWithAi,
            onSetOfflineResult = onSetOfflineResult,
            onResetAnalysis = onResetAnalysis,
            onTransferExpression = onTransferExpression,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun CameraMathScannerContent(
    angleUnit: AngleUnit,
    analysisState: CameraScanAnalysisState,
    onAnalyzeWithAi: (Bitmap) -> Unit,
    onSetOfflineResult: (List<ScannedMathExpression>) -> Unit,
    onResetAnalysis: () -> Unit,
    onTransferExpression: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playSound = LocalPlayCalcSound.current
    val performHaptic = LocalPerformCalcHaptic.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRequestedPermissionOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        hasRequestedPermissionOnce = true
    }

    // Re-check permission if returning from system app settings
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !hasRequestedPermissionOnce) {
            hasRequestedPermissionOnce = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_scanner_overlay")
    ) {
        if (hasCameraPermission) {
            CameraPhotoCaptureAndSelectFlow(
                angleUnit = angleUnit,
                analysisState = analysisState,
                onAnalyzeWithAi = onAnalyzeWithAi,
                onSetOfflineResult = onSetOfflineResult,
                onResetAnalysis = onResetAnalysis,
                onTransferExpression = { expr ->
                    playSound(CalcSoundType.EQUALS)
                    performHaptic(CalcSoundType.EQUALS)
                    onTransferExpression(expr)
                },
                onDismiss = onDismiss
            )
        } else {
            CameraPermissionPrompt(
                canShowRationale = context.findActivity()?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: true,
                hasRequestedOnce = hasRequestedPermissionOnce,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun CameraPhotoCaptureAndSelectFlow(
    angleUnit: AngleUnit,
    analysisState: CameraScanAnalysisState,
    onAnalyzeWithAi: (Bitmap) -> Unit,
    onSetOfflineResult: (List<ScannedMathExpression>) -> Unit,
    onResetAnalysis: () -> Unit,
    onTransferExpression: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playSound = LocalPlayCalcSound.current
    val performHaptic = LocalPerformCalcHaptic.current

    var useAiMode by remember { mutableStateOf(true) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isProcessingCapture by remember { mutableStateOf(false) }
    var captureErrorResId by remember { mutableStateOf<Int?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedExpressionId by remember { mutableStateOf<String?>(null) }
    var previewViewportSize by remember { mutableStateOf(IntSize.Zero) }
    var scanZone by remember { mutableStateOf(DefaultCameraScanZone) }
    var effectiveScanZone by remember { mutableStateOf(DefaultCameraScanZone) }

    val latestAngleUnit by rememberUpdatedState(angleUnit)
    val latestViewportSize by rememberUpdatedState(previewViewportSize)
    val latestEffectiveScanZone by rememberUpdatedState(effectiveScanZone)
    val latestUseAiMode by rememberUpdatedState(useAiMode)

    val isScanZoneModified = remember(scanZone) {
        kotlin.math.abs(scanZone.left - DefaultCameraScanZone.left) > 0.015f ||
            kotlin.math.abs(scanZone.top - DefaultCameraScanZone.top) > 0.015f ||
            kotlin.math.abs(scanZone.right - DefaultCameraScanZone.right) > 0.015f ||
            kotlin.math.abs(scanZone.bottom - DefaultCameraScanZone.bottom) > 0.015f
    }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            isPinchToZoomEnabled = true
            isTapToFocusEnabled = true
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    DisposableEffect(cameraController, lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.unbind()
            textRecognizer.close()
            captureExecutor.shutdown()
        }
    }

    LaunchedEffect(analysisState) {
        if (analysisState is CameraScanAnalysisState.Success) {
            selectedExpressionId = analysisState.expressions.firstOrNull()?.id
        } else if (analysisState is CameraScanAnalysisState.Loading ||
            analysisState is CameraScanAnalysisState.Error
        ) {
            selectedExpressionId = null
        }
    }

    // Keep torch active only during live preview
    LaunchedEffect(isTorchOn, capturedBitmap) {
        val shouldEnable = isTorchOn && capturedBitmap == null
        runCatching { cameraController.enableTorch(shouldEnable) }
    }

    val runOfflineMlKit: (Bitmap) -> Unit = { bitmap ->
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(inputImage)
            .addOnSuccessListener(mainExecutor) { visionText ->
                val candidates = visionText.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        val rect = line.boundingBox
                        OcrLineCandidate(
                            text = line.text,
                            box = rect?.let {
                                OcrBox(
                                    left = it.left.toFloat(),
                                    top = it.top.toFloat(),
                                    right = it.right.toFloat(),
                                    bottom = it.bottom.toFloat()
                                )
                            }
                        )
                    }
                }
                val expressions = MathExpressionOcrParser.extractAllExpressions(
                    lines = candidates,
                    angleUnit = latestAngleUnit
                )
                onSetOfflineResult(expressions)
                selectedExpressionId = expressions.firstOrNull()?.id
                isProcessingCapture = false
            }
            .addOnFailureListener(mainExecutor) {
                onSetOfflineResult(emptyList())
                selectedExpressionId = null
                isProcessingCapture = false
            }
    }

    val retakePhoto: () -> Unit = {
        playSound(CalcSoundType.UTILITY)
        performHaptic(CalcSoundType.UTILITY)
        onResetAnalysis()
        capturedBitmap = null
        selectedExpressionId = null
        captureErrorResId = null
        isProcessingCapture = false
    }

    // Pressing system Back while reviewing a captured photo returns to camera viewfinder
    BackHandler(enabled = capturedBitmap != null) {
        retakePhoto()
    }

    val takePhoto: () -> Unit = {
        if (!isProcessingCapture && capturedBitmap == null) {
            isProcessingCapture = true
            captureErrorResId = null
            playSound(CalcSoundType.OPERATOR)
            performHaptic(CalcSoundType.OPERATOR)

            val zoneSnapshot = latestEffectiveScanZone
            val viewportSnapshot = latestViewportSize
            val mainExecutor = ContextCompat.getMainExecutor(context)
            cameraController.takePicture(
                captureExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val processedBitmap: Bitmap? = try {
                            val rotationDegrees = image.imageInfo.rotationDegrees
                            val crop = image.cropRect
                            val rawBitmap = image.toBitmap()
                            val upright = cropAndRotateBitmap(rawBitmap, crop, rotationDegrees)
                            if (upright !== rawBitmap) rawBitmap.recycle()

                            val viewportMatched = cropToViewport(
                                bitmap = upright,
                                viewportSize = viewportSnapshot
                            )
                            if (viewportMatched !== upright) upright.recycle()

                            val zoneCropped = cropToNormalizedZoneAndScale(
                                bitmap = viewportMatched,
                                normalizedZone = zoneSnapshot,
                                maxDimension = 1920
                            )
                            if (zoneCropped !== viewportMatched) viewportMatched.recycle()
                            zoneCropped
                        } catch (_: Throwable) {
                            null
                        } finally {
                            image.close()
                        }

                        if (processedBitmap == null) {
                            mainExecutor.execute {
                                isProcessingCapture = false
                                captureErrorResId = R.string.camera_scan_capture_error
                            }
                            return
                        }

                        mainExecutor.execute {
                            capturedBitmap = processedBitmap
                            selectedExpressionId = null
                            isProcessingCapture = false
                            if (latestUseAiMode) {
                                onAnalyzeWithAi(processedBitmap)
                            } else {
                                runOfflineMlKit(processedBitmap)
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        mainExecutor.execute {
                            isProcessingCapture = false
                            captureErrorResId = R.string.camera_scan_capture_error
                        }
                    }
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Keep CameraX PreviewView mounted so retake is instant
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    controller = cameraController
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { previewViewportSize = it }
        )

        if (capturedBitmap == null) {
            LiveCaptureViewfinderChrome(
                scanZone = scanZone,
                isScanZoneModified = isScanZoneModified,
                useAiMode = useAiMode,
                isTorchOn = isTorchOn,
                isProcessing = isProcessingCapture,
                errorResId = captureErrorResId,
                onToggleAiMode = { targetAi ->
                    if (useAiMode != targetAi) {
                        playSound(CalcSoundType.DIGIT)
                        performHaptic(CalcSoundType.DIGIT)
                        useAiMode = targetAi
                    }
                },
                onScanZoneChange = { scanZone = it },
                onEffectiveZoneChange = { effectiveScanZone = it },
                onResetScanZone = {
                    playSound(CalcSoundType.DIGIT)
                    performHaptic(CalcSoundType.DIGIT)
                    scanZone = DefaultCameraScanZone
                },
                onToggleTorch = { isTorchOn = !isTorchOn },
                onCaptureClick = takePhoto,
                onDismiss = onDismiss
            )
        } else {
            val activeBitmap = capturedBitmap!!
            CapturedPhotoSelectorView(
                bitmap = activeBitmap,
                analysisState = analysisState,
                selectedExpressionId = selectedExpressionId,
                onSelectExpression = { id ->
                    if (selectedExpressionId != id) {
                        playSound(CalcSoundType.DIGIT)
                        performHaptic(CalcSoundType.DIGIT)
                    }
                    selectedExpressionId = id
                },
                onRetryAi = {
                    playSound(CalcSoundType.OPERATOR)
                    performHaptic(CalcSoundType.OPERATOR)
                    useAiMode = true
                    onAnalyzeWithAi(activeBitmap)
                },
                onFallbackOffline = {
                    playSound(CalcSoundType.UTILITY)
                    performHaptic(CalcSoundType.UTILITY)
                    useAiMode = false
                    runOfflineMlKit(activeBitmap)
                },
                onRetake = retakePhoto,
                onTransfer = {
                    val expressions = (analysisState as? CameraScanAnalysisState.Success)?.expressions.orEmpty()
                    val chosen = expressions.firstOrNull { it.id == selectedExpressionId }
                        ?: expressions.firstOrNull()
                    if (chosen != null) {
                        onTransferExpression(chosen.rawExpression)
                    }
                },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun LiveCaptureViewfinderChrome(
    scanZone: Rect,
    isScanZoneModified: Boolean,
    useAiMode: Boolean,
    isTorchOn: Boolean,
    isProcessing: Boolean,
    errorResId: Int?,
    onToggleAiMode: (Boolean) -> Unit,
    onScanZoneChange: (Rect) -> Unit,
    onEffectiveZoneChange: (Rect) -> Unit,
    onResetScanZone: () -> Unit,
    onToggleTorch: () -> Unit,
    onCaptureClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    var isEditingZone by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val fullHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val safeLeftPx = with(density) { 12.dp.toPx() }
        val safeRightPx = (fullWidthPx - safeLeftPx).coerceAtLeast(safeLeftPx + 1f)
        val safeTopPx = (statusBarTopPx + with(density) { 70.dp.toPx() }).coerceAtMost(fullHeightPx * 0.45f)
        val safeBottomPx = (fullHeightPx - navBarBottomPx - with(density) { 164.dp.toPx() })
            .coerceAtLeast(safeTopPx + with(density) { 96.dp.toPx() })

        val minZoneWidthPx = with(density) { 108.dp.toPx() }.coerceAtMost(safeRightPx - safeLeftPx)
        val minZoneHeightPx = with(density) { 68.dp.toPx() }.coerceAtMost(safeBottomPx - safeTopPx)

        val frameLeft = (scanZone.left * fullWidthPx).coerceIn(safeLeftPx, safeRightPx - minZoneWidthPx)
        val frameTop = (scanZone.top * fullHeightPx).coerceIn(safeTopPx, safeBottomPx - minZoneHeightPx)
        val frameRight = (scanZone.right * fullWidthPx).coerceIn(frameLeft + minZoneWidthPx, safeRightPx)
        val frameBottom = (scanZone.bottom * fullHeightPx).coerceIn(frameTop + minZoneHeightPx, safeBottomPx)
        val frameWidthPx = (frameRight - frameLeft).coerceAtLeast(minZoneWidthPx)
        val frameHeightPx = (frameBottom - frameTop).coerceAtLeast(minZoneHeightPx)

        LaunchedEffect(frameLeft, frameTop, frameRight, frameBottom, fullWidthPx, fullHeightPx) {
            onEffectiveZoneChange(
                Rect(
                    left = frameLeft / fullWidthPx,
                    top = frameTop / fullHeightPx,
                    right = frameRight / fullWidthPx,
                    bottom = frameBottom / fullHeightPx
                )
            )
        }

        val latestFrameLeft by rememberUpdatedState(frameLeft)
        val latestFrameTop by rememberUpdatedState(frameTop)
        val latestFrameRight by rememberUpdatedState(frameRight)
        val latestFrameBottom by rememberUpdatedState(frameBottom)
        val latestOnScanZoneChange by rememberUpdatedState(onScanZoneChange)

        val activeFrameColor by animateColorAsState(
            targetValue = if (isEditingZone) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.White.copy(alpha = 0.94f)
            },
            animationSpec = tween(140),
            label = "editable_zone_bracket_color"
        )

        // Subtle framing backdrop with clear rounded window & interactive corner handles
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(color = Color.Black.copy(alpha = if (isEditingZone) 0.56f else 0.44f))

            val cutoutCorner = 22.dp.toPx().coerceAtMost(minOf(frameWidthPx, frameHeightPx) / 3f)
            val cutoutRect = Rect(
                left = frameLeft,
                top = frameTop,
                right = frameRight,
                bottom = frameBottom
            )
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = cutoutRect,
                        cornerRadius = CornerRadius(cutoutCorner, cutoutCorner)
                    )
                )
            }
            drawPath(path = cutoutPath, color = Color.Transparent, blendMode = BlendMode.Clear)

            // Crisp thin border around the active camera zone
            drawRoundRect(
                color = activeFrameColor.copy(alpha = if (isEditingZone) 0.85f else 0.45f),
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameWidthPx, frameHeightPx),
                cornerRadius = CornerRadius(cutoutCorner, cutoutCorner),
                style = Stroke(width = if (isEditingZone) 2.dp.toPx() else 1.2.dp.toPx())
            )

            // Subtle alignment grid lines only while resizing/moving the zone
            if (isEditingZone) {
                val thirdW = frameWidthPx / 3f
                val thirdH = frameHeightPx / 3f
                val gridColor = Color.White.copy(alpha = 0.24f)
                val gridStroke = 1.dp.toPx()
                for (i in 1..2) {
                    val x = frameLeft + thirdW * i
                    drawLine(
                        color = gridColor,
                        start = Offset(x, frameTop + cutoutCorner * 0.4f),
                        end = Offset(x, frameBottom - cutoutCorner * 0.4f),
                        strokeWidth = gridStroke
                    )
                    val y = frameTop + thirdH * i
                    drawLine(
                        color = gridColor,
                        start = Offset(frameLeft + cutoutCorner * 0.4f, y),
                        end = Offset(frameRight - cutoutCorner * 0.4f, y),
                        strokeWidth = gridStroke
                    )
                }
            }

            val strokePx = 3.5.dp.toPx()
            val bracketLen = 26.dp.toPx().coerceAtMost(minOf(frameWidthPx, frameHeightPx) * 0.42f)
            val r = cutoutCorner

            fun drawCorner(path: Path) {
                drawPath(
                    path = path,
                    color = activeFrameColor,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            drawCorner(Path().apply {
                moveTo(frameLeft, frameTop + bracketLen)
                lineTo(frameLeft, frameTop + r)
                quadraticTo(frameLeft, frameTop, frameLeft + r, frameTop)
                lineTo(frameLeft + bracketLen, frameTop)
            })
            drawCorner(Path().apply {
                moveTo(frameRight - bracketLen, frameTop)
                lineTo(frameRight - r, frameTop)
                quadraticTo(frameRight, frameTop, frameRight, frameTop + r)
                lineTo(frameRight, frameTop + bracketLen)
            })
            drawCorner(Path().apply {
                moveTo(frameLeft, frameBottom - bracketLen)
                lineTo(frameLeft, frameBottom - r)
                quadraticTo(frameLeft, frameBottom, frameLeft + r, frameBottom)
                lineTo(frameLeft + bracketLen, frameBottom)
            })
            drawCorner(Path().apply {
                moveTo(frameRight - bracketLen, frameBottom)
                lineTo(frameRight - r, frameBottom)
                quadraticTo(frameRight, frameBottom, frameRight, frameBottom - r)
                lineTo(frameRight, frameBottom - bracketLen)
            })

            // Corner drag knob dots to signal resize affordance
            val knobRadius = if (isEditingZone) 6.dp.toPx() else 5.dp.toPx()
            val corners = listOf(
                Offset(frameLeft, frameTop),
                Offset(frameRight, frameTop),
                Offset(frameLeft, frameBottom),
                Offset(frameRight, frameBottom)
            )
            corners.forEach { center ->
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = knobRadius + 1.5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = activeFrameColor,
                    radius = knobRadius,
                    center = center
                )
            }
        }

        // Drag inside the zone to reposition it freely within safe bounds
        val moveDesc = stringResource(R.string.cd_camera_zone_move)
        Box(
            modifier = Modifier
                .offset { IntOffset(frameLeft.roundToInt(), frameTop.roundToInt()) }
                .size(
                    width = with(density) { frameWidthPx.toDp() },
                    height = with(density) { frameHeightPx.toDp() }
                )
                .semantics { contentDescription = moveDesc }
                .pointerInput(fullWidthPx, fullHeightPx, safeLeftPx, safeTopPx, safeRightPx, safeBottomPx) {
                    detectDragGestures(
                        onDragStart = { isEditingZone = true },
                        onDragEnd = { isEditingZone = false },
                        onDragCancel = { isEditingZone = false }
                    ) { change, dragAmount ->
                        change.consume()
                        val w = (latestFrameRight - latestFrameLeft).coerceAtLeast(minZoneWidthPx)
                        val h = (latestFrameBottom - latestFrameTop).coerceAtLeast(minZoneHeightPx)
                        val newLeft = (latestFrameLeft + dragAmount.x)
                            .coerceIn(safeLeftPx, safeRightPx - w)
                        val newTop = (latestFrameTop + dragAmount.y)
                            .coerceIn(safeTopPx, safeBottomPx - h)
                        latestOnScanZoneChange(
                            Rect(
                                left = newLeft / fullWidthPx,
                                top = newTop / fullHeightPx,
                                right = (newLeft + w) / fullWidthPx,
                                bottom = (newTop + h) / fullHeightPx
                            )
                        )
                    }
                }
                .testTag("camera_scan_zone_move_area")
        )

        // Four 48dp corner resize handles centered on each corner of the frame
        val handleDesc = stringResource(R.string.cd_camera_zone_handle)
        val handleSize = 48.dp
        val halfHandlePx = with(density) { (handleSize / 2).toPx() }
        val cornerPositions = listOf(
            ZoneCorner.TOP_LEFT to Offset(frameLeft, frameTop),
            ZoneCorner.TOP_RIGHT to Offset(frameRight, frameTop),
            ZoneCorner.BOTTOM_LEFT to Offset(frameLeft, frameBottom),
            ZoneCorner.BOTTOM_RIGHT to Offset(frameRight, frameBottom)
        )

        cornerPositions.forEach { (corner, center) ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (center.x - halfHandlePx).roundToInt(),
                            (center.y - halfHandlePx).roundToInt()
                        )
                    }
                    .size(handleSize)
                    .semantics { contentDescription = handleDesc }
                    .pointerInput(
                        corner,
                        fullWidthPx,
                        fullHeightPx,
                        safeLeftPx,
                        safeTopPx,
                        safeRightPx,
                        safeBottomPx,
                        minZoneWidthPx,
                        minZoneHeightPx
                    ) {
                        detectDragGestures(
                            onDragStart = { isEditingZone = true },
                            onDragEnd = { isEditingZone = false },
                            onDragCancel = { isEditingZone = false }
                        ) { change, dragAmount ->
                            change.consume()
                            var l = latestFrameLeft
                            var t = latestFrameTop
                            var r = latestFrameRight
                            var b = latestFrameBottom

                            when (corner) {
                                ZoneCorner.TOP_LEFT -> {
                                    l = (l + dragAmount.x).coerceIn(safeLeftPx, r - minZoneWidthPx)
                                    t = (t + dragAmount.y).coerceIn(safeTopPx, b - minZoneHeightPx)
                                }
                                ZoneCorner.TOP_RIGHT -> {
                                    r = (r + dragAmount.x).coerceIn(l + minZoneWidthPx, safeRightPx)
                                    t = (t + dragAmount.y).coerceIn(safeTopPx, b - minZoneHeightPx)
                                }
                                ZoneCorner.BOTTOM_LEFT -> {
                                    l = (l + dragAmount.x).coerceIn(safeLeftPx, r - minZoneWidthPx)
                                    b = (b + dragAmount.y).coerceIn(t + minZoneHeightPx, safeBottomPx)
                                }
                                ZoneCorner.BOTTOM_RIGHT -> {
                                    r = (r + dragAmount.x).coerceIn(l + minZoneWidthPx, safeRightPx)
                                    b = (b + dragAmount.y).coerceIn(t + minZoneHeightPx, safeBottomPx)
                                }
                            }

                            latestOnScanZoneChange(
                                Rect(
                                    left = l / fullWidthPx,
                                    top = t / fullHeightPx,
                                    right = r / fullWidthPx,
                                    bottom = b / fullHeightPx
                                )
                            )
                        }
                    }
                    .testTag("camera_zone_handle_${corner.name.lowercase()}")
            )
        }

        // Top action bar: Close, Title, Torch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.52f),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .testTag("camera_close_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // AI vs Fast offline mode segmented pill
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.56f),
                modifier = Modifier.testTag("camera_mode_toggle_pill")
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CameraModeChip(
                        label = stringResource(R.string.camera_scan_mode_ai),
                        icon = Icons.Rounded.AutoAwesome,
                        selected = useAiMode,
                        onClick = { onToggleAiMode(true) },
                        modifier = Modifier.testTag("camera_mode_ai_chip")
                    )
                    CameraModeChip(
                        label = stringResource(R.string.camera_scan_mode_fast),
                        icon = Icons.Rounded.Bolt,
                        selected = !useAiMode,
                        onClick = { onToggleAiMode(false) },
                        modifier = Modifier.testTag("camera_mode_fast_chip")
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isTorchOn) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Black.copy(alpha = 0.52f)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleTorch)
                    .testTag("camera_flash_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = stringResource(R.string.cd_camera_flash),
                        tint = if (isTorchOn) MaterialTheme.colorScheme.onPrimary else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom capture controls: optional reset zone pill + hint pill + tactile shutter button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isScanZoneModified && !isProcessing) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onResetScanZone)
                        .testTag("camera_reset_zone_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.camera_scan_reset_zone),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = if (errorResId != null) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                } else {
                    Color.Black.copy(alpha = 0.58f)
                }
            ) {
                Text(
                    text = when {
                        isProcessing -> stringResource(R.string.camera_scan_processing)
                        errorResId != null -> stringResource(errorResId)
                        else -> stringResource(R.string.camera_scan_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorResId != null) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        Color.White
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            ShutterButton(
                isProcessing = isProcessing,
                onClick = onCaptureClick
            )
        }
    }
}

@Composable
private fun ShutterButton(
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val innerScale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shutter_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(width = 4.dp, color = Color.White.copy(alpha = 0.92f), shape = CircleShape)
            .clickable(
                enabled = !isProcessing,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("camera_shutter_button")
    ) {
        Surface(
            shape = CircleShape,
            color = if (isProcessing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = innerScale
                    scaleY = innerScale
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = stringResource(R.string.camera_scan_shutter),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(160),
        label = "camera_mode_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            Color.White.copy(alpha = 0.86f)
        },
        animationSpec = tween(160),
        label = "camera_mode_fg"
    )

    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun CapturedPhotoSelectorView(
    bitmap: Bitmap,
    analysisState: CameraScanAnalysisState,
    selectedExpressionId: String?,
    onSelectExpression: (String) -> Unit,
    onRetryAi: () -> Unit,
    onFallbackOffline: () -> Unit,
    onRetake: () -> Unit,
    onTransfer: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val listState = rememberLazyListState()

    val successState = analysisState as? CameraScanAnalysisState.Success
    val expressions = successState?.expressions.orEmpty()

    val selectedIndex = remember(expressions, selectedExpressionId) {
        expressions.indexOfFirst { it.id == selectedExpressionId }.coerceAtLeast(0)
    }

    LaunchedEffect(selectedExpressionId, expressions) {
        if (expressions.isNotEmpty() && selectedIndex in expressions.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar over review photo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .testTag("camera_close_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (analysisState) {
                        is CameraScanAnalysisState.Loading, CameraScanAnalysisState.Idle -> {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.camera_scan_ai_analyzing),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is CameraScanAnalysisState.Error -> {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.camera_scan_ai_error_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is CameraScanAnalysisState.Success -> {
                            Text(
                                text = if (expressions.isEmpty()) {
                                    stringResource(R.string.camera_scan_none_found)
                                } else {
                                    stringResource(R.string.camera_scan_found_count, expressions.size)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (analysisState.usedAi) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.camera_scan_ai_badge),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Spacer to keep status pill centered
            Spacer(modifier = Modifier.size(44.dp))
        }

        // Captured photo with clickable bounding boxes mapped pixel-accurately
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val maxH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            val bmpW = bitmap.width.toFloat().coerceAtLeast(1f)
            val bmpH = bitmap.height.toFloat().coerceAtLeast(1f)
            val scale = minOf(maxW / bmpW, maxH / bmpH)
            val renderedW = bmpW * scale
            val renderedH = bmpH * scale

            val renderedWDp = with(density) { renderedW.toDp() }
            val renderedHDp = with(density) { renderedH.toDp() }

            Box(
                modifier = Modifier
                    .size(width = renderedWDp, height = renderedHDp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // Interactive bounding boxes for every detected expression
                expressions.forEachIndexed { index, expr ->
                    val box = expr.boundingBox ?: return@forEachIndexed
                    val isSelected = expr.id == selectedExpressionId

                    val padPx = with(density) { 6.dp.toPx() }
                    val minTapPx = with(density) { 48.dp.toPx() }

                    val rawLeft = (box.left * scale - padPx).coerceAtLeast(0f)
                    val rawTop = (box.top * scale - padPx).coerceAtLeast(0f)
                    val rawRight = (box.right * scale + padPx).coerceAtMost(renderedW)
                    val rawBottom = (box.bottom * scale + padPx).coerceAtMost(renderedH)

                    val boxW = (rawRight - rawLeft).coerceAtLeast(minTapPx * 0.65f)
                    val boxH = (rawBottom - rawTop).coerceAtLeast(minTapPx * 0.55f)

                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White.copy(alpha = 0.88f)
                        },
                        animationSpec = tween(160),
                        label = "ocr_box_border"
                    )
                    val fillColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                        } else {
                            Color.Black.copy(alpha = 0.14f)
                        },
                        animationSpec = tween(160),
                        label = "ocr_box_fill"
                    )

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(rawLeft.roundToInt(), rawTop.roundToInt())
                            }
                            .size(
                                width = with(density) { boxW.toDp() },
                                height = with(density) { boxH.toDp() }
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(fillColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.5.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectExpression(expr.id) }
                            .testTag("camera_photo_box_$index")
                    ) {
                        // Number badge on the top-left of the bounding box
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Black.copy(alpha = 0.72f)
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Material 3 Expressive Selection & Action Card
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (analysisState) {
                    is CameraScanAnalysisState.Loading, CameraScanAnalysisState.Idle -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(30.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.camera_scan_ai_analyzing),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.camera_scan_processing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onRetake,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("camera_retake_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.camera_scan_retake))
                            }

                            FilledTonalButton(
                                onClick = onFallbackOffline,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("camera_offline_fallback_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.camera_scan_offline_fallback))
                            }
                        }
                    }

                    is CameraScanAnalysisState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.camera_scan_ai_error_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = analysisState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("camera_ai_error_message")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onFallbackOffline,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("camera_offline_fallback_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.camera_scan_offline_fallback),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = onRetryAi,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("camera_ai_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.camera_scan_ai_retry),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        TextButton(
                            onClick = onRetake,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("camera_retake_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.camera_scan_retake))
                        }
                    }

                    is CameraScanAnalysisState.Success -> {
                        if (expressions.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.camera_scan_none_found),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.camera_scan_none_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = if (analysisState.usedAi) onFallbackOffline else onRetryAi,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (analysisState.usedAi) Icons.Rounded.Bolt else Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(
                                            if (analysisState.usedAi) R.string.camera_scan_offline_fallback
                                            else R.string.camera_scan_ai_retry
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = onRetake,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("camera_retake_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = stringResource(R.string.camera_scan_retake))
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.camera_scan_select_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (analysisState.usedAi) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.camera_scan_ai_badge),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.camera_scan_pick_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 176.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    items = expressions,
                                    key = { _, item -> item.id }
                                ) { index, expr ->
                                    val isSelected = expr.id == selectedExpressionId
                                    ExpressionCandidateRow(
                                        index = index + 1,
                                        expression = expr,
                                        selected = isSelected,
                                        onClick = { onSelectExpression(expr.id) },
                                        modifier = Modifier.testTag("camera_candidate_row_$index")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = onRetake,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("camera_retake_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.camera_scan_retake),
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = onTransfer,
                                    enabled = selectedExpressionId != null,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("camera_transfer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.camera_scan_transfer_short),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressionCandidateRow(
    index: Int,
    expression: ScannedMathExpression,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(160),
        label = "candidate_bg"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = expression.formattedExpression,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (expression.previewResult.isNotEmpty()) {
                    Text(
                        text = "= ${expression.previewResult}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    canShowRationale: Boolean,
    hasRequestedOnce: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.camera_permission_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.camera_permission_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

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
                            if (showSettingsFallback) R.string.camera_permission_settings
                            else R.string.camera_permission_grant
                        )
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

private fun cropAndRotateBitmap(
    source: Bitmap,
    cropRect: android.graphics.Rect,
    rotationDegrees: Int
): Bitmap {
    val left = cropRect.left.coerceIn(0, (source.width - 1).coerceAtLeast(0))
    val top = cropRect.top.coerceIn(0, (source.height - 1).coerceAtLeast(0))
    val width = cropRect.width().coerceIn(1, (source.width - left).coerceAtLeast(1))
    val height = cropRect.height().coerceIn(1, (source.height - top).coerceAtLeast(1))

    if (rotationDegrees == 0 && left == 0 && top == 0 && width == source.width && height == source.height) {
        return source
    }

    val matrix = Matrix().apply {
        if (rotationDegrees != 0) {
            postRotate(rotationDegrees.toFloat())
        }
    }
    return Bitmap.createBitmap(source, left, top, width, height, matrix, true)
}

private fun cropToViewport(
    bitmap: Bitmap,
    viewportSize: IntSize
): Bitmap {
    if (viewportSize.width <= 0 || viewportSize.height <= 0) return bitmap
    val targetAspect = viewportSize.width.toFloat() / viewportSize.height.toFloat()
    val bmpAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    if (kotlin.math.abs(targetAspect - bmpAspect) <= 0.02f) return bitmap

    val (cropW, cropH) = if (bmpAspect > targetAspect) {
        (bitmap.height * targetAspect).roundToInt().coerceIn(1, bitmap.width) to bitmap.height
    } else {
        bitmap.width to (bitmap.width / targetAspect).roundToInt().coerceIn(1, bitmap.height)
    }
    val offsetX = ((bitmap.width - cropW) / 2).coerceAtLeast(0)
    val offsetY = ((bitmap.height - cropH) / 2).coerceAtLeast(0)
    return Bitmap.createBitmap(bitmap, offsetX, offsetY, cropW, cropH)
}

private fun cropToNormalizedZoneAndScale(
    bitmap: Bitmap,
    normalizedZone: Rect,
    maxDimension: Int
): Bitmap {
    val bmpW = bitmap.width.coerceAtLeast(1)
    val bmpH = bitmap.height.coerceAtLeast(1)

    val left = (normalizedZone.left * bmpW).roundToInt().coerceIn(0, (bmpW - 2).coerceAtLeast(0))
    val top = (normalizedZone.top * bmpH).roundToInt().coerceIn(0, (bmpH - 2).coerceAtLeast(0))
    val right = (normalizedZone.right * bmpW).roundToInt().coerceIn(left + 1, bmpW)
    val bottom = (normalizedZone.bottom * bmpH).roundToInt().coerceIn(top + 1, bmpH)

    val cropW = (right - left).coerceAtLeast(1)
    val cropH = (bottom - top).coerceAtLeast(1)

    var current = if (left == 0 && top == 0 && cropW == bmpW && cropH == bmpH) {
        bitmap
    } else {
        Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
    }

    val maxSide = maxOf(current.width, current.height)
    if (maxSide > maxDimension) {
        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val targetW = (current.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (current.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(current, targetW, targetH, true)
        if (scaled !== current && current !== bitmap) {
            current.recycle()
        }
        current = scaled
    }

    return current
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
