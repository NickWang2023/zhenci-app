package com.zhenci.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val id: Int = 1,
    val totalScore: Int = 0,           // 总积分
    val todayScore: Int = 0,           // 今日积分
    val totalExecuted: Int = 0,        // 总执行次数
    val todayExecuted: Int = 0,        // 今日执行次数
    val totalClosed: Int = 0,          // 总关闭次数
    val todayClosed: Int = 0,          // 今日关闭次数
    val currentStreak: Int = 0,        // 当前连续天数
    val maxStreak: Int = 0,            // 最大连续天数
    val lastActiveDate: String = ""    // 最后活跃日期（yyyy-MM-dd）
)