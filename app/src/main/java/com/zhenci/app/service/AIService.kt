package com.zhenci.app.service

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.zhenci.app.data.entity.AIConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class AIService(private val config: AIConfig) {
    
    companion object {
        private const val TAG = "AIService"
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 30L
    }
    
    private val gson = Gson()
    
    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
    
    data class ScheduleRequest(
        val profession: String,
        val familySituation: String,
        val constraints: String,
        val preferences: String = "",
        val workDays: List<String> = listOf("周一", "周二", "周三", "周四", "周五")
    )
    
    data class GeneratedSchedule(
        val templateName: String,
        val description: String,
        val tasks: List<GeneratedTask>
    )
    
    data class GeneratedTask(
        val content: String,
        val hour: Int,
        val minute: Int,
        val description: String = ""
    )
    
    suspend fun generateSchedule(request: ScheduleRequest): Result<GeneratedSchedule> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(request)
            val response = when {
                config.baseUrl.contains("moonshot") || config.model.contains("moonshot") -> {
                    callKimiAPI(prompt)
                }
                config.baseUrl.contains("openai") || config.model.contains("gpt") -> {
                    callOpenAIAPI(prompt)
                }
                else -> {
                    callGenericOpenAICompatibleAPI(prompt)
                }
            }
            
            response?.let { json ->
                parseScheduleResponse(json)
            } ?: Result.failure(IOException("Empty response from API"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate schedule", e)
            Result.failure(e)
        }
    }
    
    private fun buildPrompt(request: ScheduleRequest): String {
        val workDaysStr = request.workDays.joinToString("、")
        
        return """
你是一个专业的日程规划助手。请根据以下用户信息，生成一个${workDaysStr}的日程模板。

用户信息：
- 职业/身份：${request.profession}
- 家庭情况：${request.familySituation}
- 时间约束：${request.constraints}
${if (request.preferences.isNotBlank()) "- 其他偏好：${request.preferences}" else ""}

请生成一个合理、健康的日程安排，遵循以下原则：
1. 时间要符合人体生物钟规律
2. 任务之间要有合理的缓冲时间
3. 包含必要的休息、用餐、运动时间
4. 考虑用户的特殊时间约束（如接送孩子）
5. 任务安排要符合职业特点

返回格式必须是JSON（不要包含markdown代码块标记）：
{
  "templateName": "模板名称",
  "description": "模板描述",
  "tasks": [
    {"content": "任务名称", "hour": 6, "minute": 30, "description": "任务描述"},
    {"content": "任务名称", "hour": 7, "minute": 0, "description": "任务描述"}
  ]
}

注意：
- hour范围是0-23，minute范围是0-59
- tasks数组按时间顺序排列
- 一天建议安排8-12个任务，不要太多
- 任务内容要简洁明了（4-10个字）
""".trimIndent()
    }
    
    private fun callKimiAPI(prompt: String): String? {
        val url = "${config.baseUrl}/chat/completions"
        
        val requestBody = KimiRequest(
            model = config.model.ifBlank { "moonshot-v1-8k" },
            messages = listOf(
                Message(role = "system", content = "你是一个专业的日程规划助手，专门帮助用户生成合理的日程安排。只返回JSON格式，不要添加任何解释。"),
                Message(role = "user", content = prompt)
            ),
            temperature = 0.7
        )
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API call failed: ${response.code} - ${response.body?.string()}")
            }
            
            val body = response.body?.string() ?: throw IOException("Empty response")
            val kimiResponse = gson.fromJson(body, KimiResponse::class.java)
            return kimiResponse.choices.firstOrNull()?.message?.content
        }
    }
    
    private fun callOpenAIAPI(prompt: String): String? {
        val url = "${config.baseUrl}/chat/completions"
        
        val requestBody = OpenAIRequest(
            model = config.model.ifBlank { "gpt-4o-mini" },
            messages = listOf(
                OpenAIMessage(role = "system", content = "你是一个专业的日程规划助手，专门帮助用户生成合理的日程安排。只返回JSON格式，不要添加任何解释。"),
                OpenAIMessage(role = "user", content = prompt)
            ),
            temperature = 0.7
        )
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API call failed: ${response.code} - ${response.body?.string()}")
            }
            
            val body = response.body?.string() ?: throw IOException("Empty response")
            val openAIResponse = gson.fromJson(body, OpenAIResponse::class.java)
            return openAIResponse.choices.firstOrNull()?.message?.content
        }
    }
    
    private fun callGenericOpenAICompatibleAPI(prompt: String): String? {
        // 使用OpenAI兼容格式
        return callOpenAIAPI(prompt)
    }
    
    private fun parseScheduleResponse(content: String): Result<GeneratedSchedule> {
        return try {
            // 清理可能的markdown代码块
            val cleanJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val schedule = gson.fromJson(cleanJson, GeneratedSchedule::class.java)
            
            // 验证数据
            if (schedule.templateName.isBlank()) {
                return Result.failure(IllegalArgumentException("模板名称为空"))
            }
            
            if (schedule.tasks.isEmpty()) {
                return Result.failure(IllegalArgumentException("任务列表为空"))
            }
            
            // 验证每个任务的时间
            schedule.tasks.forEach { task ->
                if (task.hour !in 0..23) {
                    throw IllegalArgumentException("无效的小时: ${task.hour}")
                }
                if (task.minute !in 0..59) {
                    throw IllegalArgumentException("无效的分钟: ${task.minute}")
                }
            }
            
            Result.success(schedule)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse schedule response: $content", e)
            Result.failure(e)
        }
    }
    
    // Kimi API Models
    private data class KimiRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.7
    )
    
    private data class Message(
        val role: String,
        val content: String
    )
    
    private data class KimiResponse(
        val choices: List<KimiChoice>
    )
    
    private data class KimiChoice(
        val message: KimiMessage
    )
    
    private data class KimiMessage(
        val content: String
    )
    
    // OpenAI API Models
    private data class OpenAIRequest(
        val model: String,
        val messages: List<OpenAIMessage>,
        val temperature: Double = 0.7
    )
    
    private data class OpenAIMessage(
        val role: String,
        val content: String
    )
    
    private data class OpenAIResponse(
        val choices: List<OpenAIChoice>
    )
    
    private data class OpenAIChoice(
        val message: OpenAIResponseMessage
    )
    
    private data class OpenAIResponseMessage(
        val content: String
    )
}