package com.jarvisx.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.jarvisx.app.AppMode
import com.jarvisx.app.R
import com.jarvisx.app.data.FamilyConfigEntity
import com.jarvisx.app.data.FirebaseHelper
import com.jarvisx.app.data.LocalDatabase
import com.jarvisx.app.data.MemberEntity
import androidx.lifecycle.lifecycleScope
import com.jarvisx.app.member.MemberMainActivity
import com.jarvisx.app.sync.SyncManager
import com.jarvisx.app.ui.AnimUtils
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/** يُملأ تدريجيًا عبر الخطوات الخمس، ويُحفظ محليًا (Room) عند اكتماله — لا ينتظر أي رد من السيرفر */
data class OnboardingData(
    var enteredFamilyCode: String = "",
    var name: String = "",
    var phone: String = "",
    var email: String = "",
    var wakeWord: String = "Hey Jarvis",
    var appName: String = "Jarvis",
    var themeColorHex: String = "#0D1117"
)

interface OnboardingStepListener {
    fun onStepCompleted()
}

class OnboardingActivity : AppCompatActivity(), OnboardingStepListener {

    private val data = OnboardingData()
    private var stepIndex = 0

    private val steps: List<() -> Fragment> = listOf(
        { FamilyCodeFragment.newInstance() },
        { PersonalInfoFragment.newInstance() },
        { WakeWordFragment.newInstance() },
        { AppNameFragment.newInstance() },
        { BackgroundColorFragment.newInstance() }
    )

    lateinit var progressBar: ProgressBar
        private set

    fun currentData(): OnboardingData = data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        progressBar = findViewById(R.id.onboardingProgress)
        progressBar.max = steps.size
        showStep(0)
    }

    private fun showStep(index: Int) {
        stepIndex = index
        AnimUtils.animateProgress(progressBar, index)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_right, R.anim.slide_out_left
            )
            .replace(R.id.onboardingContainer, steps[index]())
            .commit()
    }

    override fun onStepCompleted() {
        if (stepIndex < steps.size - 1) {
            showStep(stepIndex + 1)
        } else {
            finishOnboarding()
        }
    }

    /** الخطوة الأخيرة: نحفظ كل شيء محليًا فورًا وندخل للواجهة، والمزامنة تصير بصمت في الخلفية */
    private fun finishOnboarding() {
        val uid = getSharedPreferences("jarvisx_prefs", Context.MODE_PRIVATE)
            .getString("local_uid", null) ?: UUID.randomUUID().toString().also {
            getSharedPreferences("jarvisx_prefs", Context.MODE_PRIVATE).edit().putString("local_uid", it).apply()
        }

        lifecycleScope.launch {
            val db = LocalDatabase.getInstance(this@OnboardingActivity)

            db.familyConfigDao().save(
                FamilyConfigEntity(
                    familyCode = AppMode.FAMILY_CODE,
                    familyCodeHash = FirebaseHelper.sha256(AppMode.FAMILY_CODE),
                    wakeWord = data.wakeWord,
                    appName = data.appName,
                    themeColorHex = data.themeColorHex,
                    currentUserUid = uid
                )
            )

            db.memberDao().upsert(
                MemberEntity(
                    uid = uid,
                    name = data.name,
                    phone = data.phone,
                    email = data.email,
                    allowedInGroupChat = false
                )
            )

            // تسجيل العضو عند المدير يدخل قائمة الانتظار — يصير فعليًا لما يتوفر نت
            SyncManager.enqueue(
                this@OnboardingActivity, "REGISTER_MEMBER",
                JSONObject().apply {
                    put("uid", uid); put("name", data.name)
                    put("phone", data.phone); put("email", data.email)
                }
            )

            getSharedPreferences("jarvisx_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("onboarding_done", true).apply()

            startActivity(Intent(this@OnboardingActivity, MemberMainActivity::class.java))
            finish()
        }
    }
}
