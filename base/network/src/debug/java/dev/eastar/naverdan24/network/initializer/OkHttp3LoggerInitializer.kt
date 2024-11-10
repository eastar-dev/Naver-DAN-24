package dev.eastar.naverdan24.network.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import dev.eastar.naverdan24.network.interceptor.OkHttp3Logger

@Suppress("unused")
class OkHttp3LoggerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Log.e("tag","OkHttp3LoggerInitializer")
        OkHttp3Logger.initializer(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
