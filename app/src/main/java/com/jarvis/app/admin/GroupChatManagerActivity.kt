package com.jarvisx.app.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jarvisx.app.AppMode
import com.jarvisx.app.R
import com.jarvisx.app.data.ConfigLoader
import com.jarvisx.app.data.LocalDatabase
import com.jarvisx.app.data.MemberEntity
import com.jarvisx.app.sync.SyncManager
import com.jarvisx.app.ui.AnimUtils
import kotlinx.coroutines.launch
import org.json.JSONObject

class GroupChatManagerActivity : AppCompatActivity() {

    private lateinit var adapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat_manager)

        val recycler = findViewById<RecyclerView>(R.id.membersRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = MembersAdapter { member, allowed -> toggleMemberAccess(member.uid, allowed) }
        recycler.adapter = adapter

        val configLoader = ConfigLoader(this)
        var firstLoad = true
        lifecycleScope.launch {
            configLoader.observeAllMembers().collect { members ->
                adapter.submit(members)
                if (firstLoad && members.isNotEmpty()) {
                    firstLoad = false
                    AnimUtils.staggerRecyclerView(recycler)
                }
            }
        }
    }

    /** يكتب محليًا فورًا (الواجهة تتحدث في نفس اللحظة) ثم يدخل قائمة الانتظار للمزامنة */
    private fun toggleMemberAccess(uid: String, allowed: Boolean) {
        lifecycleScope.launch {
            LocalDatabase.getInstance(this@GroupChatManagerActivity).memberDao().setAllowed(uid, allowed)
            SyncManager.enqueue(
                this@GroupChatManagerActivity, "SET_GROUP_ACCESS",
                JSONObject().apply { put("uid", uid); put("allowed", allowed) }
            )
        }
    }

    private class MembersAdapter(
        private val onToggle: (MemberEntity, Boolean) -> Unit
    ) : RecyclerView.Adapter<MembersAdapter.VH>() {

        private val items = mutableListOf<MemberEntity>()

        fun submit(newItems: List<MemberEntity>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val member = items[position]
            holder.name.text = member.name
            holder.toggle.setOnCheckedChangeListener(null)
            holder.toggle.isChecked = member.allowedInGroupChat
            holder.toggle.setOnCheckedChangeListener { _, checked -> onToggle(member, checked) }
        }

        override fun getItemCount(): Int = items.size

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.memberName)
            val toggle: Switch = view.findViewById(R.id.memberToggle)
        }
    }
}
