package dev.eastar.naverdan24.network.interceptor

import okhttp3.Interceptor

class PojoInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(chain.request())
}
