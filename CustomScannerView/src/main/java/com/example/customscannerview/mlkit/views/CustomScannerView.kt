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
import com.example.customscannerview.mlkit.BitmapUtils.imageToBitmap
import com.example.customscannerview.mlkit.enums.ViewType
import com.example.customscannerview.mlkit.interfaces.OCRResult
import com.example.customscannerview.mlkit.interfaces.OCRResultQA
import com.example.customscannerview.mlkit.interfaces.OnScanResult
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
    var selectedViewType: ViewType = ViewType.RECTANGLE
    private lateinit var cameraControls: CameraControl
    val barcodeResultSingle = MutableLiveData<Barcode>()
    val textIndicator = MutableLiveData<Text>()
    val multipleBarcodes = MutableLiveData<MutableList<Barcode>>()
    val barcodeIndicators = MutableLiveData<MutableList<Barcode>>()
    private val testBarcodes = mutableListOf<Barcode>()


    fun startScanning(viewType: ViewType) {
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
                scanningWindow.setRectangleViewFinder(configuration.barcodeWindow)
            }
            addView(scanningWindow)
            scanningWindow.visibility = View.VISIBLE
            initiateCamera(viewType)
        } else if (viewType == ViewType.SQUARE) {
            scanningWindow.post {
                scanningWindow.setSquareViewFinder(configuration.qrCodeWindow)
            }

            addView(scanningWindow)
            scanningWindow.visibility = View.VISIBLE
            initiateCamera(viewType)
        } else if (viewType == ViewType.FULLSCRREN) {
            scanningWindow.visibility = GONE
            initiateCamera(viewType)
        }

    }

    private fun initiateCamera(viewType: ViewType) {
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
                bindPreview(cameraProvider)
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun getScanningRect(): RectF? {
        return scanningWindow.scanningBoxRect
    }

    @SuppressLint("RestrictedApi")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        removeAllViews()
        addView(previewView)
        preview?.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(width, height))
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST).build()
        cameraProvider.unbindAll()
        imageAnalysis.setAnalyzer(cameraExecutor!!, analyzer)
        cameraProvider.bindToLifecycle(
            context as LifecycleOwner, cameraSelector, imageCapture, imageAnalysis, preview
        )
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
        textIndicator.postValue(text)
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
        barcodeIndicators.postValue(barCodeResult)
    }


    override fun onMultiBarcodesDetected(barcodes: List<Barcode>) {
        testBarcodes.clear()
        testBarcodes.addAll(barcodes)
        multipleBarcodes.postValue(barcodes as MutableList<Barcode>)
    }

    override fun onSomeTextDetected(text: Text) {
        textIndicator.postValue(text)
    }


    fun captureImage(captureCallback: CaptureCallback) {
        imageCapture.takePicture(cameraExecutor!!, object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                var bitmap = imageToBitmap(imageProxy)
                bitmap = fixOrientation(bitmap!!)
                imageView = ImageView(context)
                val params = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                imageView.layoutParams = params
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                captureCallback.onImageCaptured(bitmap, multipleBarcodes.value)
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

    private val repository = OCRRepository()

    fun makeOCRApiCall(bitmap: Bitmap, barcodeList: List<Barcode>, onScanResult: OCRResult) {
        CoroutineScope(Dispatchers.Main).launch {
            imageView.setImageBitmap(bitmap)
            addView(imageView)
        }
        CoroutineScope(Dispatchers.IO).launch {
            val string64 = BitmapUtils.convertBitmapToBase64(bitmap).toString()
            ocrcall(onScanResult, string64, barcodeList)
        }
    }


//    fun makeQAOCRApiCall(bitmap: Bitmap, barcodeList: List<Barcode>, onScanResult: OCRResultQA) {
//        CoroutineScope(Dispatchers.Main).launch {
//            imageView.setImageBitmap(bitmap)
//            addView(imageView)
//        }
//        CoroutineScope(Dispatchers.IO).launch {
//            val string64 = BitmapUtils.convertBitmapToBase64(bitmap).toString()
//            ocrcallQA(onScanResult, string64, barcodeList)
//        }
//    }

    private suspend fun ocrcall(
        onScanResult: OCRResult,
        baseImage: String,
        barcodeList: List<Barcode>
    ) {
        try {
            val response =
                repository.analyseOCRAsync(
                    repository.getDemoRequest(
                        barcodeList, baseImage
                    )
                )
            withContext(Dispatchers.Main) {
                removeView(imageView)
                onScanResult.onOCRResponse(response)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onScanResult.onOCRResponseFailed(e)
            }
        }
    }

    private suspend fun ocrcallQA(
        onScanResult: OCRResultQA,
        baseImage: String,
        barcodeList: List<Barcode>
    ) {
        try {
            val response =
                repository.analyseOCRAsyncQA(
                    repository.getQARequest(
                        barcodeList, baseImage
                    )
                )
            withContext(Dispatchers.Main) {
                removeView(imageView)
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

    private var configuration = Configuration(
        barcodeWindow = ScanWindow(0f, 0f, 5f),
        qrCodeWindow = ScanWindow(0f, 0f, 5f),
    )

    fun setScanningWindowConfiguration(conf: Configuration) {
        this.configuration = conf
        when (selectedViewType) {
            ViewType.RECTANGLE -> scanningWindow.setRectangleViewFinder(configuration.barcodeWindow)
            ViewType.SQUARE -> scanningWindow.setSquareViewFinder(configuration.qrCodeWindow)
            ViewType.FULLSCRREN -> {}
        }
    }
}

interface CaptureCallback {
    fun onImageCaptured(bitmap: Bitmap, value: MutableList<Barcode>?)
}


data class Configuration(
    val barcodeWindow: ScanWindow,
    val qrCodeWindow: ScanWindow
)

data class ScanWindow(
    val width: Float=0f,
    val height: Float=0f,
    val radius: Float=0f,
    val verticalStartingPosition: Float = 0f
)