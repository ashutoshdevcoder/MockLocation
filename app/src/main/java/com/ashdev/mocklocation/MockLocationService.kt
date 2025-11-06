package com.ashdev.mocklocation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import com.ashdev.mocklocation.helper.AppConstant
import com.ashdev.mocklocation.helper.MockLocationProvider
import com.ashdev.mocklocation.helper.showToast
import com.ashdev.mocklocation.ui.MainActivity
import java.util.Random

class MockLocationService:Service() {
    private var runnable: Runnable?=null
    private var mockNetwork: MockLocationProvider? = null
    private var mockGps: MockLocationProvider? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var address:String=""
    private var isNotificationAdded=false
    val handler= Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latitude=intent?.getDoubleExtra(AppConstant.LATITUDE,0.0) ?:0.0
        longitude=intent?.getDoubleExtra(AppConstant.LONGITUDE,0.0) ?:0.0
        address=intent?.getStringExtra(AppConstant.ADDRESS) ?:""
        if(intent?.action.equals(AppConstant.STOP,true))
        {
            stopSelf()
        }
        else
            applyLocation()
        return START_STICKY
    }

    private fun applyLocation() {
        if (latitude==0.0 && longitude==0.0) {
            showToast(getString(R.string.MainActivity_NoLatLong))
            return
        }

        try {
            mockNetwork =
                MockLocationProvider(LocationManager.NETWORK_PROVIDER, this)
            mockGps =
                MockLocationProvider(LocationManager.GPS_PROVIDER, this)
        } catch (e: SecurityException) {
            e.printStackTrace()
            showToast(getString(R.string.ApplyMockBroadRec_MockNotApplied))
            stopMockingLocation()
            return
        }
        runnable=object :Runnable{
            override fun run() {
                exec()
                handler.postDelayed(this,1000)
            }

        }
        runnable?.let {
            handler.postDelayed(it,1000)
        }

    }

     fun stopMockingLocation() {
        mockNetwork?.shutdown()
        mockGps?.shutdown()
         isNotificationAdded=false
         runnable?.let {
             handler.removeCallbacks(it)
         }
        removeNotification()
    }

    private fun exec() {
        try {
            //MockLocationProvider mockNetwork = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);
            mockNetwork?.pushLocation(latitude, longitude)
            //MockLocationProvider mockGps = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
            mockGps?.pushLocation(latitude, longitude)
             if(isNotificationAdded.not())
             {
                 isNotificationAdded=true
                 createNotification(address)
             }
        } catch (e: java.lang.Exception) {
            removeNotification()
            showToast(getString(R.string.MainActivity_MockNotApplied))
            e.printStackTrace()
            return
        }
    }
    private fun removeNotification() {
        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }


    override fun onDestroy() {
        stopMockingLocation()
        super.onDestroy()
    }
    fun createNotification(message:String)
    {
        val channelId = AppConstant.CHANNEL_ID
        val applicationContext= MockLocationApplication.appContext
        val mBuilder = NotificationCompat.Builder(
            applicationContext, applicationContext.getString(R.string.app_name)
        )
        val intentSplash = Intent(applicationContext, MainActivity::class.java)
        val pendingIntentSplash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            PendingIntent.getActivity(applicationContext, 0, intentSplash, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }else {
            PendingIntent.getActivity(applicationContext, 0, intentSplash, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val intent=Intent(this,MockLocationService::class.java)
        intent.action = AppConstant.STOP
        val pendingIntentService = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }else {
            PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val openAction = NotificationCompat.Action.Builder(
            0, AppConstant.OPEN, pendingIntentSplash
        ).build()
        val stopAction = NotificationCompat.Action.Builder(
            0, AppConstant.STOP, pendingIntentService
        ).build()
        val bigText = NotificationCompat.BigTextStyle()
        with(mBuilder)
        {
            setContentIntent(pendingIntentSplash)
            setSmallIcon(R.mipmap.ic_launcher)
            color = ResourcesCompat.getColor(applicationContext.resources, R.color.darkBlue, null)
            setContentText(message)
            priority = Notification.PRIORITY_DEFAULT
            setStyle(bigText)
            setOngoing(true)
            addAction(openAction)
            addAction(stopAction)
        }

        val mNotificationManager = applicationContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelId,
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            mBuilder.setChannelId(channelId)
        }
        val id = Random().nextInt(9999 - 1000) + 100
        startForeground(id,mBuilder.build())
    }
}