package com.bushy.health.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import com.bushy.health.ui.theme.MaterialExpressiveShapes
import java.util.concurrent.Executors

@Composable
fun BushyCameraScreen(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val view = LocalView.current
    val window = (context as? Activity)?.window ?: (view.context as? Activity)?.window

    DisposableEffect(Unit) {
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val originalStatusLight = insetsController.isAppearanceLightStatusBars
            val originalNavLight = insetsController.isAppearanceLightNavigationBars

            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false

            onDispose {
                insetsController.isAppearanceLightStatusBars = originalStatusLight
                insetsController.isAppearanceLightNavigationBars = originalNavLight
            }
        } else {
            onDispose { }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (hasCameraPermission) {
            CameraContent(
                onDismiss = onDismiss,
                onPhotoCaptured = onPhotoCaptured
            )
        } else {
            CameraPermissionState(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onDismiss = onDismiss
            )
        }
    }
}

@OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
@Composable
private fun CameraContent(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var selectedTargetRatio by remember { mutableFloatStateOf(1.0f) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var zoomState by remember { mutableStateOf<ZoomState?>(null) }

    DisposableEffect(cameraInfo) {
        val info = cameraInfo
        if (info != null) {
            val observer = Observer<ZoomState> { state ->
                zoomState = state
            }
            info.zoomState.observeForever(observer)
            onDispose {
                info.zoomState.removeObserver(observer)
            }
        } else {
            onDispose { }
        }
    }

    val minZoomRatio = zoomState?.minZoomRatio ?: 0.5f
    val maxZoomRatio = zoomState?.maxZoomRatio ?: 10f

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    fun discoverPhysicalBackLenses(ctx: Context): List<Pair<String, Float>> {
        val manager = ctx.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return emptyList()
        val list = mutableListOf<Pair<String, Float>>()
        try {
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val focal = focalLengths?.firstOrNull() ?: 4.5f
                    list.add(Pair(id, focal))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // Rebind CameraX whenever lensFacing or selected physical camera changes
    LaunchedEffect(lensFacing, selectedTargetRatio) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val previewBuilder = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)

            val captureBuilder = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    Camera2Interop.Extender(previewBuilder)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_ZOOM_RATIO, selectedTargetRatio)
                    Camera2Interop.Extender(captureBuilder)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_ZOOM_RATIO, selectedTargetRatio)
                } catch (e: Exception) {
                    Log.e("BushyCamera", "Camera2Interop zoom error", e)
                }
            }

            val preview = previewBuilder.build()
            val capture = captureBuilder.build()

            var targetSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                val physicalLenses = discoverPhysicalBackLenses(context)
                if (physicalLenses.isNotEmpty()) {
                    val sorted = physicalLenses.sortedBy { it.second }
                    val ultraWide = sorted.firstOrNull { it.second < 3.2f } ?: sorted.first()
                    val telephoto = sorted.lastOrNull { it.second > 8.0f } ?: sorted.last()
                    val mainLens = sorted.firstOrNull { it.second in 3.2f..8.0f } ?: sorted.first()

                    val targetPhysical = when {
                        selectedTargetRatio <= 0.6f -> ultraWide
                        selectedTargetRatio >= 2.8f -> telephoto
                        else -> mainLens
                    }

                    targetSelector = CameraSelector.Builder()
                        .addCameraFilter { cameraInfos ->
                            val matched = cameraInfos.filter { info ->
                                try {
                                    Camera2CameraInfo.from(info).cameraId == targetPhysical.first
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            if (matched.isNotEmpty()) matched else cameraInfos
                        }
                        .build()
                }
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    targetSelector,
                    preview,
                    capture
                )

                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
                imageCapture = capture

                val currentMin = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 0.5f
                val currentMax = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 10f
                val clampedZoom = selectedTargetRatio.coerceIn(currentMin, currentMax)

                try {
                    camera.cameraControl.setZoomRatio(clampedZoom)
                } catch (e: Exception) {
                    Log.e("BushyCamera", "setZoomRatio error", e)
                }

                previewView?.let {
                    preview.setSurfaceProvider(it.surfaceProvider)
                }
            } catch (e: Exception) {
                Log.e("BushyCamera", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Update flash mode
    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    val supportedLensRatios = remember(minZoomRatio, maxZoomRatio) {
        val candidateRatios = listOf(0.5f, 1.0f, 2.0f, 3.0f, 5.0f)
        val filtered = candidateRatios.filter { ratio ->
            ratio >= (minZoomRatio - 0.1f) && ratio <= (maxZoomRatio + 0.1f)
        }
        if (filtered.size > 1) filtered else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedBitmap == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Camera Bar (Close, Flash)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            triggerHaptic()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        val flashIcon = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        }
                        Icon(flashIcon, contentDescription = "Flash", tint = Color.White)
                    }
                }

                // Big 3:4 Live Viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF121212))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                previewView = this
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val pView = previewView ?: return@detectTapGestures
                                    val factory = pView.meteringPointFactory
                                    val point = factory.createPoint(offset.x, offset.y)
                                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                                    cameraControl?.startFocusAndMetering(action)
                                    triggerHaptic()
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Multi-Lens Zoom Selector Pills (In the gap below Viewfinder, above Shutter)
                if (supportedLensRatios.isNotEmpty() && lensFacing == CameraSelector.LENS_FACING_BACK) {
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            supportedLensRatios.forEach { ratio ->
                                val isSelected = (selectedTargetRatio - ratio).let { if (it < 0) -it else it } < 0.2f
                                val label = if (ratio == 0.5f) "0.5x" else if (ratio == 1.0f) "1.0x" else if (ratio == 2.0f) "2.0x" else if (ratio == 3.0f) "3.0x" else "5.0x"

                                Surface(
                                    onClick = {
                                        selectedTargetRatio = ratio
                                        val clampedRatio = ratio.coerceIn(minZoomRatio, maxZoomRatio)
                                        cameraControl?.setZoomRatio(clampedRatio)
                                        triggerHaptic()
                                    },
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom Shutter Controls Bar
                var isShutterPressed by remember { mutableStateOf(false) }
                val shutterScale by animateFloatAsState(
                    targetValue = if (isShutterPressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "ShutterScale"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(52.dp))

                    // Iconic Camera Capture Shutter Button
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .graphicsLayer {
                                scaleX = shutterScale
                                scaleY = shutterScale
                            }
                            .border(4.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isShutterPressed = true
                                        triggerHaptic()
                                        try {
                                            awaitRelease()
                                        } finally {
                                            isShutterPressed = false
                                        }
                                    },
                                    onTap = {
                                        if (!isCapturing) {
                                            isCapturing = true
                                            val capture = imageCapture ?: return@detectTapGestures
                                            capture.takePicture(
                                                cameraExecutor,
                                                object : ImageCapture.OnImageCapturedCallback() {
                                                    override fun onCaptureSuccess(image: ImageProxy) {
                                                        val buffer = image.planes[0].buffer
                                                        val bytes = ByteArray(buffer.remaining())
                                                        buffer.get(bytes)
                                                        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                                                        // Rotate bitmap properly
                                                        val matrix = Matrix()
                                                        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                                                        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                                            matrix.postScale(-1f, 1f) // Mirror selfie
                                                        }

                                                        val rotated = Bitmap.createBitmap(
                                                            rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                                        )

                                                        // Crop bitmap to exact 3:4 aspect ratio
                                                        val cropped = cropTo3By4(rotated)

                                                        image.close()
                                                        capturedBitmap = cropped
                                                        isCapturing = false
                                                    }

                                                    override fun onError(exception: ImageCaptureException) {
                                                        Log.e("BushyCamera", "Capture failed", exception)
                                                        isCapturing = false
                                                    }
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                    )

                    // Flip Camera (Selfie / Rear)
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                            selectedTargetRatio = 1.0f
                            triggerHaptic()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        } else {
            // Captured Meal Photo Review Screen in 3:4 frame
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(32.dp))
                ) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Captured Meal",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Action Bar (Retake vs Analyze Meal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            capturedBitmap = null
                            triggerHaptic()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retake", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Button(
                        onClick = {
                            val bitmap = capturedBitmap
                            if (bitmap != null) {
                                triggerHaptic()
                                onPhotoCaptured(bitmap)
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Meal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

private fun cropTo3By4(bitmap: Bitmap): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val targetHeight = (w * 4) / 3

    return if (targetHeight <= h) {
        val startY = (h - targetHeight) / 2
        Bitmap.createBitmap(bitmap, 0, startY, w, targetHeight)
    } else {
        val targetWidth = (h * 3) / 4
        val startX = (w - targetWidth) / 2
        Bitmap.createBitmap(bitmap, startX, 0, targetWidth, h)
    }
}

@Composable
private fun CameraPermissionState(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Camera Access Needed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "To take photos of your meals and track calories with Bushy Wushy AI, please grant camera permission.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onDismiss) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
