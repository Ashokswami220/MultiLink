package com.example.multilink.repo

import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Data models to hold Turn-by-Turn Navigation data
data class NavStep(
    val instruction: String,
    val maneuverType: String,
    val modifier: String,
    val location: Point
)

data class RouteResult(
    val routePoints: List<Point>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<NavStep>
)

class RouteRepository(private val accessToken: String) {

    // 1. ORIGINAL FUNCTION: Used by DetailScreen and LiveTrackingScreen
    suspend fun getRoute(start: Point, end: Point): List<Point> = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                    "${start.longitude()},${start.latitude()};${end.longitude()},${end.latitude()}" +
                    "?geometries=polyline6&overview=full&access_token=$accessToken"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader()
                    .use { it.readText() }
                val json = JSONObject(response)

                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val geometry = routes.getJSONObject(0)
                        .getString("geometry")
                    return@withContext PolylineUtils.decode(geometry, 6)
                }
            }
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 2. NEW FUNCTION: Used by SoloNavigationScreen for Turn-by-Turn data
    suspend fun getNavigationRoute(start: Point, end: Point): RouteResult? =
        withContext(Dispatchers.IO) {
            try {
                val urlString = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                        "${start.longitude()},${start.latitude()};${end.longitude()},${end.latitude()}" +
                        "?geometries=polyline6&overview=full&steps=true&access_token=$accessToken"

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader()
                        .use { it.readText() }
                    val json = JSONObject(response)

                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)

                        val geometry = route.getString("geometry")
                        val routePoints = PolylineUtils.decode(geometry, 6)

                        val distance = route.getDouble("distance")
                        val duration = route.getDouble("duration")

                        val stepsList = mutableListOf<NavStep>()
                        val legs = route.getJSONArray("legs")
                        if (legs.length() > 0) {
                            val stepsArray = legs.getJSONObject(0)
                                .getJSONArray("steps")
                            for (i in 0 until stepsArray.length()) {
                                val stepObj = stepsArray.getJSONObject(i)
                                val maneuver = stepObj.getJSONObject("maneuver")

                                val instruction = maneuver.optString("instruction", "Continue")
                                val type = maneuver.optString("type", "")
                                val modifier = maneuver.optString("modifier", "straight")

                                val locArray = maneuver.getJSONArray("location")
                                val locPoint =
                                    Point.fromLngLat(locArray.getDouble(0), locArray.getDouble(1))

                                stepsList.add(NavStep(instruction, type, modifier, locPoint))
                            }
                        }

                        return@withContext RouteResult(routePoints, distance, duration, stepsList)
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}