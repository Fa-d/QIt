package dev.sadakat.qit.watch

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the Wearable-based channels as the listening and Bangla voice syncs' interfaces. */
@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {

    @Binds
    fun bindListeningSyncChannel(channel: WearableListeningSyncChannel): ListeningSyncChannel

    @Binds
    fun bindBanglaVoiceSyncChannel(channel: WearableBanglaVoiceSyncChannel): BanglaVoiceSyncChannel
}
