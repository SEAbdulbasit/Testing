package com.example.barcodescannernew.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import com.example.barcodescannernew.R

fun Context.copyToClipboard(text: CharSequence) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("label", text)
    clipboard.setPrimaryClip(clip)
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun getCarrierNameFromKey(
    context: Context,
    key: String
): String? {
    val carrierNames = context.resources.getStringArray(R.array.carrier_list_value)
    val carrierKeys =
        context.resources.getStringArray(R.array.carrier_list_key)
    val carrierList = ArrayList<String>()
    carrierList.addAll(listOf(*carrierNames))
    val carrierKeyList = ArrayList<String>()
    carrierKeyList.addAll(listOf(*carrierKeys))
    return if (carrierKeyList.contains(key)) {
        carrierList[carrierKeyList.indexOf(key)]
    } else key
}
