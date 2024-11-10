package dev.eastar.naverdan24.network.di


import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.eastar.naverdan24.network.initializer.jsonParser
import dev.eastar.naverdan24.network.interceptor.OkHttp3Logger
import dev.eastar.naverdan24.network.interceptor.PojoInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object OkHttpClientModule {
    private const val DEFAULT_TIMEOUT_SECONDS: Long = 10_000

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(OkHttp3Logger())
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.MILLISECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.MILLISECONDS)
            .build()

    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(Hosts.BASEURL)
        .client(
            okHttpClient.newBuilder()
                .addInterceptor(PojoInterceptor())
                .build()
        )
        .addConverterFactory(jsonParser.asConverterFactory("application/json".toMediaType()))
        .build()
}
