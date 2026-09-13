package com.jarvisx.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ============================== Entities ==============================

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val phone: String,
    val email: String,
    val allowedInGroupChat: Boolean = false,
    val lastSyncedAt: Long = 0L
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val syncStatus: String = "PENDING" // PENDING / SENT / FAILED
)

@Entity(tableName = "family_config")
data class FamilyConfigEntity(
    @PrimaryKey val id: Int = 0, // صف وحيد دائمًا (singleton)
    val familyCode: String,
    val familyCodeHash: String,
    val wakeWord: String = "Hey Jarvis",
    val appName: String = "Jarvis",
    val themeColorHex: String = "#0D1117",
    val currentUserUid: String? = null
)

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,       // e.g. "SET_GROUP_ACCESS" / "SEND_MESSAGE" / "REGISTER_MEMBER"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)

// ============================== DAOs ==============================

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY name ASC")
    fun observeAll(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE uid = :uid LIMIT 1")
    fun observe(uid: String): Flow<MemberEntity?>

    @Query("SELECT * FROM members WHERE uid = :uid LIMIT 1")
    suspend fun get(uid: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(members: List<MemberEntity>)

    @Query("UPDATE members SET allowedInGroupChat = :allowed WHERE uid = :uid")
    suspend fun setAllowed(uid: String, allowed: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPending(): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("UPDATE messages SET syncStatus = :status, remoteId = :remoteId WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, remoteId: String? = null)
}

@Dao
interface FamilyConfigDao {
    @Query("SELECT * FROM family_config WHERE id = 0 LIMIT 1")
    fun observe(): Flow<FamilyConfigEntity?>

    @Query("SELECT * FROM family_config WHERE id = 0 LIMIT 1")
    suspend fun get(): FamilyConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: FamilyConfigEntity)
}

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(action: PendingActionEntity): Long

    @Delete
    suspend fun delete(action: PendingActionEntity)

    @Update
    suspend fun update(action: PendingActionEntity)
}

// ============================== Database ==============================

@Database(
    entities = [MemberEntity::class, MessageEntity::class, FamilyConfigEntity::class, PendingActionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun messageDao(): MessageDao
    abstract fun familyConfigDao(): FamilyConfigDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        @Volatile private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "jarvisx_local.db"
                ).build().also { INSTANCE = it }
            }
    }
}
