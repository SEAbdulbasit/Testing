package com.example.barcodescannernew

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SettingsFragment:DialogFragment() {
    var shown = false
    lateinit var btnBack:FloatingActionButton
    override fun onStart() {
        super.onStart()
        val width = (requireActivity().resources.displayMetrics.widthPixels * 0.90).toInt()
        val height=(requireActivity().resources.displayMetrics.heightPixels*0.90).toInt()
//        dialog?.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setLayout(width, height)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog?.setCancelable(false)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view=inflater.inflate(R.layout.setting_sheet_view,container,false)
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(true)

        return view
    }
    companion object {
        fun newInstance(title: String?): SettingsFragment {
            val frag = SettingsFragment()
            return frag
        }
    }
    override fun show(manager: FragmentManager, tag: String?) {
        if (shown) return
        super.show(manager, tag)
        shown = true
    }

    override fun onDismiss(dialog: DialogInterface) {
        shown = false
        super.onDismiss(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnBack.setOnClickListener {
            dialog?.dismiss()
        }

    }

}