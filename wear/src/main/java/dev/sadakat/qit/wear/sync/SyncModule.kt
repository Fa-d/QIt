package dev.sadakat.qit.wear.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the Wearable-based channel as the watch's [ListeningChannel]. */
@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {

    @Binds
    fun bindListeningChannel(channel: WearableListeningChannel): ListeningChannel
}
