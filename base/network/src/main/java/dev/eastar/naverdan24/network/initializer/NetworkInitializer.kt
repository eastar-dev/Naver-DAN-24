package dev.eastar.naverdan24.network.initializer

import android.content.Context
import androidx.startup.Initializer

@Suppress("unused")
class NetworkInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        android.util.Log.e("Tag", "NetworkInitializer")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

