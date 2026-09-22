package dev.sadakat.qit.wear.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.wear.audio.AudioManagerStreamVolume
import dev.sadakat.qit.wear.audio.StreamVolume

/** Binds the watch's audio adapters to their ports. */
@Module
@InstallIn(SingletonComponent::class)
abstract class WearAudioModule {

    @Binds
    abstract fun bindStreamVolume(impl: AudioManagerStreamVolume): StreamVolume
}
