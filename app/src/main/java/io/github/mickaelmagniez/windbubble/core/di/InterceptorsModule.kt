package io.github.mickaelmagniez.windbubble.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.Interceptor

/**
 * Declares the (possibly empty) set of OkHttp interceptors, so the release build works without any
 * while the debug build can contribute logging.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptorsModule {

    @Multibinds
    abstract fun interceptors(): Set<Interceptor>
}
