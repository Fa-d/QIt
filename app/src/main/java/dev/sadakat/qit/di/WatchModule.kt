package dev.sadakat.qit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.watch.WatchConnection
import dev.sadakat.qit.watch.WatchLink
import javax.inject.Singleton

/** Binds the Wearable-based [WatchLink] as the app's [WatchConnection]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class WatchModule {

    @Binds
    @Singleton
    abstract fun bindWatchConnection(impl: WatchLink): WatchConnection
}
