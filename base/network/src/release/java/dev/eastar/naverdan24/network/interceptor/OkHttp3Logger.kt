package dev.eastar.naverdan24.network.interceptor

import okhttp3.Interceptor

class OkHttp3Logger : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(chain.request())
}
