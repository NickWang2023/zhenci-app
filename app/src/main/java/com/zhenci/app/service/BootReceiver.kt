package com.zhenci.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zhenci.app.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 系统级【补排】接收器：
 *  - ACTION_BOOT_COMPLETED            ：设备开机
 *  - Intent.ACTION_DATE_CHANGED       ：跨零点 / 日期变更（每日重复最重要的续链兜底）
 *  - Intent.ACTION_TIME_CHANGED       ：用户改时间
 *  - Intent.ACTION_TIMEZONE_CHANGED   ：改时区
 *  - Intent.ACTION_MY_PACKAGE_REPLACED：应用更新覆盖安装（闹钟会被系统清除，需重建）
 *  - Intent.ACTION_PACKAGE_RESTARTED  ：进程被杀后重启
 *
 * 目标：无论上面哪种事件，都把首页「今日日程」里所有启用 (isEnabled && templateId==0)
 * 的每日重复闹钟重新排到『下一次时刻』，确保闹钟不丢、不过时、不需等用户打开 App。
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val shouldReschedule = action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_DATE_CHANGED ||
                action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == Intent.ACTION_PACKAGE_RESTARTED

        if (!shouldReschedule) return

        Log.d(TAG, "onReceive: 收到系统事件 $action，开始全量重排每日日程")

        // 广播接收器超过 ~10s 会被系统视为 ANR 强杀，必须 goAsync + IO
        val pending = goAsync()
        scope.launch {
            try {
                val dao = AppDatabase.getDatabase(context.applicationContext).taskDao()
                // 首页今日日程 = templateId==0；启用 (isEnabled==1) 的每日重复项才排
                val enabledTasks = dao.getAllTasksSync()
                    .filter { it.isEnabled && it.templateId == 0L }
                Log.d(TAG, "onReceive: 查得启用日程 ${enabledTasks.size} 条，开始重排")
                AlarmScheduler(context.applicationContext).scheduleAll(enabledTasks)
            } catch (e: Exception) {
                Log.e(TAG, "onReceive: 重排失败 ${e.message}", e)
            } finally {
                pending.finish()
            }
        }
    }
}
