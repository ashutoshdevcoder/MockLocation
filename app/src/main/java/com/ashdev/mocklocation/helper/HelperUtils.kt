package com.ashdev.mocklocation.helper

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng
import java.io.IOException


fun logit(msg: Any? = "...") {
        val trace: StackTraceElement? = Thread.currentThread().stackTrace[3]
        val lineNumber = trace?.lineNumber
        val methodName = trace?.methodName
        val className = trace?.fileName?.replaceAfter(".", "")?.replace(".", "")
        Log.d("Line $lineNumber", "$className::$methodName() -> $msg")
    }

infix fun Context.showToast(msg: Any?) {
    val m = msg.toString()
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast.makeText(this, m, if (m.length < 15) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
    } else {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, m, if (m.length < 15) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
                .show()
        }
    }
}


fun Number.toDp() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
)

fun Number.toPx() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_PX,
    this.toFloat(),
    Resources.getSystem().displayMetrics
)
fun Dialog.init(cancelable:Boolean): Dialog {
    return this.apply {
        val layoutParams = window?.attributes
        layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams?.dimAmount = .7f
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.attributes = layoutParams
        setCancelable(cancelable)
        setCanceledOnTouchOutside(cancelable)
    }
}
fun Context.getLocationFromAddress(strAddress: String): LatLng? {
    val coder = Geocoder(this)
    val address: List<Address>?
    var p1: LatLng? = null
    try {
        address = coder.getFromLocationName(strAddress, 5)
        if (address.isNullOrEmpty()) {
            return null
        }
        val location = address[0]
        location.latitude
        location.longitude
        p1 = LatLng(
            (location.latitude),
            (location.longitude)
        )
        return p1
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

fun isMockLocationEnabled(context: Context): Boolean {
    return try {
        val settingsSecure = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ALLOW_MOCK_LOCATION
        ) == "1"

        val appOpsMock = isAppMockLocationEnabled(context)

        settingsSecure || appOpsMock
    } catch (e: Exception) {
        false
    }
}

@SuppressLint("NewApi")
private fun isAppMockLocationEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        try {
            val mode = appOps.unsafeCheckOp(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
}


fun Context.hasPermissions(permissions: Array<String>): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
    }
    return true
}

fun androidx.activity.ComponentActivity.startPermissionForResultLaunch(receiver:(Map<String, @JvmSuppressWildcards Boolean>) -> Unit) = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(), receiver)