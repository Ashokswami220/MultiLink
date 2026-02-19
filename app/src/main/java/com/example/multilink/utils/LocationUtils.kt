package com.example.multilink.utils

import android.location.Location
import com.mapbox.geojson.Point

object LocationUtils {
    fun calculateDistance(p1: Point?, p2: Point?): String {
        if (p1 == null || p2 == null) return "..."
        return calculateDistance(p1.latitude(), p1.longitude(), p2.latitude(), p2.longitude())
    }

    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): String {
        if (lat1 == 0.0 || lat2 == 0.0) return "..."

        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        val distanceMeters = results[0]

        return if (distanceMeters >= 1000) {
            String.format("%.1f km", distanceMeters / 1000)
        } else {
            String.format("%.0f m", distanceMeters)
        }
    }
}