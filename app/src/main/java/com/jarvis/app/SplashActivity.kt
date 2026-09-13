package com.jarvisx.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvisx.app.admin.AdminMainActivity
import com.jarvisx.app.data.NetworkStateHolder
import com.jarvisx.app.member.MemberMainActivity
import com.jarvisx.app.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // مراقبة الاتصال تنطلق من هنا مرة وحدة وتخدم كل التطبيق
        NetworkStateHolder.init(this)

        val ring = findViewById<View>(R.id.splashRing)
        val title = findViewById<TextView>(R.id.splashTitle)
        playEntranceAnimation(ring, title)

        Handler(Looper.getMainLooper()).postDelayed({ routeToNextScreen() }, 1600L)
    }

    /** أنيميشن الدخول: الدائرة تتوسع وتدور، والعنوان يظهر تدريجيًا بعدها */
    private fun playEntranceAnimation(ring: View, title: TextView) {
        ring.scaleX = 0f
        ring.scaleY = 0f
        val scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 0f, 1f)
        val rotate = ObjectAnimator.ofFloat(ring, "rotation", 0f, 360f).apply {
            duration = 1400
            repeatCount = ObjectAnimator.INFINITE
        }

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 500
            interpolator = OvershootInterpolator(2.5f)
            start()
        }
        rotate.start()

        title.animate()
            .alpha(1f)
            .setStartDelay(500)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun routeToNextScreen() {
        if (AppMode.isAdmin()) {
            startActivity(Intent(this, AdminMainActivity::class.java))
        } else {
            if (isFirstLaunch()) {
                startActivity(Intent(this, OnboardingActivity::class.java))
            } else {
                startActivity(Intent(this, MemberMainActivity::class.java))
            }
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("jarvisx_prefs", Context.MODE_PRIVATE)
        return !prefs.getBoolean("onboarding_done", false)
    }
}
