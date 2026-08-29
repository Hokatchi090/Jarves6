package com.jarvis.assistant

// \u0648\u062D\u062F\u0629 \u0645\u062A\u062E\u0635\u0635\u0629 \u0644\u0637\u0644\u0627\u0628 \u0627\u0644\u062C\u064A\u0648\u0644\u0648\u062C\u064A\u0627: \u062F\u0641\u062A\u0631 \u0645\u064A\u062F\u0627\u0646\u064A GPS + \u0628\u0648\u0635\u0644\u0629 \u0642\u064A\u0627\u0633 \u062D\u0642\u064A\u0642\u064A\u0629
// \u062A\u0623\u062E\u0630 lastLat/lastLon \u0645\u0646 MainActivity \u0644\u0623\u0646\u0647\u0627 \u0645\u062A\u062D\u062F\u0651\u062B\u0629 \u0628\u0627\u0644\u0641\u0639\u0644 \u0639\u0628\u0631 fetchAndShowLocation() \u0627\u0644\u0645\u0648\u062C\u0648\u062F\u0629 \u0623\u0635\u0644\u0627\u064B
class JarvisGeologyModule(
    private val notebook: FieldNotebookManager,
    private val compass: GeoCompassHelper,
    private val speak: (String) -> Unit,
    private val getCurrentLocation: () -> Pair<Double, Double>
) {

    fun execute(intent: JarvisIntent): Boolean {
        return when (intent.type) {
            JarvisIntentType.FIELD_LOG_ADD -> { addFieldNote(intent.argument); true }
            JarvisIntentType.FIELD_LOG_LIST -> { listFieldNotes(); true }
            JarvisIntentType.FIELD_LOG_EXPORT -> { exportFieldNotes(); true }
            JarvisIntentType.COMPASS_READ -> { readCompass(); true }
            else -> false
        }
    }

    // argument \u0645\u062A\u0648\u0642\u0639: \u0627\u0644\u0645\u0644\u0627\u062D\u0638\u0629 \u0627\u0644\u0646\u0635\u064A\u0629\u060C \u0645\u0639 "|" \u0627\u062E\u062A\u064A\u0627\u0631\u064A \u0644\u0646\u0648\u0639 \u0627\u0644\u0635\u062E\u0631 \u0625\u0630\u0627 \u0627\u0646\u0630\u0643\u0631
    private fun addFieldNote(argument: String) {
        if (argument.isBlank()) {
            speak("\u0642\u0648\u0644\u064A \u0627\u0644\u0645\u0644\u0627\u062D\u0638\u0629 \u0627\u0644\u0644\u064A \u062A\u062D\u0628 \u062A\u0633\u062C\u0644\u0647\u0627")
            return
        }
        val (lat, lon) = getCurrentLocation()
        if (lat == 0.0 && lon == 0.0) {
            speak("\u0645\u0627\u0632\u0627\u0644 \u0645\u0627\u0639\u0646\u062F\u064A \u0645\u0648\u0642\u0639 \u062D\u0642\u064A\u0642\u064A\u060C \u0631\u0648\u062D \u0644\u0634\u0627\u0634\u0629 MAP \u0648\u0637\u0644\u0628 \u0627\u0644\u0645\u0648\u0642\u0639 \u0623\u0648\u0644")
            return
        }
        val parts = argument.split("|")
        val note = parts[0].trim()
        val rockType = if (parts.size > 1) parts[1].trim() else ""
        notebook.addEntry(lat, lon, note, rockType)
        speak("\u062A\u0645 \u062A\u0633\u062C\u064A\u0644 \u0627\u0644\u0645\u0644\u0627\u062D\u0638\u0629 \u0641\u064A \u0627\u0644\u062F\u0641\u062A\u0631 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A. \u0639\u0646\u062F\u0643 \u062F\u0631\u0648\u0643 ${notebook.count()} \u0645\u0644\u0627\u062D\u0638\u0629")
    }

    private fun listFieldNotes() {
        val entries = notebook.getAll()
        if (entries.isEmpty()) {
            speak("\u0627\u0644\u062F\u0641\u062A\u0631 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A \u0641\u0627\u0636\u064A \u0644\u0633\u0627")
            return
        }
        val recent = entries.take(3).joinToString(". ") { notebook.formatEntry(it) }
        speak("\u0639\u0646\u062F\u0643 ${entries.size} \u0645\u0644\u0627\u062D\u0638\u0629. \u0622\u062E\u0631 \u062B\u0644\u0627\u062B\u0629: $recent")
    }

    private fun exportFieldNotes() {
        if (notebook.count() == 0) {
            speak("\u0627\u0644\u062F\u0641\u062A\u0631 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A \u0641\u0627\u0636\u064A\u060C \u0645\u0627\u0641\u064A\u0634 \u0634\u064A \u0646\u0635\u062F\u0651\u0631\u0647")
            return
        }
        val path = notebook.exportToFile()
        if (path != null) {
            speak("\u062A\u0645 \u062D\u0641\u0638 ${notebook.count()} \u0645\u0644\u0627\u062D\u0638\u0629 \u0641\u064A \u0645\u0644\u0641: $path")
        } else {
            speak("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u062D\u0641\u0638 \u0627\u0644\u0645\u0644\u0641")
        }
    }

    private fun readCompass() {
        if (!compass.isAvailable()) {
            speak("\u0627\u0644\u062C\u0647\u0627\u0632 \u0645\u0627\u0641\u064A\u0634 \u062D\u0633\u0627\u0633\u0627\u062A \u0628\u0648\u0635\u0644\u0629 \u0643\u0627\u0641\u064A\u0629")
            return
        }
        val bearing = compass.getBearing()
        val direction = compass.getCompassDirectionLabel()
        val tilt = compass.getTiltDegrees()
        speak(
            "\u0627\u0644\u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u062D\u0627\u0644\u064A ${bearing.toInt()} \u062F\u0631\u062C\u0629\u060C \u0646\u062D\u0648 $direction. " +
                "\u0645\u064A\u0644 \u0627\u0644\u062C\u0647\u0627\u0632 \u0639\u0646 \u0627\u0644\u0623\u0641\u0642\u064A ${tilt.toInt()} \u062F\u0631\u062C\u0629"
        )
    }
}
