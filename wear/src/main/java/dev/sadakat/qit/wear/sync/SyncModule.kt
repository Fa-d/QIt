package dev.sadakat.qit.wear.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the Wearable-based source as the watch's [BanglaVoiceSource]. */
@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {

    @Binds
    fun bindBanglaVoiceSource(source: WearableBanglaVoiceSource): BanglaVoiceSource

    @Binds
    fun bindListeningChannel(channel: WearableListeningChannel): ListeningChannel
}
