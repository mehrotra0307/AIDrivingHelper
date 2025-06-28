package com.example.aidrivinghelper

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: String,
    val timestamp: Long,           // for queries
    val date: String,              // for display (e.g., "Friday 6 PM")
    val summary: String,
    val durationMinutes: Int,
    val totalDistanceMiles: Float,
    val events: String
)

//fun TripEntity.toChunk(): String {
//    return """
//        Trip ID: $tripId
//        Date & Time: $date
//        Summary: $summary
//        Duration: $durationMinutes minutes
//        Distance: $totalDistanceMiles miles
//        Events Detected: $events
//    """.trimIndent()
//}

// WORKING CODE_----------------------------------------------------------
fun TripEntity.toChunk(): String {
    return """
        Trip ID: $tripId
        Date & Time: $date
        Summary: $summary
        Duration: $durationMinutes minutes
        Distance: $totalDistanceMiles miles
        Events Detected: $events
        Notes: ${when {
            events.contains("acceleration", ignoreCase = true) -> "Rapid acceleration detected."
            events.contains("braking", ignoreCase = true) -> "Sudden braking observed."
            events.contains("phone", ignoreCase = true) -> "Driver used phone during this trip."
            else -> "No unusual events."
        }}
    """.trimIndent()
}


//fun TripEntity.toChunk(): TextChunk {
//    return TextChunk(
//        id = tripId,
//        content = """
//            Trip ID: $tripId
//            Date: $date
//            Summary: $summary
//            Duration: $durationMinutes minutes
//            Distance: $totalDistanceMiles miles
//            Events Detected: $events
//        """.trimIndent(),
//        metadata = mapOf(
//            "event" to events.lowercase(),
//            "timestamp" to timestamp.toString()
//        )
//    )
//}














