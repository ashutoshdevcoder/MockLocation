package com.ashdev.mocklocation.helper

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock

class MockLocationProvider(var providerName: String, var ctx: Context) {
    /**
     * Class constructor
     *
     * @param name provider
     * @param ctx  context
     * @return Void
     */
    init {
        var powerUsage = 0
        var accuracy = 5
        if (Build.VERSION.SDK_INT >= 30) {
            powerUsage = 1
            accuracy = 2
        }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startup(lm, powerUsage, accuracy,  /* maxRetryCount= */3,  /* currentRetryCount= */0)
    }

    private fun startup(
        lm: LocationManager,
        powerUsage: Int,
        accuracy: Int,
        maxRetryCount: Int,
        currentRetryCount: Int
    ) {
        if (currentRetryCount < maxRetryCount) {
            try {
                shutdown()
                lm.addTestProvider(
                    providerName,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    powerUsage,
                    accuracy
                )
                lm.setTestProviderEnabled(providerName, true)
            } catch (e: Exception) {
                startup(lm, powerUsage, accuracy, maxRetryCount, currentRetryCount + 1)
            }
        } else {
            throw SecurityException("Not allowed to perform MOCK_LOCATION")
        }
    }

    /**
     * Pushes the location in the system (mock). This is where the magic gets done.
     *
     * @param lat latitude
     * @param lon longitude
     * @return Void
     */
    fun pushLocation(lat: Double, lon: Double) {
         Location(providerName).apply {
            latitude = lat
            longitude = lon
            altitude = 3.0
            time = System.currentTimeMillis()
            speed = 0.01f
            bearing = 1f
            accuracy = 1f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bearingAccuracyDegrees = 0.1f
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters = 0.1f
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                speedAccuracyMetersPerSecond = 0.01f
            }
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }.also {
             (ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager).setTestProviderLocation(providerName, it)
        }


    }

    /**
     * Removes the provider
     *
     * @return Void
     */
    fun shutdown() {
        try {
            (ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager).apply {
                removeTestProvider(providerName)
            }
        } catch (e: Exception) {
        }
    }
}