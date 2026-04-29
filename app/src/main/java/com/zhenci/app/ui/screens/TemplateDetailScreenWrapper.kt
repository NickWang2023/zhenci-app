package com.zhenci.app.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhenci.app.data.entity.Template
import com.zhenci.app.viewmodel.TemplateViewModel
import android.app.Application

/**
 * 模板详情页的包装器
 * 负责从数据库加载模板信息，然后显示详情页
 */
@Composable
fun TemplateDetailScreenWrapper(
    templateId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: TemplateViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TemplateViewModel(context.applicationContext as Application) as T
            }
        }
    )

    val templates by viewModel.templates.collectAsState()
    val template = templates.find { it.id == templateId }

    if (template != null) {
        TemplateDetailScreen(
            template = template,
            onBack = onBack
        )
    }
}
