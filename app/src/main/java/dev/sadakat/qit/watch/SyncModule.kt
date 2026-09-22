package dev.sadakat.qit.watch

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the Wearable-based channel as the listening sync's [ListeningSyncChannel]. */
@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {

    @Binds
    fun bindListeningSyncChannel(channel: WearableListeningSyncChannel): ListeningSyncChannel
}
