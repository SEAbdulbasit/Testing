package com.example.customscannerview.mlkit.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.customscannerview.mlkit.enums.ScanType
import com.example.customscannerview.mlkit.enums.ViewType
import com.example.customscannerview.mlkit.interfaces.OnScanResult
import com.example.customscannerview.mlkit.modelclasses.ocr_request.BarcodeX
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRQARequest
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRRequestParent
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OcrRequest
import com.example.customscannerview.mlkit.modelclasses.OCRResponseParent
import com.example.customscannerview.mlkit.service.OcrApiService
import com.example.customscannerview.mlkit.service.ServiceBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
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
    var rectangleView = RectangleView(context, attrs)
    var squareView = SquareView(context, attrs)
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private lateinit var previewView: PreviewView
    private lateinit var graphicOverlay: GraphicOverlay
    lateinit var selectedViewType: ViewType
    private lateinit var cameraControls: CameraControl
    val barcodeResultSingle = MutableLiveData<Barcode>()
    val textResult = MutableLiveData<Text>()
    val multipleBarcodes = MutableLiveData<MutableList<Barcode>>()
    val onSomethingDetected = MutableLiveData<MutableList<Barcode>>()
    val testBarcodes = mutableListOf<Barcode>()


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
            rectangleView.post {
                rectangleView.setRectangleViewFinder()
            }

            squareView.post {
                squareView.setSquareViewFinder()
            }
            addView(rectangleView)
            addView(squareView)
            squareView.visibility = View.GONE
            rectangleView.visibility = View.VISIBLE
            initiateCamera(viewType, scanType)
        } else if (viewType == ViewType.SQUARE) {
            rectangleView.post {
                rectangleView.setRectangleViewFinder()
            }

            squareView.post {
                squareView.setSquareViewFinder()
            }
            addView(rectangleView)
            addView(squareView)
            squareView.visibility = View.VISIBLE
            rectangleView.visibility = View.GONE
            initiateCamera(viewType, scanType)
        } else if (viewType == ViewType.FULLSCRREN) {
            rectangleView.visibility = GONE
            squareView.visibility = GONE
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
        return if (selectedViewType == ViewType.RECTANGLE) rectangleView.scanningBoxRect else squareView.scanningBoxRect
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


    override fun onOCRResponse(ocrResponse: OCRResponseParent?) {

    }

    override fun onOCRResponseFailed(throwable: Throwable?) {

    }

    override fun onSomeTextDetected(text: Text) {
        textResult.postValue(text)
    }


    fun captureImage(onScanResult: OnScanResult) {
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

    fun imageToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.NO_WRAP)
    }

    private val repository = Repository(ServiceBuilder.buildService(OcrApiService::class.java))
    private val isQAVariant = true

    suspend fun callOCR(onScanResult: OnScanResult, baseImage: String) {
        try {
            val response = repository.analyseOCRAsync(
                getOCRRequest(
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

    private fun getOCRRequest(
        barcodesList: List<Barcode>, baseImage: String, isQAVariant: Boolean
    ): OCRRequestParent {
        return if (isQAVariant) {
            OCRQARequest(
                barcode = BarcodeX(listOf()),
                callType = "extract",
                extractTime = "2022-08-29T05:58:28.902Z",
                image = baseImage,
                orgUuid = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                platform = "API"
            )

        } else {
            OcrRequest(image_url = "data:image/jpeg;base64,$baseImage",
                type = "shipping_label",
                barcode_values = barcodesList.map { it.displayValue!! })
        }
    }
}