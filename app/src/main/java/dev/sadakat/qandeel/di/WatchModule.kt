package dev.sadakat.qandeel.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qandeel.watch.WatchConnection
import dev.sadakat.qandeel.watch.WatchLink
import javax.inject.Singleton

/** Binds the Wearable-based [WatchLink] as the app's [WatchConnection]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class WatchModule {

    @Binds
    @Singleton
    abstract fun bindWatchConnection(impl: WatchLink): WatchConnection
}
