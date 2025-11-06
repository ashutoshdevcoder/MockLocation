package com.ashdev.mocklocation.helper

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.annotation.Keep
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnSuccessListener

@Keep
class LocationLifecycleObserver(mContext: Context,DEFAULT_INTERVAL_IN_MILLISECONDS:Long) : DefaultLifecycleObserver {
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private var mContext: Context
    val locationLiveData = MutableLiveData<Location>()
    private var DEFAULT_INTERVAL_IN_MILLISECONDS:Long

    init {
        this.DEFAULT_INTERVAL_IN_MILLISECONDS=DEFAULT_INTERVAL_IN_MILLISECONDS
        this.mContext=mContext;
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
        mLocationRequest =LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,DEFAULT_INTERVAL_IN_MILLISECONDS).
            setMinUpdateIntervalMillis(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .build();
    }


    @SuppressLint("MissingPermission")
    override fun onResume(owner: LifecycleOwner) {
        if (successListener != null)
            mFusedLocationClient.lastLocation
                .addOnSuccessListener(
                    successListener
                )
        Looper.myLooper()?.let {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mCallback,
                it
            )
        }

    }


    override fun onStop(owner: LifecycleOwner) {
        mFusedLocationClient.removeLocationUpdates(mCallback)
    }


   private var successListener: OnSuccessListener<Location> =
        OnSuccessListener { location ->
            locationLiveData.postValue(location)
        }

   private var mCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult.locations.size > 0) {
                locationLiveData.postValue(locationResult.locations[0])

            }
            super.onLocationResult(locationResult)
        }
    }
}