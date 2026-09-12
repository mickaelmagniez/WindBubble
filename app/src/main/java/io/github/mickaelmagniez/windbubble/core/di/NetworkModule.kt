package io.github.mickaelmagniez.windbubble.core.di

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.mickaelmagniez.windbubble.data.remote.OpenMeteoApi
import dagger.Module
import dagger.Provides

import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** Empty in release; the debug variant contributes an HTTP logging interceptor. */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        interceptors: Set<@JvmSuppressWildcards Interceptor>,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply { interceptors.forEach(::addInterceptor) }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(OpenMeteoApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideOpenMeteoApi(retrofit: Retrofit): OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)

    private const val TIMEOUT_SECONDS = 15L
}
