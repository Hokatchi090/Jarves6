package com.jarvisx.app.sync

import android.content.Context
import androidx.work.*
import com.jarvisx.app.AppMode
import com.jarvisx.app.data.FirebaseHelper
import com.jarvisx.app.data.LocalDatabase
import com.jarvisx.app.data.PendingActionEntity
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * القلب النابض للـ Offline-First: أي عملية كتابة (رسالة، تبديل صلاحية، تسجيل
 * عضو) تُحفظ محليًا فورًا عبر enqueue(...) وتُعرض في الواجهة مباشرة (Optimistic
 * UI). SyncWorker يفرّغ القائمة لـ Firebase بمجرد توفر شبكة، بترتيب ومحاولات
 * متكررة (Exponential Backoff) بدون أي تدخل يدوي من المستخدم.
 */
object SyncManager {

    private const val UNIQUE_WORK_NAME = "jarvisx_sync_worker"

    /** يضيف عملية لقائمة الانتظار المحلية، ويطلب تشغيل المزامنة عند توفر النت */
    suspend fun enqueue(context: Context, type: String, payload: JSONObject) {
        val dao = LocalDatabase.getInstance(context).pendingActionDao()
        dao.insert(PendingActionEntity(type = type, payloadJson = payload.toString()))
        scheduleSync(context)
    }

    /** يجدول تشغيل SyncWorker أول ما يتوفر اتصال (WorkManager نفسه يراقب الشبكة) */
    fun scheduleSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = LocalDatabase.getInstance(applicationContext)
        val dao = db.pendingActionDao()
        val actions = dao.getAll()
        if (actions.isEmpty()) return Result.success()

        var hadFailure = false

        for (action in actions) {
            try {
                val payload = JSONObject(action.payloadJson)
                when (action.type) {
                    "REGISTER_MEMBER" -> FirebaseHelper.registerMemberRemote(
                        familyId = AppMode.FAMILY_CODE,
                        uid = payload.getString("uid"),
                        name = payload.getString("name"),
                        phone = payload.getString("phone"),
                        email = payload.getString("email")
                    )
                    "SET_GROUP_ACCESS" -> FirebaseHelper.setGroupChatAccess(
                        familyId = AppMode.FAMILY_CODE,
                        uid = payload.getString("uid"),
                        allowed = payload.getBoolean("allowed")
                    )
                    "SEND_MESSAGE" -> {
                        val remoteId = FirebaseHelper.sendGroupMessage(
                            familyId = AppMode.FAMILY_CODE,
                            senderId = payload.getString("senderId"),
                            text = payload.getString("text"),
                            timestamp = payload.getLong("timestamp")
                        )
                        payload.optLong("localMessageId", -1L).takeIf { it >= 0 }?.let { localId ->
                            db.messageDao().updateStatus(localId, "SENT", remoteId)
                        }
                    }
                }
                dao.delete(action)
            } catch (e: Exception) {
                // نسيب العنصر في القائمة ونزيد عداد المحاولات، باش WorkManager يعاود المحاولة لاحقًا
                dao.update(action.copy(attempts = action.attempts + 1))
                hadFailure = true
            }
        }

        return if (hadFailure) Result.retry() else Result.success()
    }
}
