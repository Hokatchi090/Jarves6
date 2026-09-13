package com.jarvisx.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * كل قراءة في التطبيق تمر من هنا، وهاذ الملف بدوره يقرأ من Room فقط —
 * ما يناديش Firebase مباشرة أبدًا. المزامنة تصير في الخلفية عبر SyncManager
 * وتحدّث Room، والواجهة تلاحظ التغيير تلقائيًا عبر Flow.
 */
class ConfigLoader(context: Context) {

    private val db = LocalDatabase.getInstance(context)

    fun observeFamilyConfig(): Flow<FamilyConfigEntity?> = db.familyConfigDao().observe()

    suspend fun getFamilyConfig(): FamilyConfigEntity? = db.familyConfigDao().get()

    fun observeCurrentMember(uid: String): Flow<MemberEntity?> = db.memberDao().observe(uid)

    suspend fun isAllowedInGroupChat(uid: String): Boolean =
        db.memberDao().get(uid)?.allowedInGroupChat ?: false

    fun observeAllMembers(): Flow<List<MemberEntity>> = db.memberDao().observeAll()

    /** يدمج تحديثات قادمة من Firebase (لو توفر نت) داخل Room، بدون ما يكسر القراءة الأوفلاين */
    suspend fun mergeAllowedMembersFromRemote(allowedMap: Map<String, Boolean>) {
        val dao = db.memberDao()
        for ((uid, allowed) in allowedMap) {
            dao.setAllowed(uid, allowed)
        }
    }
}
