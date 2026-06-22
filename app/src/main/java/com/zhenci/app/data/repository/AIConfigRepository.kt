package com.zhenci.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.zhenci.app.data.entity.AIConfig
import com.zhenci.app.data.entity.AIProvider

class AIConfigRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        AIConfig.PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    fun getConfig(): AIConfig {
        val providerName = prefs.getString(AIConfig.KEY_PROVIDER, AIProvider.KIMI.name)
        val provider = try {
            AIProvider.valueOf(providerName ?: AIProvider.KIMI.name)
        } catch (e: Exception) {
            AIProvider.KIMI
        }
        
        return AIConfig(
            provider = provider,
            apiKey = prefs.getString(AIConfig.KEY_API_KEY, "") ?: "",
            model = prefs.getString(AIConfig.KEY_MODEL, provider.getDefaultModel()) ?: provider.getDefaultModel(),
            baseUrl = prefs.getString(AIConfig.KEY_BASE_URL, provider.getDefaultBaseUrl()) ?: provider.getDefaultBaseUrl()
        )
    }
    
    fun saveConfig(config: AIConfig) {
        prefs.edit().apply {
            putString(AIConfig.KEY_PROVIDER, config.provider.name)
            putString(AIConfig.KEY_API_KEY, config.apiKey)
            putString(AIConfig.KEY_MODEL, config.model)
            putString(AIConfig.KEY_BASE_URL, config.baseUrl)
            apply()
        }
    }
    
    fun hasApiKey(): Boolean {
        return prefs.getString(AIConfig.KEY_API_KEY, "")?.isNotBlank() == true
    }
    
    fun clearConfig() {
        prefs.edit().clear().apply()
    }
}