package com.jarvisx.app.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView
import com.jarvisx.app.R

/**
 * ملاحظة: هاذي واجهة أساسية بسيطة تستضيف القائمة الجانبية (SideDrawerMenu) فقط.
 * لو عندك واجهة Admin كاملة موجودة مسبقًا (مثلاً MainActivity من مشروع Jarvis
 * الأصلي بكل ميزاته)، عوّض محتوى هاذ الملف باش يفتح تلك الواجهة، وزيد فيها
 * استدعاء SideDrawerMenu.setup(this, navigationView) فقط.
 */
class AdminMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_main)

        val navigationView = findViewById<NavigationView>(R.id.adminNavigationView)
        SideDrawerMenu.setup(this, navigationView)
    }
}
