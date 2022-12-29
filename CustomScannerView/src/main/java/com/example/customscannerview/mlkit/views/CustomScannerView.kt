package com.example.customscannerview.mlkit.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.customscannerview.mlkit.*
import com.example.customscannerview.mlkit.BitmapUtils.convertBitmapToBase64
import com.example.customscannerview.mlkit.BitmapUtils.imageToBitmap
import com.example.customscannerview.mlkit.enums.ScanType
import com.example.customscannerview.mlkit.enums.ViewType
import com.example.customscannerview.mlkit.interfaces.OCRResult
import com.example.customscannerview.mlkit.interfaces.OnScanResult
import com.example.customscannerview.mlkit.service.OcrApiService
import com.example.customscannerview.mlkit.service.ServiceBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CustomScannerView(
    context: Context, private val attrs: AttributeSet?
) : FrameLayout(context, attrs), CameraXBarcodeCallback, CameraXTextCallback,
    CameraXMultiBarcodeCallback, OnScanResult {

    private lateinit var imageCapture: ImageCapture
    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
        ProcessCameraProvider.getInstance(context)
    private val lifecycleOwner = context as LifecycleOwner
    private lateinit var analyzer: ImageAnalyzer
    private lateinit var cameraSelector: CameraSelector
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private lateinit var cameraXViewModel: CameraXViewModel
    private var cameraExecutor: ExecutorService? = null
    lateinit var imageView: ImageView
    var imageProcessor: VisionImageProcessor? = null
    var scanningWindow = ScanningWindow(context, attrs)
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private lateinit var previewView: PreviewView
    private lateinit var graphicOverlay: GraphicOverlay
    lateinit var selectedViewType: ViewType
    private lateinit var cameraControls: CameraControl
    val barcodeResultSingle = MutableLiveData<Barcode>()
    val textResult = MutableLiveData<Text>()
    val multipleBarcodes = MutableLiveData<MutableList<Barcode>>()
    val onSomethingDetected = MutableLiveData<MutableList<Barcode>>()
    private val testBarcodes = mutableListOf<Barcode>()


    fun startScanning(viewType: ViewType, scanType: ScanType) {
        selectedViewType = viewType

        // design work
        removeAllViews()
        cameraProvider?.unbindAll()
        analyzer = ImageAnalyzer(this)
        previewView = PreviewView(context)
        addView(previewView)
        val builder = Preview.Builder()
        preview = builder.build()

        graphicOverlay = GraphicOverlay(context, attrs)
        addView(graphicOverlay)
        imageCapture = ImageCapture.Builder().build()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (viewType == ViewType.RECTANGLE) {
            scanningWindow.post {
                scanningWindow.setRectangleViewFinder()
            }
            addView(scanningWindow)
            scanningWindow.visibility = View.VISIBLE
            initiateCamera(viewType, scanType)
        } else if (viewType == ViewType.SQUARE) {
            scanningWindow.post {
                scanningWindow.setSquareViewFinder()
            }

            addView(scanningWindow)
            scanningWindow.visibility = View.GONE
            initiateCamera(viewType, scanType)
        } else if (viewType == ViewType.FULLSCRREN) {
            scanningWindow.visibility = GONE
            if (scanType == ScanType.FULL) {
                initiateCamera(viewType, scanType)
            } else {
                initiateCamera(viewType, scanType)
            }

        }

    }

    private fun initiateCamera(viewType: ViewType, scanType: ScanType) {
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        imageProcessor = BarcodeScannerProcessor(callback = this,
            textCallback = this,
            somethingDetected = this,
            getRectCallback = { getScanningRect() })

        if (viewType == ViewType.RECTANGLE || viewType == ViewType.SQUARE) {
            cameraXViewModel =
                ViewModelProvider(context as ViewModelStoreOwner)[CameraXViewModel::class.java]
            cameraXViewModel.processCameraProvider.observe(context as LifecycleOwner) { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                bindAllCameraUseCases(cameraSelector)
            }

        } else {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider, scanType)
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun getScanningRect(): RectF? {
        return scanningWindow.scanningBoxRect
    }

    @SuppressLint("RestrictedApi")
    private fun bindPreview(cameraProvider: ProcessCameraProvider, scanType: ScanType) {
        cameraProvider.unbindAll()
        removeAllViews()
        addView(previewView)
        preview?.setSurfaceProvider(previewView.surfaceProvider)
        val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(width, height))
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST).build()
        if (scanType == ScanType.OCR) {
            cameraProvider.unbindAll()
            imageAnalysis.setAnalyzer(cameraExecutor!!, analyzer)
            cameraProvider.bindToLifecycle(
                context as LifecycleOwner, cameraSelector, imageCapture, imageAnalysis, preview
            )
        } else if (scanType == ScanType.FULL) {
            imageAnalysis.setAnalyzer(cameraExecutor!!, analyzer)
            cameraControls = cameraProvider.bindToLifecycle(
                context as LifecycleOwner, cameraSelector, preview, imageAnalysis
            ).cameraControl
        }


    }

    private fun bindAllCameraUseCases(cameraSelector: CameraSelector) {
        bindPreviewUseCase(cameraSelector)
        bindAnalysisUseCase(cameraSelector)
    }

    private fun bindPreviewUseCase(cameraSelector: CameraSelector) {
        if (cameraProvider == null) {
            return
        }
        preview?.setSurfaceProvider(previewView.surfaceProvider)
        cameraControls = cameraProvider?.bindToLifecycle(
            context as LifecycleOwner, cameraSelector, preview
        )!!.cameraControl

    }

    @SuppressLint("RestrictedApi")
    private fun bindAnalysisUseCase(cameraSelector: CameraSelector) {
        val builder = ImageAnalysis.Builder().setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(width, height))
        val imageAnalysis = builder.build()
        needUpdateGraphicOverlayImageSourceInfo = true
        imageAnalysis.setAnalyzer(
            cameraExecutor!!
        ) { imageProxy: ImageProxy ->

            if (needUpdateGraphicOverlayImageSourceInfo) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, false)
                } else {
                    graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, false)
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }
            try {
                imageProcessor?.processImageProxy(imageProxy, graphicOverlay)
            } catch (e: MlKitException) {
                Log.e("TAG", "Failed to process image. Error: " + e.localizedMessage)
            }

        }
        cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
    }

    override fun onNewBarcodeScanned(barcode: Barcode) {
        barcodeResultSingle.postValue(barcode)
    }

    override fun onTextDetected(text: Text) {
        textResult.postValue(text)
    }

    override fun onMultiBarcodeScanned(barcodes: MutableList<Barcode>) {
        multipleBarcodes.postValue(barcodes)
    }

    fun stopScanning() {
        cameraExecutor?.shutdown()
    }

    fun enableTorch() {
        cameraControls.enableTorch(true)
    }

    fun disableTorch() {
        cameraControls.enableTorch(false)
    }

    override fun onViewDetected(barCodeResult: MutableList<Barcode>) {
        onSomethingDetected.postValue(barCodeResult)
    }


    override fun onMultiBarcodesDetected(barcodes: List<Barcode>) {
        testBarcodes.clear()
        testBarcodes.addAll(barcodes)
        multipleBarcodes.postValue(barcodes as MutableList<Barcode>)
    }

    override fun onSomeTextDetected(text: Text) {
        textResult.postValue(text)
    }


    fun captureImage(onScanResult: OCRResult) {
        imageCapture.takePicture(cameraExecutor!!, object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                var bitmap = imageToBitmap(imageProxy)
                bitmap = fixOrientation(bitmap!!)
                var string64: String
                imageView = ImageView(context)
                val params = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                imageView.layoutParams = params
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                CoroutineScope(Dispatchers.Main).launch {
                    imageView.setImageBitmap(bitmap)
                    addView(imageView)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    string64 = convertBitmapToBase64(bitmap!!).toString()
                    callOCR(onScanResult, string64)
                }

                imageProxy.close()

            }

            override fun onError(exception: ImageCaptureException) {
                Log.d("newTag", exception.message.toString())

            }
        })
    }

    private fun fixOrientation(mBitmap: Bitmap): Bitmap {
        return if (mBitmap.width > mBitmap.height) {
            val matrix = Matrix()
            matrix.postRotate(90F)
            Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.width, mBitmap.height, matrix, true)
        } else mBitmap
    }

    private val repository = OCRRepository(ServiceBuilder.buildService(OcrApiService::class.java))
    private val isQAVariant = true

    suspend fun callOCR(onScanResult: OCRResult, baseImage: String) {
        try {
            val response = repository.analyseOCRAsync(
                repository.getOCRRequest(
                    multipleBarcodes.value ?: emptyList(), baseImage, isQAVariant
                )
            )
            withContext(Dispatchers.Main) {
                onScanResult.onOCRResponse(response)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                removeView(imageView)
                onScanResult.onOCRResponseFailed(e)
            }
        }
    }
}