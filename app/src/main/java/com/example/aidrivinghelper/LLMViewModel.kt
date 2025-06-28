package com.example.aidrivinghelper

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

class LLMViewModel(private val application: Application) : AndroidViewModel(application) {

    private val llmInferenceChain = LLMInferenceChain(application)
    private val backgroundExecutor: Executor = Executors.newSingleThreadExecutor()

    // Observables for Compose UI
    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput.asStateFlow()

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response.asStateFlow()

    fun onInputChanged(input: String) {
        _userInput.value = input
    }

    fun onSubmit() {
        viewModelScope.launch {
            val result = llmInferenceChain.generateResponse(_userInput.value, null)
            _response.value = stripMarkdown(result)
        }
    }


    suspend fun memorizeChunk(fileName: String) {
        withContext(backgroundExecutor.asCoroutineDispatcher()) {
            llmInferenceChain.memorizeChunks(application.applicationContext, fileName)
        }
    }


    suspend fun insertDemoTrips(context: Context) {
        val dao = AppDatabase.getDatabase(context).tripDao()

        // Insert only if DB is empty
        if (dao.getAllTrips().isNotEmpty()) return

        val demoTrips = listOf(
            TripEntity(
                tripId = "T001",
                timestamp = 1718377800000L, // 2024-06-14 18:00
                date = "Friday 6 PM",
                summary = "15-minute night trip with speeding in a residential area",
                durationMinutes = 15,
                totalDistanceMiles = 7.8f,
                events = "speeding"
            ),
            TripEntity(
                tripId = "T002",
                timestamp = 1718420400000L, // 2024-06-15 07:00
                date = "Saturday 7 AM",
                summary = "Phone usage observed during 10-minute morning commute",
                durationMinutes = 10,
                totalDistanceMiles = 5.1f,
                events = "phone"
            ),
            TripEntity(
                tripId = "T003",
                timestamp = 1718578500000L, // 2024-06-16 21:00
                date = "Sunday 9 PM",
                summary = "Sudden braking recorded during evening trip",
                durationMinutes = 12,
                totalDistanceMiles = 5.6f,
                events = "sudden braking"
            ),
            TripEntity(
                tripId = "T004",
                timestamp = 1718625600000L, // 2024-06-17 12:00
                date = "Monday 12 PM",
                summary = "Smooth 14-minute drive with no incidents",
                durationMinutes = 14,
                totalDistanceMiles = 6.8f,
                events = "none"
            ),
            TripEntity(
                tripId = "T005",
                timestamp = 1718732700000L, // 2024-06-18 19:45
                date = "Tuesday 7 PM",
                summary = "Rapid acceleration noted during highway merge",
                durationMinutes = 11,
                totalDistanceMiles = 6.5f,
                events = "acceleration"
            ),
            TripEntity(
                tripId = "T006",
                timestamp = 1718811600000L, // 2024-06-19 22:00
                date = "Wednesday 10 PM",
                summary = "Phone usage again during late-night trip",
                durationMinutes = 13,
                totalDistanceMiles = 6.0f,
                events = "phone"
            ),
            TripEntity(
                tripId = "T007",
                timestamp = 1718889600000L, // 2024-06-20 17:00
                date = "Thursday 5 PM",
                summary = "Evening trip with no recorded events",
                durationMinutes = 16,
                totalDistanceMiles = 8.1f,
                events = "none"
            )
        )

        demoTrips.forEach { dao.insertTrip(it) }
    }



    // Convert "YYYY-MM-DD HH:mm" to timestamp
    fun toTimestamp(datetime: String): Long {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
        return formatter.parse(datetime)?.time ?: System.currentTimeMillis()
    }


    fun embedTripsFromDatabase(context: Context) {
        viewModelScope.launch {
            val tripDao = AppDatabase.getDatabase(context).tripDao()
            val trips = tripDao.getAllTrips()
            Log.d("LLMCheck", "all trips from DB are $trips")
            val chunks = trips.map { it.toChunk() }
            Log.d("LLMCheck", "Memorized Chunks:\n${chunks.joinToString("\n\n")}")
            llmInferenceChain.memorize(chunks)
        }
    }

    fun stripMarkdown(text: String): String {
        return text
            // Remove bold/italic/underline markers: **, *, __, _
            .replace(Regex("([*_]{1,3})"), "")
            // Remove backticks for code blocks or inline code
            .replace(Regex("`+"), "")
            // Remove leading bullet characters like - or *
            .replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "")
            // Trim extra spaces or line breaks
            .trim()
    }


}
