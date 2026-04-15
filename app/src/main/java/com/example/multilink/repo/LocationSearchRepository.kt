package com.example.multilink.repo

import com.example.multilink.BuildConfig
import com.example.multilink.model.SearchResult
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class LocationSearchRepository {
    // Tokens are now safely encapsulated in the Repository layer
    private val token = BuildConfig.MAPBOX_ACCESS_TOKEN
    private val sessionToken = UUID.randomUUID()
        .toString()

    suspend fun getSuggestions(query: String, proximity: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) return@withContext emptyList()

                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url =
                    "https://api.mapbox.com/search/searchbox/v1/suggest?q=$encodedQuery&access_token=$token&session_token=$sessionToken&proximity=$proximity&language=en&limit=5&types=poi,address,place"

                val json = URL(url).readText()
                val suggestions = JSONObject(json).getJSONArray("suggestions")

                val list = mutableListOf<SearchResult>()
                for (i in 0 until suggestions.length()) {
                    val item = suggestions.getJSONObject(i)
                    val name = item.getString("name")
                    val address =
                        if (item.has("full_address")) item.getString("full_address") else ""
                    val mapboxId = if (item.has("mapbox_id")) item.getString("mapbox_id") else null

                    if (mapboxId != null) {
                        list.add(
                            SearchResult(
                                name, address, Point.fromLngLat(0.0, 0.0), mapboxId = mapboxId
                            )
                        )
                    }
                }
                list
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun getPlaceDetails(mapboxId: String): Point? = withContext(Dispatchers.IO) {
        try {
            val retrieveUrl =
                "https://api.mapbox.com/search/searchbox/v1/retrieve/$mapboxId?access_token=$token&session_token=$sessionToken"
            val json = URL(retrieveUrl).readText()
            val feature = JSONObject(json).getJSONArray("features")
                .getJSONObject(0)
            val coords = feature.getJSONObject("geometry")
                .getJSONArray("coordinates")
            Point.fromLngLat(coords.getDouble(0), coords.getDouble(1))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}