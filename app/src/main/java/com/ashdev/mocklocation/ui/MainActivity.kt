package com.ashdev.mocklocation.ui

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ashdev.mocklocation.MockLocationService
import com.ashdev.mocklocation.R
import com.ashdev.mocklocation.databinding.ActivityMainBinding
import com.ashdev.mocklocation.helper.AppConstant
import com.ashdev.mocklocation.helper.LocationLifecycleObserver
import com.ashdev.mocklocation.helper.hasPermissions
import com.ashdev.mocklocation.helper.isMockLocationEnabled
import com.ashdev.mocklocation.helper.logit
import com.ashdev.mocklocation.helper.showToast
import com.ashdev.mocklocation.helper.startPermissionForResultLaunch

import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var isExplainationNeeded=false
    private val mapZoom=14.0f
    private var googleMap:GoogleMap?=null
    private lateinit var toggle: ActionBarDrawerToggle
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val locationLifecycleObserver: LocationLifecycleObserver by lazy {
        LocationLifecycleObserver(this,20000)
    }
    private var isCurrentLocationSet=false
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var address:String?=null
    private var mockLocationService:Intent?=null
    private val locationPermission = arrayOf(
        "android.permission.FOREGROUND_SERVICE_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION"
    )
    private var multiplePermissionActivityResultLauncher: ActivityResultLauncher<Array<String>>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.open,
            R.string.close
        )
        toggle.isDrawerIndicatorEnabled = false
        toggle.syncState()
        multiplePermissionActivityResultLauncher = startPermissionForResultLaunch { isGranted: Map<String, Boolean?> ->
                if (!isExplainationNeeded) {
                    val deniedList: MutableList<String> =
                        ArrayList()
                    for ((key, value) in isGranted) {
                        if (!value!!) {
                            deniedList.add(key)
                        }
                    }
                    for (permission in deniedList) {
                        if (!shouldShowRequestPermissionRationale(permission)) isExplainationNeeded =
                            true
                    }
                    if (deniedList.isNotEmpty()) {
                        if (isExplainationNeeded && isGranted.containsKey(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showSettingsDialog(getString(R.string.location_permission_message))
                            isExplainationNeeded = false

                        } else {
                            logit("keys ${isGranted.keys}")
                            checkLocationPermissionStatus()
                        }
                    } else {
                        initObserver()
                    }

                }
            }

        checkLocationPermissionStatus()

        binding.appBarLayout.actionSearch.setOnClickListener {
            if(isMockLocationEnabled(this))
                 showInputDialog()
            else
                showToast(getString(R.string.MainActivity_MockNotApplied))

        }
        binding.btnSelect.setOnClickListener {
            if(address.isNullOrEmpty().not() && latitude>0 && longitude >0) {
                mockLocationService = Intent(this, MockLocationService::class.java).apply {
                    putExtra(AppConstant.LATITUDE, latitude)
                    putExtra(AppConstant.LONGITUDE, longitude)
                    putExtra(AppConstant.ADDRESS, address)
                }
                startService(mockLocationService)
                address=null
                binding.btnRemove.isEnabled = true
                binding.btnRemove.visibility = View.VISIBLE
            }
            else
                showToast(getString(R.string.please_enter_mock_address))
        }
        binding.btnRemove.setOnClickListener {
            mockLocationService?.let {
                stopService(mockLocationService)
                mockLocationService=null
                binding.btnRemove.isEnabled = false
                binding.btnRemove.visibility = View.GONE
            }
        }
    }

    private fun initObserver() {
        lifecycle.addObserver(locationLifecycleObserver)
        locationLifecycleObserver.locationLiveData.observe(this) {
            if (it != null && isCurrentLocationSet.not()) {
                isCurrentLocationSet = true
                latitude = it.latitude
                longitude = it.longitude
                val latLng = LatLng(latitude, longitude)
                moveMapCamera(latLng)
            }

        }
    }

    private fun showSettingsDialog(message: String?) {
         AlertDialog.Builder(this).apply {
             setTitle(getString(R.string.dialog_permission_title))
             setMessage(message)
             setPositiveButton(getString(R.string.go_to_settings)) { dialog: DialogInterface, which: Int ->
                 dialog.cancel()
                 openSettings()
             }
             setNegativeButton(
                 getString(android.R.string.cancel)
             ) { dialog: DialogInterface, which: Int ->
                 dialog.cancel()
                 finish()
             }
        }.also {
             it.show()
         }
    }
    private fun openSettings() {
        val intentAppSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", this.packageName, null)
        intentAppSettings.data = uri
        permissionResultSystemSettings.launch(intentAppSettings)
    }
    private val permissionResultSystemSettings: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                    checkLocationPermissionStatus()
            })

    private fun checkLocationPermissionStatus() {
        if (!hasPermissions(locationPermission)
        ) {
            multiplePermissionActivityResultLauncher?.launch(locationPermission)
        } else {
            initObserver()
        }
    }

    private fun showInputDialog() {
        InputDialogFragment.newInstance().also {
            lifecycleScope.launchWhenCreated {
                if(supportFragmentManager.findFragmentByTag("InputDialog")==null)
                    it.show(supportFragmentManager,"InputDialog")
            }
        }
    }

    fun applyLatLng(latLng: LatLng, addr: String)
    {
        latitude=latLng.latitude
        longitude=latLng.longitude
        address=addr
        moveMapCamera(latLng)
        binding.btnSelect.isEnabled = true
    }

    private fun moveMapCamera(latLng: LatLng) {
        try {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mapZoom))
        } catch (e: Exception) {
            googleMap?.setOnMapLoadedCallback {
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mapZoom))
            }
        }
        val bitmapDrawable =
            ContextCompat.getDrawable(this, R.drawable.ic_pin) as BitmapDrawable
        googleMap?.addMarker(MarkerOptions().position(LatLng(latitude,longitude)).icon( BitmapDescriptorFactory.fromBitmap(
            bitmapDrawable.bitmap)))
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }


    override fun onMapReady(map: GoogleMap) {
        this.googleMap=map
        googleMap?.let {
            it.uiSettings.isCompassEnabled = false
            it.uiSettings.isMyLocationButtonEnabled = true
            it.uiSettings.isMapToolbarEnabled = false
            it.isMyLocationEnabled = !(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
            val latLng=LatLng(latitude,longitude)
            moveMapCamera(latLng)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()

    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }


    override fun onDestroy() {
        binding.mapView.onDestroy()
        lifecycle.removeObserver(locationLifecycleObserver)
        mockLocationService?.let {
            stopService(it)
        }
        super.onDestroy()
        // Prevent leaks
    }



}