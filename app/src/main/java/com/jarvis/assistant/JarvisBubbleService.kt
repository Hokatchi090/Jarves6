package com.jarvis.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * فقاعة عائمة دائرية خارج التطبيق (زي "chat head"): تضل ظاهرة فوق أي تطبيق،
 * تنجرّ بإصبعك لأي مكان في الشاشة، وبضغطة وحدة تفتح قائمة دائرية حول نفسها
 * (مش قائمة مستطيلة) وكل خيار فيها هو بحد ذاته دائرة صغيرة.
 */
class JarvisBubbleService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_bubble_service"
        const val NOTIF_ID = 502
        private const val BUBBLE_SIZE_DP = 56
        private const val MENU_ITEM_SIZE_DP = 52
        private const val MENU_RADIUS_DP = 100
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var menuView: View? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        removeMenu()
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        super.onDestroy()
    }

    // ---------------- الإشعار (foreground service إلزامي على أندرويد الحديث) ----------------

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "\u0627\u0644\u0641\u0642\u0627\u0639\u0629 \u0627\u0644\u0639\u0627\u0626\u0645\u0629", NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("\u062C\u0627\u0631\u0641\u0633 \u0641\u0648\u0642 \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A")
            .setSmallIcon(android.R.drawable.presence_online)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // ---------------- الفقاعة نفسها ----------------

    private fun circleDrawable(fillColor: Int, strokeColor: Int, strokeWidthDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
            setStroke(dp(strokeWidthDp), strokeColor)
        }
    }

    private fun addBubble() {
        val size = dp(BUBBLE_SIZE_DP)
        val bubble = FrameLayout(this).apply {
            background = circleDrawable(Color.parseColor("#050B14"), Color.parseColor("#00F6FF"), 2)
            clipToOutline = true
        }
        val label = TextView(this).apply {
            text = "JARVIS"
            setTextColor(Color.parseColor("#00F6FF"))
            textSize = 7f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        bubble.addView(label, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            size, size, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dp(200)
        }

        bubble.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (hypot(dx, dy) > 12) isDragging = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    runCatching { windowManager.updateViewLayout(bubble, params) }
                    if (menuView != null) removeMenu()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) toggleMenu(params)
                    true
                }
                else -> false
            }
        }

        bubbleView = bubble
        bubbleParams = params
        runCatching { windowManager.addView(bubble, params) }
    }

    // ---------------- القائمة الدائرية (كل خيار = دائرة) ----------------

    private data class MenuOption(val label: String, val action: () -> Unit)

    private fun menuOptions(): List<MenuOption> = listOf(
        MenuOption("\u26C5") { openMainActivity("WEATHER") },          // الطقس
        MenuOption("\uD83D\uDC5F") { openMainActivity("FITNESS") },     // الخطوات
        MenuOption("\uD83D\uDCAC") { openMainActivity(null) },          // الشات
        MenuOption("\uD83D\uDD11") { openMainActivity("API_KEYS") },    // المفاتيح
        MenuOption("\u2715") { stopSelf() }                             // إغلاق
    )

    private fun toggleMenu(bubbleParams: WindowManager.LayoutParams) {
        if (menuView != null) {
            removeMenu()
            return
        }
        val options = menuOptions()
        val itemSize = dp(MENU_ITEM_SIZE_DP)
        val radius = dp(MENU_RADIUS_DP)
        val bubbleSize = dp(BUBBLE_SIZE_DP)
        val centerX = bubbleParams.x + bubbleSize / 2
        val centerY = bubbleParams.y + bubbleSize / 2

        // حاوية شفافة بملء الشاشة: تحوي الخيارات الدائرية + تُغلق القائمة عند الضغط برّاها
        val container = FrameLayout(this)
        container.setOnClickListener { removeMenu() }

        val count = options.size
        options.forEachIndexed { index, option ->
            // نوزّع الخيارات على نصف دائرة فوق الفقاعة (زاوية 180° إلى 360°) باش تبقى مرئية دائمًا
            val angleDeg = 180.0 + (180.0 / (count - 1)) * index
            val angleRad = Math.toRadians(angleDeg)
            val itemCx = centerX + radius * cos(angleRad)
            val itemCy = centerY + radius * sin(angleRad)

            val chip = TextView(this).apply {
                text = option.label
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#E8FFFF"))
                background = circleDrawable(Color.parseColor("#020608"), Color.parseColor("#00F6FF"), 2)
                setOnClickListener {
                    removeMenu()
                    option.action()
                }
            }
            val lp = FrameLayout.LayoutParams(itemSize, itemSize).apply {
                leftMargin = (itemCx - itemSize / 2).toInt()
                topMargin = (itemCy - itemSize / 2).toInt()
            }
            container.addView(chip, lp)
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        menuView = container
        runCatching { windowManager.addView(container, menuParams) }
    }

    private fun removeMenu() {
        menuView?.let { runCatching { windowManager.removeView(it) } }
        menuView = null
    }

    // ---------------- فتح التطبيق الرئيسي مع أمر سريع ----------------

    private fun openMainActivity(bubbleAction: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            if (bubbleAction != null) putExtra("BUBBLE_ACTION", bubbleAction)
        }
        startActivity(intent)
    }
}
