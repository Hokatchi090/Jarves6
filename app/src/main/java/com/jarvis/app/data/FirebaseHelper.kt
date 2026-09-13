package com.jarvisx.app.data

import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

/**
 * كل دوال Firebase هنا. القاعدة الذهبية: الواجهة ما تنادي هاذ الملف مباشرة أبدًا —
 * فقط SyncManager يستدعيه (من قائمة الانتظار)، أو FamilyConfigDao/ConfigLoader
 * للتحقق الأولي من الكود بشكل offline عبر hash.
 */
object FirebaseHelper {

    init {
        // يفعّل الكاش المحلي المدمج في Firebase نفسه (اختياري فوق Room، لكنه مجاني ومفيد)
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }
    }

    private fun db() = FirebaseDatabase.getInstance().reference

    // ---------------- كود العائلة ----------------

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** تحقق أوفلاين بالكامل — لا يحتاج شبكة أبدًا */
    fun validateFamilyCodeOffline(enteredCode: String, storedHash: String): Boolean {
        return sha256(enteredCode.trim()) == storedHash
    }

    // ---------------- الأعضاء ----------------

    /** تُستدعى فقط من SyncManager بعد التأكد من وجود اتصال */
    fun registerMemberRemote(familyId: String, uid: String, name: String, phone: String, email: String) {
        db().child("families").child(familyId).child("members").child(uid)
            .setValue(mapOf("name" to name, "phone" to phone, "email" to email))
    }

    fun setGroupChatAccess(familyId: String, uid: String, allowed: Boolean) {
        db().child("families").child(familyId).child("chat").child("group")
            .child("allowed_members").child(uid).setValue(allowed)
    }

    /** استماع حي اختياري (لو متوفر نت) — التحديثات تُكتب في Room عبر ConfigLoader.syncMembersFromRemote */
    fun listenToAllowedMembers(familyId: String, onUpdate: (Map<String, Boolean>) -> Unit) {
        db().child("families").child(familyId).child("chat").child("group").child("allowed_members")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val map = mutableMapOf<String, Boolean>()
                    for (child in snapshot.children) {
                        val allowed = child.getValue(Boolean::class.java) ?: false
                        map[child.key ?: continue] = allowed
                    }
                    onUpdate(map)
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    // ---------------- الرسائل ----------------

    fun sendGroupMessage(familyId: String, senderId: String, text: String, timestamp: Long): String {
        val ref = db().child("families").child(familyId).child("chat").child("group")
            .child("messages").push()
        ref.setValue(mapOf("senderId" to senderId, "text" to text, "timestamp" to timestamp))
        return ref.key ?: ""
    }
}
