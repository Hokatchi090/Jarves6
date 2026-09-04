package com.jarvis.assistant

import android.content.Context
import androidx.room.*

/**
 * سجل قرارات شفاف: كل اقتراح تداول يُخزَّن هنا مع سبب اعتباره حلالاً
 * وسبب اختيار التوقيت، بحيث يمكن مراجعته قبل الموافقة عليه.
 *
 * مهم: هذا الجدول لا يحتوي على أي مفاتيح API أو بيانات حساب مصرفي/وسيط.
 * "الموافقة" هنا تعني فقط "أنا أرغب بتنفيذ هذا الاقتراح يدوياً في تطبيق
 * الوسيط الخاص بي" — الحقل approvalStatus لا يُشغّل أي تنفيذ آلي أبداً.
 */
@Entity(tableName = "trade_suggestions")
data class TradeSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticker: String,
    val timestampMillis: Long,
    val isHalal: Boolean,
    val halalReason: String,
    val timingReason: String,
    val suggestedAction: String,   // "شراء" أو "بيع" أو "انتظار"
    val approvalStatus: String,    // PENDING / APPROVED_MANUAL_EXECUTION / REJECTED
    val notes: String = ""
)

@Dao
interface TradeSuggestionDao {
    @Insert
    suspend fun insert(entity: TradeSuggestionEntity): Long

    @Query("SELECT * FROM trade_suggestions ORDER BY timestampMillis DESC")
    suspend fun getAll(): List<TradeSuggestionEntity>

    @Query("SELECT * FROM trade_suggestions WHERE approvalStatus = 'PENDING' ORDER BY timestampMillis DESC")
    suspend fun getPending(): List<TradeSuggestionEntity>

    @Query("UPDATE trade_suggestions SET approvalStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}

@Database(entities = [TradeSuggestionEntity::class], version = 1, exportSchema = false)
abstract class JarvisFinanceDatabase : RoomDatabase() {
    abstract fun tradeSuggestionDao(): TradeSuggestionDao

    companion object {
        @Volatile private var instance: JarvisFinanceDatabase? = null

        fun getInstance(context: Context): JarvisFinanceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JarvisFinanceDatabase::class.java,
                    "jarvis_finance.db"
                ).build().also { instance = it }
            }
    }
}
