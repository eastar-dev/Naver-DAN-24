@file:Suppress("unused", "FunctionName", "NonAsciiCharacters", "SpellCheckingInspection", "MemberVisibilityCanBePrivate", "RemoveRedundantBackticks", "PropertyName")

package dev.eastar.naverdan24.__package__

import android.app.Activity
import android.content.Intent
import android.log.Log
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity

class __Package__EasterEgg(private val activity: Activity) {
    fun ComponentActivity.`여기코드를변경해서작업해주세요_1`() {
        Log.d("여기코드를변경해서작업해주세요_1")
    }

    fun ComponentActivity.`여기코드를변경해서작업해주세요_2`() {
        Log.d("여기코드를변경해서작업해주세요_2")
    }

    fun ComponentActivity._startSetting() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }.also {
            startActivity(it)
        }
    }

    fun ComponentActivity._logout() {
        Log.i("로그아웃 완료 $it")
    }

    fun ComponentActivity._login() {
        Log.i("로그인 완료 $it")
        recreate()
    }

    /**
     * 자동 실행
     */
    fun ___autoRun() {
        Log.i("여기에 작성한 코드는 자동으로 실행합니다.")
    }

    /**
     * navigate 로시작하는 함수는 @Composable DetailScreen() 화면으로 변경됨
     */
    fun ___navigate_detail() = Unit
}

