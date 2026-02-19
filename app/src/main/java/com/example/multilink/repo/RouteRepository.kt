package com.example.multilink.repo

import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RouteRepository(private val accessToken: String) {

    suspend fun getRoute(start: Point, end: Point): List<Point> = withContext(Dispatchers.IO) {
        try {
            // 1. Build URL for Driving Directions
            // We use the 'accessToken' string passed from the UI
            val urlString = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                    "${start.longitude()},${start.latitude()};${end.longitude()},${end.latitude()}" +
                    "?geometries=polyline6&overview=full&access_token=$accessToken"

            // 2. Fetch Data
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                // 3. Parse Route Geometry
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val geometry = routes.getJSONObject(0).getString("geometry")
                    // Decode the Polyline6 string into Points
                    return@withContext PolylineUtils.decode(geometry, 6)
                }
            }
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}