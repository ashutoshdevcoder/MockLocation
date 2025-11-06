package com.ashdev.mocklocation

import android.content.Context
import android.os.StrictMode
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.ashdev.mocklocation.helper.logit
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MockLocationApplication :MultiDexApplication(), OnMapsSdkInitializedCallback {

    override fun onCreate() {
        super.onCreate()
        setupStrictVm()
        MapsInitializer.initialize(applicationContext,MapsInitializer.Renderer.LATEST,this)
        appContext = applicationContext
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object{
        lateinit var appContext: Context
        fun setupStrictVm() {
            // provides fragments each transaction status
            // use adb shell setprop log.tag.FragmentManager VERBOSE
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().run {

                    detectActivityLeaks()
                    detectLeakedClosableObjects()
                    detectLeakedSqlLiteObjects()
                    detectLeakedRegistrationObjects()
                    detectUnsafeIntentLaunch() // doesnt works on API <=11
                        .penaltyLog()
                    build()
                }
            )
        }

    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> logit("MapsDemo :The latest version of the renderer is used.")
            MapsInitializer.Renderer.LEGACY -> logit("MapsDemo :The legacy version of the renderer is used.")
        }
    }
}