package com.jarvisx.app.ui

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.AbsListView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.hypot

/**
 * كل الأنيميشن اللي نستعملها عبر التطبيق كامل مجمّعة هنا، باش أي شاشة
 * جديدة تقدر تستدعي نفس الحركات بسطر وحد بدل ما تعيد كتابتها.
 */
object AnimUtils {

    /** حركة "نبضة" عند الضغط على أي زر — نستعملها في كل أزرار "التالي/إرسال/إنهاء" */
    fun bounceClick(view: View, onEnd: (() -> Unit)? = null) {
        view.animate().cancel()
        view.animate()
            .scaleX(0.9f).scaleY(0.9f)
            .setDuration(90)
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(140)
                    .setInterpolator(OvershootInterpolator(3f))
                    .withEndAction { onEnd?.invoke() }
                    .start()
            }
            .start()
    }

    /** تحويل سلس بين لونين بدل التغيير الفجائي — نستعملها في تبديل لون الخلفية */
    fun crossfadeBackgroundColor(view: View, fromColor: Int, toColor: Int, durationMs: Long = 450) {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        animator.duration = durationMs
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { view.setBackgroundColor(it.animatedValue as Int) }
        animator.start()
    }

    /** ظهور تدريجي من الأسفل للأعلى — نستعملها لعناوين وبطاقات الشاشات */
    fun fadeInUp(view: View, delayMs: Long = 0, durationMs: Long = 380) {
        view.alpha = 0f
        view.translationY = 40f
        view.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(durationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /** يحرّك شريط التقدم بسلاسة بدل القفز المباشر — نستعملها بين خطوات التسجيل */
    fun animateProgress(bar: ProgressBar, toValue: Int) {
        ObjectAnimator.ofInt(bar, "progress", bar.progress, toValue)
            .setDuration(280)
            .start()
    }

    /** موجة دائرية تنطلق من الزر المضغوط وتغطي الشاشة — للانتقالات "الأسطورية" بين الشاشات الكبيرة */
    fun circularReveal(root: View, originView: View, onComplete: (() -> Unit)? = null) {
        if (root !is ViewGroup) return
        val cx = (originView.x + originView.width / 2).toInt()
        val cy = (originView.y + originView.height / 2).toInt()
        val radius = hypot(root.width.toDouble(), root.height.toDouble()).toFloat()
        root.visibility = View.VISIBLE
        val anim = ViewAnimationUtils.createCircularReveal(root, cx, cy, 0f, radius)
        anim.duration = 500
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.start()
        onComplete?.let { root.postDelayed(it, 500) }
    }

    /** دخول متتابع (Staggered) لعناصر RecyclerView — نستعملها في قائمة الأعضاء وقائمة الرسائل */
    fun staggerRecyclerView(recyclerView: RecyclerView) {
        val controller = android.view.animation.AnimationUtils.loadLayoutAnimation(
            recyclerView.context, com.jarvisx.app.R.anim.layout_fall_down
        )
        recyclerView.layoutAnimation = controller
        recyclerView.scheduleLayoutAnimation()
    }

    /** نفس الفكرة لكن لـ ListView (تُستعمل في شاشة الدردشة الجماعية) */
    fun staggerListView(listView: AbsListView) {
        val controller = android.view.animation.AnimationUtils.loadLayoutAnimation(
            listView.context, com.jarvisx.app.R.anim.layout_fall_down
        )
        listView.layoutAnimation = controller
        listView.scheduleLayoutAnimation()
    }
}
