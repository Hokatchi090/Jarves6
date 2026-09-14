package com.jarvisx.app.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView
import com.jarvisx.app.AppMode
import com.jarvisx.app.R

/**
 * يُستدعى من AdminMainActivity فقط. في نسخة FAMILY هاذ الكلاس ما يُستعملش
 * إطلاقًا (AppMode.isAdmin() == false)، فالقائمة الجانبية ما تظهرش نهائيًا.
 */
object SideDrawerMenu {

    fun setup(activity: AppCompatActivity, navigationView: NavigationView) {
        if (!AppMode.isAdmin()) {
            navigationView.visibility = android.view.View.GONE
            return
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_join_group -> showFamilyCodeDialog(activity)
                R.id.menu_manage_group_chat -> activity.startActivity(
                    Intent(activity, GroupChatManagerActivity::class.java)
                )
                R.id.menu_group_settings -> showFamilyCodeDialog(activity) // نفس الشاشة حاليًا: عرض/نسخ الكود
            }
            true
        }
    }

    private fun showFamilyCodeDialog(activity: AppCompatActivity) {
        val code = AppMode.FAMILY_CODE
        AlertDialog.Builder(activity)
            .setTitle("\u0643\u0648\u062F \u0627\u0644\u0639\u0627\u0626\u0644\u0629")
            .setMessage(code)
            .setPositiveButton("\u0646\u0633\u062E") { _, _ ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("family_code", code))
            }
            .setNeutralButton("\u0645\u0634\u0627\u0631\u0643\u0629") { _, _ ->
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, code)
                }
                activity.startActivity(Intent.createChooser(share, null))
            }
            .setNegativeButton("\u0625\u063A\u0644\u0627\u0642", null)
            .show()
    }
}
