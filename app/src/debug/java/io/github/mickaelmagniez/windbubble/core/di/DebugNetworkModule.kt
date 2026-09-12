package io.github.mickaelmagniez.windbubble.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/** Debug-only: logs every Open-Meteo call to logcat under the `WindBubbleHttp` tag. */
@Module
@InstallIn(SingletonComponent::class)
object DebugNetworkModule {

    @Provides
    @IntoSet
    fun provideLoggingInterceptor(): Interceptor =
        HttpLoggingInterceptor { message -> android.util.Log.d("WindBubbleHttp", message) }
            .apply { level = HttpLoggingInterceptor.Level.BASIC }
}
