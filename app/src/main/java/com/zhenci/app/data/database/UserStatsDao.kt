package com.zhenci.app.data.database

import androidx.room.*
import com.zhenci.app.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsSync(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStats)

    @Query("UPDATE user_stats SET totalScore = totalScore + :points, todayScore = todayScore + :points WHERE id = 1")
    suspend fun addScore(points: Int)

    @Query("UPDATE user_stats SET totalExecuted = totalExecuted + 1, todayExecuted = todayExecuted + 1 WHERE id = 1")
    suspend fun incrementExecuted()

    @Query("UPDATE user_stats SET totalClosed = totalClosed + 1, todayClosed = todayClosed + 1 WHERE id = 1")
    suspend fun incrementClosed()

    @Query("UPDATE user_stats SET todayScore = 0, todayExecuted = 0, todayClosed = 0 WHERE id = 1")
    suspend fun resetDailyStats()
}