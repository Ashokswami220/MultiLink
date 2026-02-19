package com.example.multilink

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import com.mapbox.common.MapboxOptions

class MultiLinkApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            FirebaseDatabase.getInstance()
                .setPersistenceEnabled(true)
        } catch (_: Exception) {
        }

        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}