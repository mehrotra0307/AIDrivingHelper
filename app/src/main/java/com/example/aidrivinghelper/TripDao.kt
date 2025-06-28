package com.example.aidrivinghelper

import androidx.room.*

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY timestamp DESC")
    suspend fun getAllTrips(): List<TripEntity>

    @Query("SELECT SUM(totalDistanceMiles) FROM trips WHERE timestamp >= :startOfWeek")
    suspend fun getWeeklyDistance(startOfWeek: Long): Float?

}
