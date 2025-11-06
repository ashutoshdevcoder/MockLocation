package com.ashdev.mocklocation.sharedpref

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ashdev.mocklocation.helper.AppConstant

class EncryptedPreferenceHelperImpl constructor(context: Context):IEncryptedPreferenceHelper {

    private var masterKeyAlias: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    } else {
        null
    }
    private var preferences: SharedPreferences? = null

    init {
        preferences = if (masterKeyAlias != null) {
            try {
                createSharedPreferencesEncrypted(context, masterKeyAlias!!)
            } catch (e: Exception) {
                createSharedPreferencesNormal(context)
            }
        } else {
            createSharedPreferencesNormal(context)
        }
    }

    @Synchronized
    private fun createSharedPreferencesEncrypted(context: Context, masterKeyAlias: String): SharedPreferences {
        return EncryptedSharedPreferences.create(
            AppConstant.PREFS_NAME_ENCRYPTED,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun createSharedPreferencesNormal(context: Context): SharedPreferences{
        return context.getSharedPreferences(AppConstant.PREFS_NAME_NORMAL, Context.MODE_PRIVATE)
    }
}