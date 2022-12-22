package com.example.barcodescannernew

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.barcodescannernew.databinding.ActivityMainBinding
import com.example.barcodescannernew.databinding.BarcodeNotFoundViewBinding
import com.example.barcodescannernew.databinding.ResponseViewBinding
import com.example.barcodescannernew.model.BarcodeModel
import com.example.barcodescannernew.utils.ONE_DIMENSIONAL_FORMATS
import com.example.barcodescannernew.utils.TWO_DIMENSIONAL_FORMATS
import com.example.barcodescannernew.utils.copyToClipboard
import com.example.barcodescannernew.utils.getCarrierNameFromKey
import com.example.barcodescannernew.utils.hide
import com.example.barcodescannernew.utils.show
import com.example.customscannerview.mlkit.enums.ScanType
import com.example.customscannerview.mlkit.enums.ViewType
import com.example.customscannerview.mlkit.interfaces.OnScanResult
import com.example.customscannerview.mlkit.modelclasses.OCRResponseDemo
import com.example.customscannerview.mlkit.modelclasses.OCRResponseParent
import com.example.customscannerview.mlkit.modelclasses.OcrResponse
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), OnScanResult {
    lateinit var binding: ActivityMainBinding
    lateinit var barcodeNotFoundBinding: BarcodeNotFoundViewBinding
    private var doNotShowDialog: Boolean = false
    private var isManualModeActive: Boolean = false
    private var isResultEmpty: Boolean = false
    private lateinit var mediaPlayer: MediaPlayer
    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isSoundEnabled: Boolean = false
    private var isSilentEnabled: Boolean = false
    private var isVibrationEnabled: Boolean = false
    private var isFlashEnabled: Boolean = false
    private lateinit var preferences: SharedPreferences
    private var isOCREnabled: Boolean = false
    private lateinit var OCRDialog: Dialog
    private var dialogShowing: Boolean = false
    private lateinit var responseBinding: ResponseViewBinding
    private lateinit var resultDialog: AlertDialog
    private lateinit var failureDialog: AlertDialog
    private var isMultiDetectionEnabled: Boolean = false
    private val multiBarcodesDialog = MultiBarcodeFragment.newInstance("DialogFragment")
    private var barcodesListDetected = mutableListOf<BarcodeModel>()
    private lateinit var settingsFragment: Dialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }
        checkPreferences()
        initViews()
        setupRadioButtonListener()
        setUpClickListener()
        setUpNavigationListener()
        isManualModeActive = false
        runnable = Runnable {
            showErrorDialog()
            if (binding.progressBar.isVisible) binding.progressBar.visibility = View.GONE
            isManualModeActive = false
        }
        binding.customScannerView.barcodeResultSingle.observe(this) { barCodeResult ->


            if (!doNotShowDialog) {
                if (TWO_DIMENSIONAL_FORMATS.contains(barCodeResult.format)) {
                    if (binding.customScannerView.selectedViewType == ViewType.SQUARE) {
                        if (isManualModeActive) {
                            binding.progressBar.visibility = View.GONE
                            val inputString = barCodeResult.displayValue.toString()
                            val re = Regex("[^A-Za-z0-9(@)><&/. -]")
                            val answer = re.replace(inputString, "")
                            showDialog(answer)
                            handler.removeCallbacksAndMessages(null)
                            if (binding.btnSwitch.checkedRadioButtonId == R.id.radioManual) {
                                isManualModeActive = false
                            }
                        }
                    }
                } else if (ONE_DIMENSIONAL_FORMATS.contains(barCodeResult.format)) {
                    if (binding.customScannerView.selectedViewType == ViewType.RECTANGLE) if (isManualModeActive) {
                        val inputString = barCodeResult.displayValue.toString()
                        val re = Regex("[^A-Za-z0-9(@)><&/. -]")
                        val answer = re.replace(inputString, "")
                        showDialog(answer)
                        handler.removeCallbacksAndMessages(null)
                        binding.progressBar.visibility = View.GONE
                        if (binding.btnSwitch.checkedRadioButtonId == R.id.radioManual) {
                            isManualModeActive = false
                        }
                    }
                }
            }

        }
        binding.customScannerView.textResult.observe(this) { text ->
            if (text.textBlocks.isEmpty()) {
                binding.textDetector.setImageResource(R.drawable.ic_text_inactive)
            } else {
                binding.textDetector.setImageResource(R.drawable.ic_text_active)
            }
        }
        binding.customScannerView.onSomethingDetected.observe(this) { barcodeList ->
            if (barcodeList.size == 0) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                }
                binding.qrCodeDetector.setImageResource(R.drawable.ic_qr_inactive)
                binding.barCodeDetector.setImageResource(R.drawable.ic_br_inactive)

            } else {
                barcodeList.forEach {
                    if (TWO_DIMENSIONAL_FORMATS.contains(it.format)) {
                        if (isResultEmpty) {
                            binding.qrCodeDetector.setImageResource(R.drawable.ic_qr_active)
                        }
                    }
                    if (ONE_DIMENSIONAL_FORMATS.contains(it.format)) {
                        if (isResultEmpty) {
                            binding.barCodeDetector.setImageResource(R.drawable.ic_br_active)
                        }
                    }
                }
            }
            isResultEmpty = barcodeList.isEmpty()

        }
        binding.customScannerView.multipleBarcodes.observe(this) { barcodes ->
            barcodesListDetected.clear()
            if (barcodes.isNotEmpty()) {
                if (settingsFragment.findViewById<SwitchCompat>(R.id.btnSwitchSetting).isChecked) {
                    barcodes.forEach {
                        if (TWO_DIMENSIONAL_FORMATS.contains(it.format) && binding.bottomNav.selectedItemId == R.id.qrCode) {
                            val inputString = it.displayValue.toString()
                            val re = Regex("[^A-Za-z0-9(@)><&/. -]")
                            val answer = re.replace(inputString, "")
                            if (!barcodesListDetected.contains(BarcodeModel(answer))) {
                                barcodesListDetected.add(BarcodeModel(answer))
                                multiBarcodesDialog.barcodesList =
                                    barcodesListDetected.distinct() as MutableList<BarcodeModel>
                                if (isManualModeActive) {
                                    multiBarcodesDialog.show(supportFragmentManager, "DialogCustom")
                                    binding.progressBar.visibility = View.GONE
                                    handler.removeCallbacksAndMessages(null)
                                    if (binding.btnSwitch.checkedRadioButtonId == R.id.radioManual) {
                                        isManualModeActive = false
                                    }
                                }
                            }
                        } else if (ONE_DIMENSIONAL_FORMATS.contains(it.format) && binding.bottomNav.selectedItemId == R.id.barCode) {
                            val inputString = it.displayValue.toString()
                            val re = Regex("[^A-Za-z0-9(@)><&/. -]")
                            val answer = re.replace(inputString, "")
                            if (!barcodesListDetected.contains(BarcodeModel(answer))) {
                                barcodesListDetected.add(BarcodeModel(answer))
                                multiBarcodesDialog.barcodesList =
                                    barcodesListDetected.distinct() as MutableList<BarcodeModel>
                                if (isManualModeActive) {
                                    multiBarcodesDialog.show(supportFragmentManager, "DialogCustom")
                                    binding.progressBar.visibility = View.GONE
                                    handler.removeCallbacksAndMessages(null)
                                    if (binding.btnSwitch.checkedRadioButtonId == R.id.radioManual) {
                                        isManualModeActive = false
                                    }
                                }
                            }
                        }/*else{
                            if(isManualModeActive){
                                isManualModeActive=false
                                showErrorDialog()
                            }
                        }*/

                    }

                }
            }
        }

    }


    private fun showErrorDialog() {
        val view = binding.customScannerView.selectedViewType
        val v = barcodeNotFoundBinding.root
        var name = "something"
        if (view == ViewType.SQUARE) {
            name = "QR code"
        } else if (view == ViewType.RECTANGLE) {
            name = "Barcode"
        }
        doNotShowDialog = true
        failureDialog.setView(v)
        failureDialog.setTitle("No $name Found")
        failureDialog.setMessage("Please capture photo when $name indicator is active or manually enter the code")
        failureDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE,
            "OK"
        ) { dialog, _ ->
            val code = v.findViewById<EditText>(R.id.inputValue).text.toString()
            doNotShowDialog = false
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep_sound)
            applicationContext.copyToClipboard(code)
            dialog?.dismiss()
        }
        failureDialog.setCanceledOnTouchOutside(true)
        failureDialog.setOnCancelListener {
            doNotShowDialog = false
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep_sound)
            it?.dismiss()
        }
        failureDialog.show()
    }

    private fun setUpClickListener() {
        binding.camIcon.setOnClickListener {
            if (isOCREnabled) {
                if (!isMultiDetectionEnabled) {
                    binding.progressBar.bringToFront()
                    binding.progressBar.show()
                    binding.customScannerView.captureImage(this)
                    binding.camIcon.isEnabled = false
                    showOCRDialog()
                } else {
                    if (!binding.progressBar.isVisible) {
                        binding.progressBar.visibility = View.VISIBLE
                        handler.postDelayed({
                            runnable.run()
                        }, 1500)
                        isManualModeActive = true
                    }
                }
            } else {
                if (!binding.progressBar.isVisible) {
                    binding.progressBar.visibility = View.VISIBLE
                    handler.postDelayed({
                        runnable.run()
                    }, 1500)
                    isManualModeActive = true
                }
            }
        }
        binding.btnSettings.setOnClickListener {
            settingsFragment.show()
        }
        binding.flashIcon.setOnClickListener {
            isFlashEnabled = if (isFlashEnabled) {
                binding.flashIcon.setImageResource(R.drawable.ic_flash_inactive)
                binding.customScannerView.disableTorch()
                false
            } else {
                binding.flashIcon.setImageResource(R.drawable.ic_flash_active)
                binding.customScannerView.enableTorch()
                true
            }
        }
        binding.soundIcon.setOnClickListener {
            if (isSoundEnabled) {
                setPreferences("Vibration")
                mediaPlayer.stop()
                checkPreferences()
                isSoundEnabled = false
                isSilentEnabled = false
                binding.soundIcon.setImageResource(R.drawable.ic_vibration)
            } else if (isVibrationEnabled) {
//                mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound)
                setPreferences("Silent")
                isSilentEnabled = false
                isVibrationEnabled = false
                checkPreferences()
                binding.soundIcon.setImageResource(R.drawable.ic_sound_inactive)
            } else {
                isVibrationEnabled = false
                isSilentEnabled = false
                mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound)
                setPreferences("Sound")
                checkPreferences()
                binding.soundIcon.setImageResource(R.drawable.ic_sound_active)
            }
        }

    }

    private fun setUpNavigationListener() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.barCode -> {
                    isOCREnabled = false
                    binding.btnSwitch.visibility = View.VISIBLE
                    binding.soundIcon.visibility = View.VISIBLE
                    binding.customScannerView.stopScanning()
                    if (settingsFragment.findViewById<SwitchCompat>(R.id.btnSwitchSetting).isChecked) {
                        binding.btnSwitch.visibility = View.GONE
                        binding.customScannerView.startScanning(ViewType.FULLSCRREN, ScanType.FULL)
                    } else {
                        if (binding.btnSwitch.checkedRadioButtonId == R.id.radioAuto) {
                            binding.customScannerView.startScanning(
                                ViewType.RECTANGLE, ScanType.AUTO
                            )
                        } else {
                            binding.customScannerView.startScanning(
                                ViewType.RECTANGLE, ScanType.MANUAL
                            )
                        }
                    }

                    true
                }

                R.id.qrCode -> {
                    isOCREnabled = false
                    binding.btnSwitch.visibility = View.VISIBLE
                    binding.soundIcon.visibility = View.VISIBLE
                    binding.customScannerView.stopScanning()
                    if (settingsFragment.findViewById<SwitchCompat>(R.id.btnSwitchSetting).isChecked) {
                        binding.customScannerView.startScanning(ViewType.FULLSCRREN, ScanType.FULL)
                        binding.btnSwitch.visibility = View.GONE
                    } else {
                        if (binding.btnSwitch.checkedRadioButtonId == R.id.radioAuto) {
                            binding.customScannerView.startScanning(ViewType.SQUARE, ScanType.AUTO)

                        } else {
                            binding.customScannerView.startScanning(
                                ViewType.SQUARE, ScanType.MANUAL
                            )
                        }
                    }

                    true
                }

                R.id.ocr -> {
                    binding.radioManual.isChecked = true
                    binding.btnSwitch.visibility = View.GONE
                    isOCREnabled = true
                    binding.customScannerView.stopScanning()
                    binding.customScannerView.startScanning(ViewType.FULLSCRREN, ScanType.OCR)
                    binding.camIcon.visibility = View.VISIBLE
                    binding.soundIcon.visibility = View.GONE
                    true
                }

                else -> {
                    false
                }
            }
        }
    }

    private fun setupRadioButtonListener() {
        binding.btnSwitch.setOnCheckedChangeListener { _, item ->
            when (item) {
                R.id.radioManual -> {
                    isManualModeActive = false
                    binding.camIcon.visibility = View.VISIBLE
                }

                R.id.radioAuto -> {
                    isManualModeActive = true
                    binding.camIcon.visibility = View.GONE
                }
            }
        }
    }


    // GET PERMISSIONS //
    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissionsToRequest.toTypedArray(), PERMISSION_REQUESTS
            )
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    companion object {
        private const val TAG = "MLBarcodeScanner"
        private const val PERMISSION_REQUESTS = 1
        private val REQUIRED_RUNTIME_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private fun showDialog(message: String) {
        doNotShowDialog = true
        resultDialog.setMessage(message)
        resultDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "Copy"
        ) { dialog, _ ->
            applicationContext.copyToClipboard(message)
            doNotShowDialog = false
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep_sound)
            dialog?.dismiss()
        }
        resultDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE,
            "Cancel"
        ) { dialog, _ ->
            doNotShowDialog = false
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep_sound)
            dialog?.dismiss()
        }
        resultDialog.show()

        if (isSoundEnabled) {
            mediaPlayer.start()

        } else if (isVibrationEnabled) {
            val vibrator = applicationContext?.getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        400, VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(400)
            }

        }
    }

    private fun showOCRDialog() {
        responseBinding.scrollView.visibility = View.GONE
        OCRDialog.setContentView(responseBinding.root)
        OCRDialog.setCancelable(false)
        dialogShowing = true
        OCRDialog.setCanceledOnTouchOutside(true)
        responseBinding.progressBar.visibility = View.VISIBLE
        responseBinding.scrollView.visibility = View.GONE
        responseBinding.btnBack.visibility = View.GONE
        responseBinding.textScanning.text = "Processing..."
        OCRDialog.setOnCancelListener {
            binding.camIcon.isEnabled = true
            binding.customScannerView.imageView.hide()
            if (binding.progressBar.isVisible) {
                binding.progressBar.hide()
            }
        }
        OCRDialog.show()
    }

    private fun checkPreferences() {
        preferences = getSharedPreferences(application.packageName, MODE_PRIVATE)

//        isSoundEnabled = preferences.getBoolean("soundEnabled", false)
        when (preferences.getString("option", null)) {
            "Sound" -> {
                isSoundEnabled = true
            }

            "Vibration" -> {
                isVibrationEnabled = true
            }

            "Silent" -> {
                isSilentEnabled = true
            }
        }
    }

    private fun setPreferences(option: String) {
        preferences = getSharedPreferences(application.packageName, MODE_PRIVATE)
        preferences.edit {
            putString("option", option)
        }
    }


    private fun initViews() {
        barcodeNotFoundBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this), R.layout.barcode_not_found_view, null, false
        )
        Handler(Looper.getMainLooper()).postDelayed({

        }, 1000)
        binding.customScannerView.startScanning(ViewType.RECTANGLE, ScanType.MANUAL)
        mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound)
        if (isSoundEnabled) {
            binding.soundIcon.setImageResource(R.drawable.ic_sound_active)
        } else if (isVibrationEnabled) {
            binding.soundIcon.setImageResource(R.drawable.ic_vibration)
        }
        if (isFlashEnabled) {
            binding.flashIcon.setImageResource(R.drawable.ic_flash_inactive)
            binding.customScannerView.disableTorch()
        }
        OCRDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        responseBinding =
            DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.response_view, null, false)
        resultDialog =
            AlertDialog.Builder(this).setCancelable(false).setTitle("SCAN RESULT").create()
        failureDialog =
            AlertDialog.Builder(this).setCancelable(false).setTitle("SCAN FAILED").create()/* multiBarcodesDialog.dialog?.setOnCancelListener {
             binding.camIcon.isEnabled = true
             binding.scannerView.imageView.hide()
         }*/
        settingsFragment = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val v = LayoutInflater.from(this).inflate(R.layout.setting_sheet_view, null, false)
        settingsFragment?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        settingsFragment?.setCancelable(false)
        settingsFragment.setCanceledOnTouchOutside(true)
        settingsFragment.setContentView(v)
        settingsFragment.setOnCancelListener {
            if (settingsFragment.findViewById<SwitchCompat>(R.id.btnSwitchSetting).isChecked && binding.bottomNav.selectedItemId != R.id.ocr) {
                binding.customScannerView.startScanning(ViewType.FULLSCRREN, ScanType.FULL)
            }
        }

        settingsFragment.findViewById<ImageView>(R.id.btnDownSetting).setOnClickListener {
            settingsFragment.dismiss()
        }/*settingsFragment.findViewById<ImageView>(R.id.btnDownSetting)
            .setOnClickListener {
                if(settingsFragment.findViewById<SwitchCompat>(R.id.btnSwitchSetting).isChecked){
                    binding.customScannerView.startScanning(ViewType.FULLSCRREN,ScanType.FULL)
                    isMultiDetectionEnabled=true
                    binding.btnSwitch.visibility=View.GONE
                }
                else{
                binding.btnSwitch.checkedRadioButtonId==R.id.radioManual
                binding.btnSwitch.visibility=View.VISIBLE
                settingsFragment.dismiss()
                    isMultiDetectionEnabled=false
                binding.customScannerView.startScanning(ViewType.RECTANGLE,ScanType.MANUAL)
                }
            }*/
        settingsFragment.findViewById<SwitchCompat>(R.id.btnSwitchSetting)
            .setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {/*binding.customScannerView.startScanning(ViewType.FULLSCRREN,ScanType.FULL)
                    binding.btnSwitch.visibility=View.GONE*/
                    binding.btnSwitch.check(R.id.radioManual)
                    if (binding.bottomNav.selectedItemId != R.id.ocr) {
                        binding.customScannerView.startScanning(ViewType.FULLSCRREN, ScanType.FULL)
                        binding.customScannerView.startScanning(ViewType.FULLSCRREN, ScanType.FULL)
                        binding.btnSwitch.visibility = View.GONE
                    }
//                isMultiDetectionEnabled=true
                } else {

                    if (binding.bottomNav.selectedItemId == R.id.barCode) {
                        binding.btnSwitch.visibility = View.VISIBLE
                        if (binding.btnSwitch.checkedRadioButtonId == R.id.radioManual) {

                            binding.customScannerView.startScanning(
                                ViewType.RECTANGLE, ScanType.MANUAL
                            )
                        } else {
                            binding.customScannerView.startScanning(
                                ViewType.RECTANGLE, ScanType.AUTO
                            )

                        }
                    } else if (binding.bottomNav.selectedItemId == R.id.qrCode) {
                        binding.btnSwitch.visibility = View.VISIBLE
                        if (binding.btnSwitch.checkedRadioButtonId == R.id.radioManual) {

                            binding.customScannerView.startScanning(
                                ViewType.SQUARE, ScanType.MANUAL
                            )
                        } else {
                            binding.customScannerView.startScanning(ViewType.SQUARE, ScanType.AUTO)
                        }
                    }
                }
            }


    }

    private fun showOCRResult(ocrResponse: OcrResponse) {
        val meta = mutableListOf<String>()
        responseBinding.metadata.text = ""
        responseBinding.scrollView.visibility = View.VISIBLE
        responseBinding.btnBack.visibility = View.VISIBLE
        responseBinding.progressBar.visibility = View.INVISIBLE
        responseBinding.textScanning.text = "Scanned"
        if (ocrResponse.data != null) {

            val trackingNo = ocrResponse.data?.output?.scanOutput?.courierInfo?.trackingNo
            if (!trackingNo.isNullOrEmpty()) {
                responseBinding.trackingNo.text = trackingNo
                responseBinding.textTrackingNo.visibility = View.VISIBLE
                responseBinding.trackingNo.visibility = View.VISIBLE
            } else {
                responseBinding.textTrackingNo.visibility = View.GONE
                responseBinding.trackingNo.visibility = View.GONE
            }

            val shipmentType = ocrResponse.data?.output?.scanOutput?.courierInfo?.shipmentType
            if (!shipmentType.isNullOrEmpty()) {
                responseBinding.shipmentType.text = shipmentType
                responseBinding.shipmentType.visibility = View.VISIBLE
                responseBinding.textShipmentType.visibility = View.VISIBLE
            } else {
                responseBinding.shipmentType.visibility = View.GONE
                responseBinding.textShipmentType.visibility = View.GONE
            }

            val courier = ocrResponse.data?.output?.scanOutput?.courierInfo?.courierName
            if (!courier.isNullOrEmpty()) {
                val name = getCarrierNameFromKey(applicationContext, courier)
                responseBinding.courier.text = name
                responseBinding.textCourier.visibility = View.VISIBLE
                responseBinding.courier.visibility = View.VISIBLE
            } else {
                responseBinding.textCourier.visibility = View.GONE
                responseBinding.courier.visibility = View.GONE
            }
            val weight = ocrResponse.data?.output?.scanOutput?.courierInfo?.weightInfo
            if (!weight.isNullOrEmpty()) {
                responseBinding.weight.text = weight
                responseBinding.textWeight.visibility = View.VISIBLE
                responseBinding.weight.visibility = View.VISIBLE
            } else {
                responseBinding.textWeight.visibility = View.GONE
                responseBinding.weight.visibility = View.GONE
            }
            val receiverName = ocrResponse.data?.output?.scanOutput?.data?.recipientFound?.firstOrNull()?.name
            if (!receiverName.isNullOrEmpty()) {
                val rName = capitalize(receiverName)
                responseBinding.receiverName.text = rName
                responseBinding.textReceiverName.visibility = View.VISIBLE
                responseBinding.receiverName.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverName.visibility = View.GONE
                responseBinding.receiverName.visibility = View.GONE
            }
            val receiverStreet =
                ocrResponse.data?.output?.scanOutput?.address?.receiverAddress?.addressLine1
            if (!receiverStreet.isNullOrEmpty()) {
                responseBinding.receiverStreet.text = receiverStreet
                responseBinding.textReceiverStreet.visibility = View.VISIBLE
                responseBinding.receiverStreet.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverStreet.visibility = View.GONE
                responseBinding.receiverStreet.visibility = View.GONE
            }
            val receiverCity = ocrResponse.data?.output?.scanOutput?.address?.receiverAddress?.city
            if (!receiverCity.isNullOrEmpty()) {
                responseBinding.receiverCity.text = receiverCity
                responseBinding.textReceiverCity.visibility = View.VISIBLE
                responseBinding.receiverCity.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverCity.visibility = View.GONE
                responseBinding.receiverCity.visibility = View.GONE
            }
            val receiverState =
                ocrResponse.data?.output?.scanOutput?.address?.receiverAddress?.state
            if (!receiverState.isNullOrEmpty()) {
                responseBinding.receiverState.text = receiverState
                responseBinding.textReceiverState.visibility = View.VISIBLE
                responseBinding.receiverState.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverState.visibility = View.GONE
                responseBinding.receiverState.visibility = View.GONE
            }
            val receiverZip =
                ocrResponse.data?.output?.scanOutput?.address?.receiverAddress?.zipcode
            if (!receiverZip.isNullOrEmpty()) {
                responseBinding.receiverZipcode.text = receiverZip
                responseBinding.textReceiverZipCode.visibility = View.VISIBLE
                responseBinding.receiverZipcode.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverZipCode.visibility = View.GONE
                responseBinding.receiverZipcode.visibility = View.GONE
            }
            val receiverAddress =
                ocrResponse.data?.output?.scanOutput?.address?.receiverAddress?.completeAddress
            if (!receiverAddress.isNullOrEmpty()) {
                responseBinding.receiverAddress.text = receiverAddress
                responseBinding.textReceiverAddress.visibility = View.VISIBLE
                responseBinding.receiverAddress.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverAddress.visibility = View.GONE
                responseBinding.receiverAddress.visibility = View.GONE
            }

            val receiverUnitNo =
                ocrResponse.data?.output?.scanOutput?.address?.receiverAddress?.unitNo
            if (!receiverUnitNo.isNullOrEmpty()) {
                responseBinding.receiverUnitNo.text = receiverUnitNo
                responseBinding.receiverUnitNo.visibility = View.VISIBLE
                responseBinding.textReceiverUnitNo.visibility = View.VISIBLE
            } else {
                responseBinding.receiverUnitNo.visibility = View.GONE
                responseBinding.textReceiverUnitNo.visibility = View.GONE
            }

            val senderFound = ocrResponse.data?.output?.scanOutput?.data?.senderFound ?: emptyList()
            val senderName = senderFound.firstOrNull()?.combinedInfo
            if (!senderName.isNullOrEmpty()) {
                val sName = capitalize(senderName)
                responseBinding.senderName.text = sName
                responseBinding.senderName.visibility = View.VISIBLE
                responseBinding.textSenderName.visibility = View.VISIBLE
            } else {
                responseBinding.senderName.visibility = View.GONE
                responseBinding.textSenderName.visibility = View.GONE
            }
            val senderStreet =
                ocrResponse.data?.output?.scanOutput?.address?.senderAddress?.addressLine1
            if (!senderStreet.isNullOrEmpty()) {
                responseBinding.senderStreet.text = senderStreet
                responseBinding.textSenderStreetAddress.visibility = View.VISIBLE
                responseBinding.senderStreet.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderStreetAddress.visibility = View.GONE
                responseBinding.senderStreet.visibility = View.GONE
            }
            val senderCity = ocrResponse.data?.output?.scanOutput?.address?.senderAddress?.city
            if (!senderCity.isNullOrEmpty()) {
                responseBinding.senderCity.text = senderCity
                responseBinding.textSenderCity.visibility = View.VISIBLE
                responseBinding.senderCity.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderCity.visibility = View.GONE
                responseBinding.senderCity.visibility = View.GONE
            }
            val senderState = ocrResponse.data?.output?.scanOutput?.address?.senderAddress?.state
            if (!senderState.isNullOrEmpty()) {
                responseBinding.senderState.text = senderState
                responseBinding.textSenderStateAddress.visibility = View.VISIBLE
                responseBinding.senderState.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderStateAddress.visibility = View.GONE
                responseBinding.senderState.visibility = View.GONE
            }
            val senderZip = ocrResponse.data?.output?.scanOutput?.address?.senderAddress?.zipcode
            if (!senderZip.isNullOrEmpty()) {
                responseBinding.senderZipcode.text = senderZip
                responseBinding.textSenderZipcode.visibility = View.VISIBLE
                responseBinding.senderZipcode.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderZipcode.visibility = View.GONE
                responseBinding.senderZipcode.visibility = View.GONE
            }
            val senderAddress =
                ocrResponse.data?.output?.scanOutput?.address?.senderAddress?.completeAddress
            if (!senderAddress.isNullOrEmpty()) {
                responseBinding.senderAddress.text = senderAddress
                responseBinding.senderAddress.visibility = View.VISIBLE
                responseBinding.textSenderAddress.visibility = View.VISIBLE
            } else {
                responseBinding.senderAddress.visibility = View.GONE
                responseBinding.textSenderAddress.visibility = View.GONE
            }
            val poNo = ocrResponse.data?.output?.scanOutput?.itemInfo?.poNumber
            if (!poNo.isNullOrEmpty()) {
                responseBinding.poNo.text = poNo
                responseBinding.textPO.visibility = View.VISIBLE
                responseBinding.poNo.visibility = View.VISIBLE
            } else {
                responseBinding.textPO.visibility = View.GONE
                responseBinding.poNo.visibility = View.GONE
            }


            val senderUnitNo = ocrResponse.data?.output?.scanOutput?.address?.senderAddress?.unitNo
            if (!senderUnitNo.isNullOrEmpty()) {
                responseBinding.senderUnitNo.text = senderUnitNo
                responseBinding.senderUnitNo.visibility = View.VISIBLE
                responseBinding.textSenderUnitNo.visibility = View.VISIBLE
            } else {
                responseBinding.senderUnitNo.visibility = View.GONE
                responseBinding.textSenderUnitNo.visibility = View.GONE
            }

            //TODO
//            if (!ocrResponse.data?.output?.scanOutput?.courierInfo.parcelDimensions .length.isNullOrEmpty() || !ocrResponse.data?.dimensions?.height.isNullOrEmpty() || !ocrResponse.data?.dimensions?.width.isNullOrEmpty()) {
//                val stringBuilder = StringBuilder()
//                if (!ocrResponse.data?.dimensions?.length.isNullOrEmpty()) {
//                    stringBuilder.append(" L: ${ocrResponse.data?.dimensions?.length}")
//                }
//                if (!ocrResponse.data?.dimensions?.width.isNullOrEmpty()) {
//                    stringBuilder.append(" W: ${ocrResponse.data?.dimensions?.width}")
//                }
//                if (!ocrResponse.data?.dimensions?.height.isNullOrEmpty()) {
//                    stringBuilder.append(" W: ${ocrResponse.data?.dimensions?.height}")
//                }
//                responseBinding.dimensions.text = stringBuilder
//                responseBinding.dimensions.visibility = View.VISIBLE
//                responseBinding.textDimensions.visibility = View.VISIBLE
//            } else {
//                responseBinding.dimensions.visibility = View.GONE
//                responseBinding.textDimensions.visibility = View.GONE
//            }

            val presetLabels = ocrResponse.data?.output?.scanOutput?.courierInfo?.presetLabels
            val dynamicExtracted =
                ocrResponse.data?.output?.scanOutput?.courierInfo?.dynamicExtractedLabels
            val locationBased =
                ocrResponse.data?.output?.scanOutput?.courierInfo?.locationBasedLabels
            if (!presetLabels.isNullOrEmpty()) {
                presetLabels.forEach { label ->
                    meta.add("$label ")
                }
            }
            if (!dynamicExtracted.isNullOrEmpty()) {
//                meta.add("")
                dynamicExtracted.forEach { dynamicLabel ->
                    meta.add("$dynamicLabel ")
//                    metaData.plus(dynamicLabel.toString()+" ")
                }
            }
            if (!locationBased.isNullOrEmpty()) {
//                meta.add("")
                locationBased.forEach { locationLabel ->
                    meta.add("$locationLabel ")
                }
            }
            if (meta.isNotEmpty()) {
                responseBinding.textMetadata.visibility = View.VISIBLE
                responseBinding.metadata.visibility = View.VISIBLE
                meta.forEach { data ->
                    responseBinding.metadata.append(data)
                }
            } else {
                responseBinding.textMetadata.visibility = View.GONE
                responseBinding.metadata.visibility = View.GONE
            }

            val refNo = ocrResponse.data?.output?.scanOutput?.itemInfo?.refNumber
            if (!refNo.isNullOrEmpty()) {
                responseBinding.refNo.text = refNo
                responseBinding.refNo.visibility = View.VISIBLE
                responseBinding.textRefNo.visibility = View.VISIBLE
            } else {
                responseBinding.refNo.visibility = View.GONE
                responseBinding.textRefNo.visibility = View.GONE
            }

            if (!trackingNo.isNullOrEmpty() || !courier.isNullOrEmpty() || !weight.isNullOrEmpty()) {
                responseBinding.textPackageInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textPackageInfo.visibility = View.GONE
            }

            if (!receiverName.isNullOrEmpty() || !receiverStreet.isNullOrEmpty() || !receiverCity.isNullOrEmpty() || !receiverState.isNullOrEmpty() || !receiverZip.isNullOrEmpty() || !receiverAddress.isNullOrEmpty()) {
                responseBinding.textRecevierInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textRecevierInfo.visibility = View.GONE
            }
            if (!senderName.isNullOrEmpty() || !senderStreet.isNullOrEmpty() || !senderCity.isNullOrEmpty() || !senderState.isNullOrEmpty() || !senderZip.isNullOrEmpty() || !senderAddress.isNullOrEmpty()) {
                responseBinding.textSenderInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderInfo.visibility = View.GONE
            }
            if (!refNo.isNullOrEmpty() || !poNo.isNullOrEmpty()) {
                responseBinding.textLogistics.visibility = View.VISIBLE
            } else {
                responseBinding.textLogistics.visibility = View.GONE
            }
            responseBinding.btnBack.setOnClickListener {
                binding.camIcon.isEnabled = true
                binding.customScannerView.imageView.visibility = View.GONE
                OCRDialog.dismiss()
            }
        }
    }

    private fun showOCRResultDemo(ocrResponse: OCRResponseDemo) {
        val meta = mutableListOf<String>()
        responseBinding.metadata.text = ""
        responseBinding.scrollView.visibility = View.VISIBLE
        responseBinding.btnBack.visibility = View.VISIBLE
        responseBinding.progressBar.visibility = View.INVISIBLE
        responseBinding.textScanning.text = "Scanned"
        if (ocrResponse.data != null) {

            val trackingNo = ocrResponse.data?.trackingNumber
            if (!trackingNo.isNullOrEmpty()) {
                responseBinding.trackingNo.text = trackingNo
                responseBinding.textTrackingNo.visibility = View.VISIBLE
                responseBinding.trackingNo.visibility = View.VISIBLE
            } else {
                responseBinding.textTrackingNo.visibility = View.GONE
                responseBinding.trackingNo.visibility = View.GONE
            }

            val shipmentType: String? = ocrResponse.data?.serviceLevelName
            if (!shipmentType.isNullOrEmpty()) {
                responseBinding.shipmentType.text = shipmentType
                responseBinding.shipmentType.visibility = View.VISIBLE
                responseBinding.textShipmentType.visibility = View.VISIBLE
            } else {
                responseBinding.shipmentType.visibility = View.GONE
                responseBinding.textShipmentType.visibility = View.GONE
            }

            val courier = ocrResponse.data?.providerName
            if (!courier.isNullOrEmpty()) {
                val name = getCarrierNameFromKey(applicationContext, courier)
                responseBinding.courier.text = name
                responseBinding.textCourier.visibility = View.VISIBLE
                responseBinding.courier.visibility = View.VISIBLE
            } else {
                responseBinding.textCourier.visibility = View.GONE
                responseBinding.courier.visibility = View.GONE
            }
            val weight = ocrResponse.data?.weight//output?.scanOutput?.courierInfo?.weightInfo
            if (!weight.isNullOrEmpty()) {
                responseBinding.weight.text = weight + "lbs"
                responseBinding.textWeight.visibility = View.VISIBLE
                responseBinding.weight.visibility = View.VISIBLE
            } else {
                responseBinding.textWeight.visibility = View.GONE
                responseBinding.weight.visibility = View.GONE
            }
            val receiverName = ocrResponse.data?.recipient?.name
            if (!receiverName.isNullOrEmpty()) {
                val rName = capitalize(receiverName)
                responseBinding.receiverName.text = rName
                responseBinding.textReceiverName.visibility = View.VISIBLE
                responseBinding.receiverName.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverName.visibility = View.GONE
                responseBinding.receiverName.visibility = View.GONE
            }
            val receiverStreet =
                ocrResponse.data?.recipient?.address?.line1
            if (!receiverStreet.isNullOrEmpty()) {
                responseBinding.receiverStreet.text = receiverStreet
                responseBinding.textReceiverStreet.visibility = View.VISIBLE
                responseBinding.receiverStreet.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverStreet.visibility = View.GONE
                responseBinding.receiverStreet.visibility = View.GONE
            }
            val receiverCity = ocrResponse.data?.recipient?.address?.city
            if (!receiverCity.isNullOrEmpty()) {
                responseBinding.receiverCity.text = receiverCity
                responseBinding.textReceiverCity.visibility = View.VISIBLE
                responseBinding.receiverCity.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverCity.visibility = View.GONE
                responseBinding.receiverCity.visibility = View.GONE
            }
            val receiverState =
                ocrResponse.data?.recipient?.address?.stateCode
            if (!receiverState.isNullOrEmpty()) {
                responseBinding.receiverState.text = receiverState
                responseBinding.textReceiverState.visibility = View.VISIBLE
                responseBinding.receiverState.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverState.visibility = View.GONE
                responseBinding.receiverState.visibility = View.GONE
            }
            val receiverZip =
                ocrResponse.data?.recipient?.address?.postalCode
            if (!receiverZip.isNullOrEmpty()) {
                responseBinding.receiverZipcode.text = receiverZip
                responseBinding.textReceiverZipCode.visibility = View.VISIBLE
                responseBinding.receiverZipcode.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverZipCode.visibility = View.GONE
                responseBinding.receiverZipcode.visibility = View.GONE
            }
            val receiverAddress =
                ocrResponse.data?.recipient?.address?.formattedAddress
            if (!receiverAddress.isNullOrEmpty()) {
                responseBinding.receiverAddress.text = receiverAddress
                responseBinding.textReceiverAddress.visibility = View.VISIBLE
                responseBinding.receiverAddress.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverAddress.visibility = View.GONE
                responseBinding.receiverAddress.visibility = View.GONE
            }

            val receiverUnitNo =
                ocrResponse.data?.recipient?.address?.line2
            if (!receiverUnitNo.isNullOrEmpty()) {
                responseBinding.receiverUnitNo.text = receiverUnitNo
                responseBinding.receiverUnitNo.visibility = View.VISIBLE
                responseBinding.textReceiverUnitNo.visibility = View.VISIBLE
            } else {
                responseBinding.receiverUnitNo.visibility = View.GONE
                responseBinding.textReceiverUnitNo.visibility = View.GONE
            }


            val senderName = ocrResponse.data?.sender?.name
            if (!senderName.isNullOrEmpty()) {
                val sName = capitalize(senderName)
                responseBinding.senderName.text = sName
                responseBinding.senderName.visibility = View.VISIBLE
                responseBinding.textSenderName.visibility = View.VISIBLE
            } else {
                responseBinding.senderName.visibility = View.GONE
                responseBinding.textSenderName.visibility = View.GONE
            }
            val senderStreet =
                ocrResponse.data?.sender?.address?.line1
            if (!senderStreet.isNullOrEmpty()) {
                responseBinding.senderStreet.text = senderStreet
                responseBinding.textSenderStreetAddress.visibility = View.VISIBLE
                responseBinding.senderStreet.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderStreetAddress.visibility = View.GONE
                responseBinding.senderStreet.visibility = View.GONE
            }
            val senderCity = ocrResponse.data?.sender?.address?.city
            if (!senderCity.isNullOrEmpty()) {
                responseBinding.senderCity.text = senderCity
                responseBinding.textSenderCity.visibility = View.VISIBLE
                responseBinding.senderCity.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderCity.visibility = View.GONE
                responseBinding.senderCity.visibility = View.GONE
            }
            val senderState = ocrResponse.data?.sender?.address?.stateCode
            if (!senderState.isNullOrEmpty()) {
                responseBinding.senderState.text = senderState
                responseBinding.textSenderStateAddress.visibility = View.VISIBLE
                responseBinding.senderState.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderStateAddress.visibility = View.GONE
                responseBinding.senderState.visibility = View.GONE
            }
            val senderZip = ocrResponse.data?.sender?.address?.postalCode
            if (!senderZip.isNullOrEmpty()) {
                responseBinding.senderZipcode.text = senderZip
                responseBinding.textSenderZipcode.visibility = View.VISIBLE
                responseBinding.senderZipcode.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderZipcode.visibility = View.GONE
                responseBinding.senderZipcode.visibility = View.GONE
            }
            val senderAddress = ocrResponse.data?.sender?.address?.formattedAddress
            if (!senderAddress.isNullOrEmpty()) {
                responseBinding.senderAddress.text = senderAddress
                responseBinding.senderAddress.visibility = View.VISIBLE
                responseBinding.textSenderAddress.visibility = View.VISIBLE
            } else {
                responseBinding.senderAddress.visibility = View.GONE
                responseBinding.textSenderAddress.visibility = View.GONE
            }

            val senderUnitNo = ocrResponse.data?.sender?.address?.line2
            if (!senderUnitNo.isNullOrEmpty()) {
                responseBinding.senderUnitNo.text = senderUnitNo
                responseBinding.senderUnitNo.visibility = View.VISIBLE
                responseBinding.textSenderUnitNo.visibility = View.VISIBLE
            } else {
                responseBinding.senderUnitNo.visibility = View.GONE
                responseBinding.textSenderUnitNo.visibility = View.GONE
            }
            val poNo = ocrResponse.data?.purchaseOrder
            if (!poNo.isNullOrEmpty()) {
                responseBinding.poNo.text = poNo
                responseBinding.textPO.visibility = View.VISIBLE
                responseBinding.poNo.visibility = View.VISIBLE
            } else {
                responseBinding.textPO.visibility = View.GONE
                responseBinding.poNo.visibility = View.GONE
            }
            val presetLabels = ocrResponse.data?.extractedLabels

            if (!presetLabels.isNullOrEmpty()) {
                presetLabels.forEach { label ->
                    meta.add("$label ")
                }
            }
            if (meta.isNotEmpty()) {
                responseBinding.textMetadata.visibility = View.VISIBLE
                responseBinding.metadata.visibility = View.VISIBLE
                meta.forEach { data ->
                    responseBinding.metadata.append(data)
                }
            } else {
                responseBinding.textMetadata.visibility = View.GONE
                responseBinding.metadata.visibility = View.GONE
            }

            val refNo = ocrResponse.data?.referenceNumber
            if (!refNo.isNullOrEmpty()) {
                responseBinding.refNo.text = refNo
                responseBinding.refNo.visibility = View.VISIBLE
                responseBinding.textRefNo.visibility = View.VISIBLE
            } else {
                responseBinding.refNo.visibility = View.GONE
                responseBinding.textRefNo.visibility = View.GONE
            }

            //TODO
            if (!ocrResponse.data?.dimensions?.length.isNullOrEmpty() || !ocrResponse.data?.dimensions?.height.isNullOrEmpty() || !ocrResponse.data?.dimensions?.width.isNullOrEmpty()) {
                val stringBuilder = StringBuilder()
                if (!ocrResponse.data?.dimensions?.length.isNullOrEmpty()) {
                    stringBuilder.append(" L: ${ocrResponse.data?.dimensions?.length}")
                }
                if (!ocrResponse.data?.dimensions?.width.isNullOrEmpty()) {
                    stringBuilder.append(" W: ${ocrResponse.data?.dimensions?.width}")
                }
                if (!ocrResponse.data?.dimensions?.height.isNullOrEmpty()) {
                    stringBuilder.append(" W: ${ocrResponse.data?.dimensions?.height}")
                }
                responseBinding.dimensions.text = stringBuilder
                responseBinding.dimensions.visibility = View.VISIBLE
                responseBinding.textDimensions.visibility = View.VISIBLE
            } else {
                responseBinding.dimensions.visibility = View.GONE
                responseBinding.textDimensions.visibility = View.GONE
            }

            if (!trackingNo.isNullOrEmpty() || !courier.isNullOrEmpty() || !weight.isNullOrEmpty()) {
                responseBinding.textPackageInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textPackageInfo.visibility = View.GONE
            }

            if (!receiverName.isNullOrEmpty() || !receiverStreet.isNullOrEmpty() || !receiverCity.isNullOrEmpty() || !receiverState.isNullOrEmpty() || !receiverZip.isNullOrEmpty() || !receiverAddress.isNullOrEmpty()) {
                responseBinding.textRecevierInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textRecevierInfo.visibility = View.GONE
            }
            if (!senderName.isNullOrEmpty() || !senderStreet.isNullOrEmpty() || !senderCity.isNullOrEmpty() || !senderState.isNullOrEmpty() || !senderZip.isNullOrEmpty() || !senderAddress.isNullOrEmpty()) {
                responseBinding.textSenderInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderInfo.visibility = View.GONE
            }

            //TODO
            if (!refNo.isNullOrEmpty() || !poNo.isNullOrEmpty() || !ocrResponse.data?.dimensions?.length.isNullOrEmpty() || !ocrResponse.data?.dimensions?.height.isNullOrEmpty() || !ocrResponse.data?.dimensions?.width.isNullOrEmpty()) {
                responseBinding.textLogistics.visibility = View.VISIBLE
            } else {
                responseBinding.textLogistics.visibility = View.GONE
            }
            responseBinding.btnBack.setOnClickListener {
                binding.camIcon.isEnabled = true
                binding.customScannerView.imageView.visibility = View.GONE
                OCRDialog.dismiss()
            }
        }
    }

    private fun capitalize(
        string: String, delimiter: String = " ", separator: String = " "
    ): String {
        return string.split(delimiter).joinToString(separator = separator) {
            it.lowercase().replaceFirstChar { char -> char.titlecase() }
        }
    }

    override fun onViewDetected(barCodeResult: MutableList<Barcode>) {

    }

    override fun onMultiBarcodesDetected(barcodes: List<Barcode>) {

    }

    override fun onOCRResponse(ocrResponse: OCRResponseParent?) {
        binding.progressBar.hide()
        when (ocrResponse) {
            is OcrResponse -> {
                showOCRResult(ocrResponse)
            }

            is OCRResponseDemo -> {
                showOCRResultDemo(ocrResponse)
            }

            else -> {
                Toast.makeText(this, "Something went wrong !", Toast.LENGTH_SHORT).show()
                binding.progressBar.hide()
                OCRDialog.dismiss()
                binding.camIcon.isEnabled = true
                Toast.makeText(this, "Something went wrong !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOCRResponseFailed(throwable: Throwable?) {
        binding.progressBar.hide()
        OCRDialog.dismiss()
        binding.camIcon.isEnabled = true
        Toast.makeText(this, "Failed: ${throwable.toString()}", Toast.LENGTH_SHORT).show()
    }

    override fun onSomeTextDetected(text: Text) {

    }
}