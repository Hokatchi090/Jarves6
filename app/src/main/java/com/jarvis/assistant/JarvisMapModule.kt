package com.jarvis.assistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// \u0648\u062D\u062F\u0629 \u0627\u0644\u062E\u0631\u0627\u0626\u0637: \u062A\u062A\u0648\u0644\u0649 \u0627\u0644\u0645\u0644\u0627\u062D\u0629 (\u0641\u062A\u062D Google Maps) \u0648\u062D\u0633\u0627\u0628 \u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0628\u064A\u0646 \u0627\u0644\u0645\u062F\u0646 \u0623\u0648\u0641\u0644\u0627\u064A\u0646
// \u0646\u0641\u0633 \u0646\u0645\u0637 JarvisSystemModule: \u062A\u0623\u062E\u0630 Activity \u0644\u0644\u0648\u0635\u0648\u0644 \u0644\u0640 startActivity\u060C \u0648\u062F\u0627\u0644\u0629 speak \u0644\u0644\u0631\u062F
class JarvisMapModule(
    private val activity: Activity,
    private val speak: (String) -> Unit
) {

    fun execute(intent: JarvisIntent): Boolean {
        return when (intent.type) {
            JarvisIntentType.MAP_NAVIGATE -> handleNavigate(intent.argument)
            JarvisIntentType.MAP_DISTANCE -> handleDistance(intent.argument)
            else -> false
        }
    }

    private fun handleNavigate(place: String): Boolean {
        if (place.isBlank()) {
            speak("\u0642\u0644\u064A \u0648\u064A\u0646 \u0628\u062F\u0643 \u062A\u0631\u0648\u062D")
            return true
        }
        val encodedPlace = Uri.encode(place)
        val mapsUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=$encodedPlace&travelmode=driving"
        )
        return try {
            val mapIntent = Intent(Intent.ACTION_VIEW, mapsUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            activity.startActivity(mapIntent)
            speak("\u062C\u0627\u0631\u064A \u0641\u062A\u062D \u0627\u0644\u0637\u0631\u064A\u0642 \u0627\u0644\u0649 $place")
            true
        } catch (e: Exception) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, mapsUri))
                speak("\u062C\u0627\u0631\u064A \u0641\u062A\u062D \u0627\u0644\u0637\u0631\u064A\u0642 \u0627\u0644\u0649 $place")
            } catch (e2: Exception) {
                speak("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u062A\u062D \u0627\u0644\u062E\u0631\u0627\u0626\u0637")
            }
            true
        }
    }

    // argument \u0645\u062A\u0648\u0642\u0639: "\u0627\u0644\u062C\u0632\u0627\u0626\u0631|\u0648\u0647\u0631\u0627\u0646" (\u0627\u0633\u0645\u064A \u0627\u0644\u0645\u062F\u064A\u0646\u062A\u064A\u0646 \u0645\u0641\u0635\u0648\u0644\u064A\u0646 \u0628\u062E\u0637 \u0639\u0645\u0648\u062F\u064A)
    private fun handleDistance(argument: String): Boolean {
        val parts = argument.split("|")
        if (parts.size != 2) {
            speak("\u0642\u0648\u0644\u064A \u0627\u0633\u0645 \u0627\u0644\u0645\u062F\u064A\u0646\u062A\u064A\u0646 \u0627\u0644\u0644\u064A \u062A\u062D\u0628 \u062A\u0639\u0631\u0641 \u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0628\u064A\u0646\u0647\u0645")
            return true
        }
        val cityA = parts[0].trim()
        val cityB = parts[1].trim()
        val coordA = CityCoordinates.coordinates[cityA]
        val coordB = CityCoordinates.coordinates[cityB]
        if (coordA == null || coordB == null) {
            speak("\u0644\u0644\u0623\u0633\u0641 \u0645\u0627 \u0639\u0646\u062F\u064A \u0625\u062D\u062F\u0627\u062B\u064A\u0627\u062A \u0644\u0647\u0627\u064A \u0627\u0644\u0645\u062F\u064A\u0646\u0629 \u062D\u0627\u0644\u064A\u064B\u0627")
            return true
        }
        val distanceKm = haversine(coordA.first, coordA.second, coordB.first, coordB.second)
        speak("\u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0645\u0646 $cityA \u0627\u0644\u0649 $cityB \u062D\u0648\u0627\u0644\u064A ${distanceKm.toInt()} \u0643\u0645 (\u062E\u0637 \u0645\u0633\u062A\u0642\u064A\u0645 \u062A\u0642\u0631\u064A\u0628\u064A)")
        return true
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
