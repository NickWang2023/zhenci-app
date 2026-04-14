package com.zhenci.app.data.database

import androidx.room.*
import com.zhenci.app.data.entity.Template
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultTemplate(): Template?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("UPDATE templates SET isDefault = 0")
    suspend fun clearDefault()

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): Template?
}