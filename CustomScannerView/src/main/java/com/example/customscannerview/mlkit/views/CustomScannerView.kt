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
import com.example.customscannerview.mlkit.modelclasses.BoxSides
import com.example.scannerview.modelclasses.ocr_request.Frame
import com.example.scannerview.modelclasses.ocr_request.OcrRequest
import com.example.scannerview.modelclasses.ocr_response.OcrResponse
import com.example.customscannerview.mlkit.service.OcrApiService
import com.example.customscannerview.mlkit.service.ServiceBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.*
import retrofit2.Callback
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CustomScannerView(
    context: Context,
    val attrs: AttributeSet?
) :
    FrameLayout(context, attrs),
    CameraXBarcodeCallback,
    CameraXTextCallback,
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
    private var boxSides = BoxSides(0F, 0F, 0F, 0F)
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
        imageCapture = ImageCapture.Builder()
            .build()
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


        if (viewType == ViewType.RECTANGLE) {
            val overlayWidth = width.toFloat()
            val overlayHeight = height.toFloat()
            val boxWidth = overlayWidth * 75 / 100
            val boxHeight = overlayHeight * 20 / 100
            val cx = overlayWidth / 2
            val cy = overlayHeight / 2
            boxSides.boxLeftSide = cx - boxWidth / 2
            boxSides.boxTopSide = cy - boxHeight / 1.5f
            boxSides.boxRightSide = cx + boxWidth / 2
            boxSides.boxBottomSide = cy + boxHeight / 4.5f
        } else if (viewType == ViewType.SQUARE) {
            val overlayWidth = width.toFloat()
            val overlayHeight = height.toFloat()
            val boxWidth = overlayWidth * 62 / 100
            val boxHeight = overlayHeight * 28 / 100
            val cx = overlayWidth / 2
            val cy = overlayHeight / 2
            boxSides.boxLeftSide = cx - boxWidth / 2
            boxSides.boxTopSide = cy - boxHeight / 1.5f
            boxSides.boxRightSide = cx + boxWidth / 2
            boxSides.boxBottomSide = cy + boxHeight / 4.5f
        }



        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        imageProcessor = BarcodeScannerProcessor(
            this,
            boxSides,
            textCallback = this, this, this
        ) { getScanningRect() }
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
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(width, height))
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .build()
        if (scanType == ScanType.OCR) {
            cameraProvider.unbindAll()
            imageAnalysis.setAnalyzer(cameraExecutor!!, analyzer)
            cameraProvider.bindToLifecycle(
                context as LifecycleOwner, cameraSelector, imageCapture,
                imageAnalysis, preview
            )
        } else if (scanType == ScanType.FULL) {
            imageAnalysis.setAnalyzer(cameraExecutor!!, analyzer)
            cameraControls = cameraProvider.bindToLifecycle(
                context as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            ).cameraControl
        }


    }

    fun bindAllCameraUseCases(cameraSelector: CameraSelector) {
        bindPreviewUseCase(cameraSelector)
        bindAnalysisUseCase(cameraSelector)
    }

    private fun bindPreviewUseCase(cameraSelector: CameraSelector) {
        if (cameraProvider == null) {
            return
        }
        preview?.setSurfaceProvider(previewView.surfaceProvider)
        cameraControls =
            cameraProvider?.bindToLifecycle(
                context as LifecycleOwner,
                cameraSelector,
                preview
            )!!.cameraControl

    }

    @SuppressLint("RestrictedApi")
    private fun bindAnalysisUseCase(cameraSelector: CameraSelector) {
        val builder = ImageAnalysis.Builder()
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
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


    override fun onOCRResponse(ocrResponse: OcrResponse?) {

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
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
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
        } else
            mBitmap
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

    fun callOCR(onScanResult: OnScanResult, baseImage: String) {
        val retrofit = ServiceBuilder.buildService(OcrApiService::class.java)
        retrofit.analyseOCRR(
            OcrRequest(
                com.example.scannerview.modelclasses.ocr_request.Barcode(
                    listOf(
                        listOf(
                            Frame(
                                "",
                                ""
                            )
                        )
                    )
                ),
                "scan",
                "API",
                baseImage,
                false,
                7140,
                777644,
                2473793,
                "API",
                false,
                "2022-08-29T05:58:28.902Z",
                "tauqeer.sajid@yopmail.net"
            )
        ).enqueue(object : Callback<OcrResponse> {
            override fun onResponse(
                call: retrofit2.Call<OcrResponse>,
                response: retrofit2.Response<OcrResponse>
            ) {
                onScanResult.onOCRResponse(response.body())

            }

            override fun onFailure(call: retrofit2.Call<OcrResponse>, t: Throwable) {
                removeView(imageView)
                onScanResult.onOCRResponseFailed(t)

            }
        })
    }
}