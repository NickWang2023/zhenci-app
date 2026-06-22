package com.zhenci.app.data.entity

data class AIConfig(
    val provider: AIProvider,
    val apiKey: String,
    val model: String = "",
    val baseUrl: String = ""
) {
    companion object {
        const val PREFS_NAME = "ai_config_prefs"
        const val KEY_PROVIDER = "ai_provider"
        const val KEY_API_KEY = "ai_api_key"
        const val KEY_MODEL = "ai_model"
        const val KEY_BASE_URL = "ai_base_url"
        
        // 默认Kimi配置
        val DEFAULT_KIMI = AIConfig(
            provider = AIProvider.KIMI,
            apiKey = "",
            model = "moonshot-v1-8k",
            baseUrl = "https://api.moonshot.cn/v1"
        )
    }
}

enum class AIProvider(val displayName: String) {
    KIMI("Kimi (月之暗面)"),
    OPENAI("OpenAI"),
    CUSTOM("自定义");
    
    fun getDefaultModel(): String = when (this) {
        KIMI -> "moonshot-v1-8k"
        OPENAI -> "gpt-4o-mini"
        CUSTOM -> ""
    }
    
    fun getDefaultBaseUrl(): String = when (this) {
        KIMI -> "https://api.moonshot.cn/v1"
        OPENAI -> "https://api.openai.com/v1"
        CUSTOM -> ""
    }
}