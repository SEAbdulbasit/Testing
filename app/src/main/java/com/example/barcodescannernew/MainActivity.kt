package com.example.barcodescannernew

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
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
import com.example.customscannerview.mlkit.modelclasses.ocr_response.OCRResponseParent
import com.example.customscannerview.mlkit.modelclasses.ocr_response.OcrResponse
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
        binding.customScannerView.barcodeResultSingle.observe(this, Observer { barCodeResult ->


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

        })
        binding.customScannerView.textResult.observe(this, Observer { text ->
            if (text.textBlocks.isEmpty()) {
                binding.textDetector.setImageResource(R.drawable.ic_text_inactive)
            } else {
                binding.textDetector.setImageResource(R.drawable.ic_text_active)
            }
        })
        binding.customScannerView.onSomethingDetected.observe(this, Observer { barcodeList ->
            val TWO_DIMENSIONAL_FORMATS = mutableListOf<Int>(
                Barcode.FORMAT_QR_CODE
            )
            val ONE_DIMENSIONAL_FORMATS = mutableListOf<Int>(
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
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

        })
        binding.customScannerView.multipleBarcodes.observe(this, Observer { barcodes ->
            barcodesListDetected.clear()
            if (barcodes.isNotEmpty()) {
                if (settingsFragment.findViewById<SwitchCompat>(R.id.btnSwitchSetting).isChecked) {
                    barcodes.forEach {
                        val TWO_DIMENSIONAL_FORMATS = mutableListOf<Int>(
                            Barcode.FORMAT_QR_CODE
                        )
                        val ONE_DIMENSIONAL_FORMATS = mutableListOf<Int>(
                            Barcode.FORMAT_CODABAR,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_CODE_93,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_ITF,
                            Barcode.FORMAT_DATA_MATRIX,
                            Barcode.FORMAT_AZTEC,
                            Barcode.FORMAT_PDF417,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E
                        )
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
        })

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
        failureDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
            "OK",
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val code = v.findViewById<EditText>(R.id.inputValue).text.toString()
                    doNotShowDialog = false
                    mediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep_sound)
                    applicationContext.copyToClipboard(code)
                    dialog?.dismiss()
                }

            })
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
            if (isFlashEnabled) {
                binding.flashIcon.setImageResource(R.drawable.ic_flash_inactive)
                binding.customScannerView.disableTorch()
                isFlashEnabled = false
            } else {
                binding.flashIcon.setImageResource(R.drawable.ic_flash_active)
                binding.customScannerView.enableTorch()
                isFlashEnabled = true
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
        resultDialog.setButton(AlertDialog.BUTTON_POSITIVE,
            "Copy",
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    applicationContext.copyToClipboard(message)
                    doNotShowDialog = false
                    mediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep_sound)
                    dialog?.dismiss()
                }
            })
        resultDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
            "Cancel",
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    doNotShowDialog = false
                    mediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep_sound)
                    dialog?.dismiss()
                }
            })
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
                    if (binding.bottomNav.selectedItemId == R.id.ocr) {

                    } else {
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
            val trackingNo = ocrResponse.data.output.scan_output.courier_info.tracking_no
            if (trackingNo != null && trackingNo.isNotEmpty()) {
                responseBinding.trackingNo.text = trackingNo
                responseBinding.textTrackingNo.visibility = View.VISIBLE
                responseBinding.trackingNo.visibility = View.VISIBLE
            } else {
                responseBinding.textTrackingNo.visibility = View.GONE
                responseBinding.trackingNo.visibility = View.GONE
            }
            var courier = ocrResponse.data.output.scan_output.courier_info.courier_name
            if (courier != null && courier.isNotEmpty()) {
                val name = getCarrierNameFromKey(applicationContext, courier)
                responseBinding.courier.text = name
                responseBinding.textCourier.visibility = View.VISIBLE
                responseBinding.courier.visibility = View.VISIBLE
            } else {
                responseBinding.textCourier.visibility = View.GONE
                responseBinding.courier.visibility = View.GONE
            }
            val weight = ocrResponse.data.output.scan_output.courier_info.weight_info
            if (weight != null && weight.isNotEmpty()) {
                responseBinding.weight.text = weight
                responseBinding.textWeight.visibility = View.VISIBLE
                responseBinding.weight.visibility = View.VISIBLE
            } else {
                responseBinding.textWeight.visibility = View.GONE
                responseBinding.weight.visibility = View.GONE
            }
            val receiverName = ocrResponse.data.output.scan_output.address.receiver_address.name
            if (receiverName != null && receiverName.isNotEmpty()) {
                val rName = capitalize(receiverName)
                responseBinding.receiverName.text = rName
                responseBinding.textReceiverName.visibility = View.VISIBLE
                responseBinding.receiverName.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverName.visibility = View.GONE
                responseBinding.receiverName.visibility = View.GONE
            }
            val receiverStreet =
                ocrResponse.data.output.scan_output.address.receiver_address.address_line_1
            if (receiverStreet != null && receiverStreet.isNotEmpty()) {
                responseBinding.receiverStreet.text = receiverStreet
                responseBinding.textReceiverStreet.visibility = View.VISIBLE
                responseBinding.receiverStreet.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverStreet.visibility = View.GONE
                responseBinding.receiverStreet.visibility = View.GONE
            }
            val receiverCity = ocrResponse.data.output.scan_output.address.receiver_address.city
            if (receiverCity != null && receiverCity.isNotEmpty()) {
                responseBinding.receiverCity.text = receiverCity
                responseBinding.textReceiverCity.visibility = View.VISIBLE
                responseBinding.receiverCity.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverCity.visibility = View.GONE
                responseBinding.receiverCity.visibility = View.GONE
            }
            val receiverState = ocrResponse.data.output.scan_output.address.receiver_address.state
            if (receiverState != null && receiverState.isNotEmpty()) {
                responseBinding.receiverState.text = receiverState
                responseBinding.textReceiverState.visibility = View.VISIBLE
                responseBinding.receiverState.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverState.visibility = View.GONE
                responseBinding.receiverState.visibility = View.GONE
            }
            val receiverZip = ocrResponse.data.output.scan_output.address.receiver_address.zipcode
            if (receiverZip != null && receiverZip.isNotEmpty()) {
                responseBinding.receiverZipcode.text = receiverZip
                responseBinding.textReceiverZipCode.visibility = View.VISIBLE
                responseBinding.receiverZipcode.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverZipCode.visibility = View.GONE
                responseBinding.receiverZipcode.visibility = View.GONE
            }
            val receiverAddress =
                ocrResponse.data.output.scan_output.address.receiver_address.complete_address
            if (receiverAddress != null && receiverAddress.isNotEmpty()) {
                responseBinding.receiverAddress.text = receiverAddress
                responseBinding.textReceiverAddress.visibility = View.VISIBLE
                responseBinding.receiverAddress.visibility = View.VISIBLE
            } else {
                responseBinding.textReceiverAddress.visibility = View.GONE
                responseBinding.receiverAddress.visibility = View.GONE
            }

            val senderFound = ocrResponse.data.output.scan_output.data.sender_found
            val senderName = senderFound[0].combined_info
            if (senderName != null && senderName.isNotEmpty()) {
                val sName = capitalize(senderName)
                responseBinding.senderName.text = sName
                responseBinding.senderName.visibility = View.VISIBLE
                responseBinding.textSenderName.visibility = View.VISIBLE
            } else {
                responseBinding.senderName.visibility = View.GONE
                responseBinding.textSenderName.visibility = View.GONE
            }
            val senderStreet =
                ocrResponse.data.output.scan_output.address.sender_address.address_line_1
            if (senderStreet != null && senderStreet.isNotEmpty()) {
                responseBinding.senderStreet.text = senderStreet
                responseBinding.textSenderStreetAddress.visibility = View.VISIBLE
                responseBinding.senderStreet.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderStreetAddress.visibility = View.GONE
                responseBinding.senderStreet.visibility = View.GONE
            }
            val senderCity = ocrResponse.data.output.scan_output.address.sender_address.city
            if (senderCity != null && senderCity.isNotEmpty()) {
                responseBinding.senderCity.text = senderCity
                responseBinding.textSenderCity.visibility = View.VISIBLE
                responseBinding.senderCity.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderCity.visibility = View.GONE
                responseBinding.senderCity.visibility = View.GONE
            }
            val senderState = ocrResponse.data.output.scan_output.address.sender_address.state
            if (senderState != null && senderState.isNotEmpty()) {
                responseBinding.senderState.text = senderState
                responseBinding.textSenderStateAddress.visibility = View.VISIBLE
                responseBinding.senderState.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderStateAddress.visibility = View.GONE
                responseBinding.senderState.visibility = View.GONE
            }
            val senderZip = ocrResponse.data.output.scan_output.address.sender_address.zipcode
            if (senderZip != null && senderZip.isNotEmpty()) {
                responseBinding.senderZipcode.text = senderZip
                responseBinding.textSenderZipcode.visibility = View.VISIBLE
                responseBinding.senderZipcode.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderZipcode.visibility = View.GONE
                responseBinding.senderZipcode.visibility = View.GONE
            }
            val senderAddress =
                ocrResponse.data.output.scan_output.address.sender_address.complete_address
            if (senderAddress != null && senderAddress.isNotEmpty()) {
                responseBinding.senderAddress.text = senderAddress
                responseBinding.senderAddress.visibility = View.VISIBLE
                responseBinding.textSenderAddress.visibility = View.VISIBLE
            } else {
                responseBinding.senderAddress.visibility = View.GONE
                responseBinding.textSenderAddress.visibility = View.GONE
            }
            val poNo = ocrResponse.data.output.scan_output.item_info.po_number
            if (poNo != null && poNo.isNotEmpty()) {
                responseBinding.poNo.text = poNo
                responseBinding.textPO.visibility = View.VISIBLE
                responseBinding.poNo.visibility = View.VISIBLE
            } else {
                responseBinding.textPO.visibility = View.GONE
                responseBinding.poNo.visibility = View.GONE
            }
            val presetLabels = ocrResponse?.data?.output?.scan_output?.courier_info?.preset_labels
            val dynamicExtracted =
                ocrResponse?.data?.output?.scan_output?.courier_info?.dynamic_extracted_labels
            val locationBased =
                ocrResponse?.data?.output?.scan_output?.courier_info?.location_based_labels
            if (presetLabels != null && presetLabels.isNotEmpty()) {
                presetLabels.forEach { label ->
                    meta.add("$label ")
                }
            }
            if (dynamicExtracted != null && dynamicExtracted.isNotEmpty()) {
//                meta.add("")
                dynamicExtracted.forEach { dynamicLabel ->
                    meta.add("$dynamicLabel ")
//                    metaData.plus(dynamicLabel.toString()+" ")
                }
            }
            if (locationBased != null && locationBased.isNotEmpty()) {
//                meta.add("")
                locationBased.forEach { locationLabel ->
                    meta.add("$locationLabel ")
                }
            }
            if (meta != null && meta.isNotEmpty()) {
                responseBinding.textMetadata.visibility = View.VISIBLE
                responseBinding.metadata.visibility = View.VISIBLE
                meta.forEach { data ->
                    responseBinding.metadata.append(data)
                }
            } else {
                responseBinding.textMetadata.visibility = View.GONE
                responseBinding.metadata.visibility = View.GONE
            }

            val refNo = ocrResponse.data.output.scan_output.item_info.ref_number
            if (refNo != null && refNo.isNotEmpty()) {
                responseBinding.refNo.text = refNo
                responseBinding.refNo.visibility = View.VISIBLE
                responseBinding.textRefNo.visibility = View.VISIBLE
            } else {
                responseBinding.refNo.visibility = View.GONE
                responseBinding.textRefNo.visibility = View.GONE
            }

            if (trackingNo != null && trackingNo.isNotEmpty() || courier != null && courier.isNotEmpty() || weight != null && weight.isNotEmpty()) {
                responseBinding.textPackageInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textPackageInfo.visibility = View.GONE
            }

            if (receiverName != null && receiverName.isNotEmpty() || receiverStreet != null && receiverStreet.isNotEmpty() || receiverCity != null && receiverCity.isNotEmpty() || receiverState != null && receiverState.isNotEmpty() || receiverZip != null && receiverZip.isNotEmpty() || receiverAddress != null && receiverAddress.isNotEmpty()) {
                responseBinding.textRecevierInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textRecevierInfo.visibility = View.GONE
            }
            if (senderName != null && senderName.isNotEmpty() || senderStreet != null && senderStreet.isNotEmpty() || senderCity != null && senderCity.isNotEmpty() || senderState != null && senderState.isNotEmpty() || senderZip != null && senderZip.isNotEmpty() || senderAddress != null && senderAddress.isNotEmpty()) {
                responseBinding.textSenderInfo.visibility = View.VISIBLE
            } else {
                responseBinding.textSenderInfo.visibility = View.GONE
            }
            if (refNo != null && refNo.isNotEmpty() || poNo != null && poNo.isNotEmpty()) {
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

            null -> {
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