package io.github.mickaelmagniez.windbubble.core.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Application-wide scope, used for state that must outlive any single screen or service. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
